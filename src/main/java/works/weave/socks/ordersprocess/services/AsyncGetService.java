package works.weave.socks.ordersprocess.services;

import static org.springframework.hateoas.MediaTypes.HAL_JSON;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeReferences;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import works.weave.socks.ordersprocess.config.RestProxyTemplate;
import works.weave.socks.ordersprocess.entities.Item;

@Service
public class AsyncGetService {
	private final Logger LOG = LoggerFactory.getLogger(getClass());

	private final RestProxyTemplate restProxyTemplate;

	private final RestTemplate halTemplate;

	@Autowired
	public AsyncGetService(RestProxyTemplate restProxyTemplate) {
		this.restProxyTemplate = restProxyTemplate;
		this.halTemplate = new RestTemplate(restProxyTemplate.getRestTemplate().getRequestFactory());

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.registerModule(new Jackson2HalModule());
		MappingJackson2HttpMessageConverter halConverter = new MappingJackson2HttpMessageConverter();
		halConverter.setSupportedMediaTypes(Arrays.asList(MediaTypes.HAL_JSON));
		halConverter.setObjectMapper(objectMapper);
		halTemplate.setMessageConverters(Collections.singletonList(halConverter));
	}

	@Async
	public <T> Future<Resource<T>> getResource(URI url, TypeReferences.ResourceType<T> type)
			throws InterruptedException, IOException {
		RequestEntity<Void> request = RequestEntity.get(url).accept(HAL_JSON).build();
		LOG.debug("Requesting: " + request.toString());
		Resource<T> body = restProxyTemplate.getRestTemplate().exchange(request, type).getBody();
		LOG.debug("Received: " + body.toString());
		return new AsyncResult<>(body);
	}

	@Async
	public String getAmount(URI url) {
		RestTemplate restTemplate = new RestTemplate();
		LOG.info("jetzt gehts los");
		String result = restTemplate.postForObject(url, null, String.class);
		LOG.info("Received: " + result);
		return result;
	}

	@Async
	public String postAmount(URI url, List<Item> items) {
		RestTemplate restTemplate = new RestTemplate();
		LOG.info("jetzt gehts los");
		if (items.isEmpty()) {
			return "ciao kakao";
		}
		String result = restTemplate.postForObject(url, items.get(0).toString(), String.class);
		LOG.info("Received: " + result);
		return result;
	}

	@Async
	public <T> Future<Resources<T>> getDataList(URI url, TypeReferences.ResourcesType<T> type)
			throws InterruptedException, IOException {
		RequestEntity<Void> request = RequestEntity.get(url).accept(HAL_JSON).build();
		LOG.debug("Requesting: " + request.toString());
		Resources<T> body = restProxyTemplate.getRestTemplate().exchange(request, type).getBody();
		LOG.debug("Received: " + body.toString());
		return new AsyncResult<>(body);
	}

	@Async
	public <T> Future<List<T>> getDataList(URI url, ParameterizedTypeReference<List<T>> type)
			throws InterruptedException, IOException {
		RequestEntity<Void> request = RequestEntity.get(url).accept(MediaType.APPLICATION_JSON).build();
		LOG.debug("Requesting: " + request.toString());
		List<T> body = restProxyTemplate.getRestTemplate().exchange(request, type).getBody();
		LOG.debug("Received: " + body.toString());
		return new AsyncResult<>(body);
	}

	@Async
	public <T, B> Future<T> postResource(URI uri, B body, ParameterizedTypeReference<T> returnType) {
		RequestEntity<B> request = RequestEntity.post(uri).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON).body(body);
		LOG.debug("Requesting: " + request.toString());
		T responseBody = restProxyTemplate.getRestTemplate().exchange(request, returnType).getBody();
		LOG.debug("Received: " + responseBody);
		return new AsyncResult<>(responseBody);
	}
}
