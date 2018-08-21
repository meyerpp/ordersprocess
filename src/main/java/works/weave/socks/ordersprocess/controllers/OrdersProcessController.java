package works.weave.socks.ordersprocess.controllers;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.mvc.TypeReferences;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import works.weave.socks.ordersprocess.config.OrdersProcessConfigurationProperties;
import works.weave.socks.ordersprocess.entities.Address;
import works.weave.socks.ordersprocess.entities.Card;
import works.weave.socks.ordersprocess.entities.Customer;
import works.weave.socks.ordersprocess.entities.CustomerOrder;
import works.weave.socks.ordersprocess.entities.Item;
import works.weave.socks.ordersprocess.entities.Shipment;
import works.weave.socks.ordersprocess.resources.NewOrderResource;
import works.weave.socks.ordersprocess.services.AsyncGetService;
import works.weave.socks.ordersprocess.values.PaymentRequest;
import works.weave.socks.ordersprocess.values.PaymentResponse;

@RepositoryRestController
public class OrdersProcessController {
	private final Logger LOG = LoggerFactory.getLogger(getClass());

	@Autowired
	private OrdersProcessConfigurationProperties config;

	@Autowired
	private AsyncGetService asyncGetService;


	@Value(value = "${http.timeout:5}")
	private long timeout;

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(path = "/orders", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
	public @ResponseBody CustomerOrder newOrder(@RequestBody NewOrderResource item) {
		LOG.info("Begin of orderprocess");
		try {

			if (item.address == null || item.customer == null || item.card == null || item.items == null) {
				throw new InvalidOrderException(
						"Invalid order request. Order requires customer, address, card and items.");
			}

			LOG.debug("Starting calls");
			Future<Resource<Address>> addressFuture = asyncGetService.getResource(item.address,
					new TypeReferences.ResourceType<Address>() {
					});
			Future<Resource<Customer>> customerFuture = asyncGetService.getResource(item.customer,
					new TypeReferences.ResourceType<Customer>() {
					});
			Future<Resource<Card>> cardFuture = asyncGetService.getResource(item.card,
					new TypeReferences.ResourceType<Card>() {
					});
			Future<List<Item>> itemsFuture = asyncGetService.getDataList(item.items,
					new ParameterizedTypeReference<List<Item>>() {
					});
			LOG.debug("End of calls.");

			Future<String> amountResponse = asyncGetService.postResource(config.getOrderAmountUri(), itemsFuture.get(),
					new ParameterizedTypeReference<String>() {
					});

			LOG.info("Amount to pay: " + amountResponse.get());
			float amount = Float.parseFloat(amountResponse.get());

			// Call payment service to make sure they've paid
			PaymentRequest paymentRequest = new PaymentRequest(
					addressFuture.get(timeout, TimeUnit.SECONDS).getContent(),
					cardFuture.get(timeout, TimeUnit.SECONDS).getContent(),
					customerFuture.get(timeout, TimeUnit.SECONDS).getContent(), amount);
			LOG.info("Sending payment request: " + paymentRequest);
			Future<PaymentResponse> paymentFuture = asyncGetService.postResource(config.getPaymentUri(), paymentRequest,
					new ParameterizedTypeReference<PaymentResponse>() {
					});
			PaymentResponse paymentResponse = paymentFuture.get(timeout, TimeUnit.SECONDS);
			LOG.info("Received payment response: " + paymentResponse);
			if (paymentResponse == null) {
				throw new PaymentDeclinedException("Unable to parse authorisation packet");
			}
			if (!paymentResponse.isAuthorised()) {
				throw new PaymentDeclinedException(paymentResponse.getMessage());
			}

			// Ship
			String customerId = parseId(customerFuture.get(timeout, TimeUnit.SECONDS).getId().getHref());
			Future<Shipment> shipmentFuture = asyncGetService.postResource(config.getShippingUri(),
					new Shipment(customerId), new ParameterizedTypeReference<Shipment>() {
					});

			CustomerOrder order = new CustomerOrder(null, customerId,
					customerFuture.get(timeout, TimeUnit.SECONDS).getContent(),
					addressFuture.get(timeout, TimeUnit.SECONDS).getContent(),
					cardFuture.get(timeout, TimeUnit.SECONDS).getContent(), itemsFuture.get(timeout, TimeUnit.SECONDS),
					shipmentFuture.get(timeout, TimeUnit.SECONDS), Calendar.getInstance().getTime(), amount);
			LOG.info("Received data: " + order.toString());

			Future<CustomerOrder> savedOrderFuture = asyncGetService.postResource(config.getOrderSaveUri(), order,
					new ParameterizedTypeReference<CustomerOrder>() {
					});

			// CustomerOrder savedOrder = customerOrderRepository.save(order);
			LOG.info("Saved order: " + savedOrderFuture.get());

			Future<String> deleteCartsResponse = asyncGetService.deleteCard(
					config.deleteCarts(savedOrderFuture.get().getCustomerId()),
					new ParameterizedTypeReference<String>() {
					});

			LOG.info("delete Response: " + deleteCartsResponse.get());

			return savedOrderFuture.get();
		} catch (TimeoutException e) {
			throw new IllegalStateException("Unable to create order due to timeout from one of the services.", e);
		} catch (InterruptedException | IOException | ExecutionException e) {
			throw new IllegalStateException("Unable to create order due to unspecified IO error.", e);
		}
	}

	private String parseId(String href) {
		Pattern idPattern = Pattern.compile("[\\w-]+$");
		Matcher matcher = idPattern.matcher(href);
		if (!matcher.find()) {
			throw new IllegalStateException("Could not parse user ID from: " + href);
		}
		return matcher.group(0);
	}

	// TODO: Add link to shipping
	// @RequestMapping(method = RequestMethod.GET, value = "/orders")
	// public @ResponseBody
	// ResponseEntity<?> getOrders() {
	// List<CustomerOrder> customerOrders = customerOrderRepository.findAll();
	//
	// Resources<CustomerOrder> resources = new Resources<>(customerOrders);
	//
	// resources.forEach(r -> r);
	//
	// resources.add(linkTo(methodOn(ShippingController.class,
	// CustomerOrder.getShipment::ge)).withSelfRel());
	//
	// // add other links as needed
	//
	// return ResponseEntity.ok(resources);
	// }

	@ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
	public class PaymentDeclinedException extends IllegalStateException {
		public PaymentDeclinedException(String s) {
			super(s);
		}
	}

	@ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
	public class InvalidOrderException extends IllegalStateException {
		public InvalidOrderException(String s) {
			super(s);
		}
	}
}
