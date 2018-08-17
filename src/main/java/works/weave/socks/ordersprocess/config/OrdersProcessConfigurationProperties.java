package works.weave.socks.ordersprocess.config;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class OrdersProcessConfigurationProperties {
	private String domain = "";

	public URI getOrderAmountUri() {
		return new ServiceUri(new Hostname("orders"), new Domain(domain), "/amount").toUri();
	}

	public URI getOrderSaveUri() {
		return new ServiceUri(new Hostname("orders"), new Domain(domain), "/save").toUri();
	}

	public URI deleteCarts(String id) {
		return new ServiceUri(new Hostname("carts"), new Domain(domain), "/carts/" + id).toUri();
	}

	public URI getPaymentUri() {
		return new ServiceUri(new Hostname("payment"), new Domain(domain), "/paymentAuth").toUri();
	}

	public URI getShippingUri() {
		return new ServiceUri(new Hostname("shipping"), new Domain(domain), "/shipping").toUri();
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	private class Hostname {
		private final String hostname;

		private Hostname(String hostname) {
			this.hostname = hostname;
		}

		@Override
		public String toString() {
			if (hostname != null && !hostname.equals("")) {
				return hostname;
			} else {
				return "";
			}
		}
	}

	private class Domain {
		private final String domain;

		private Domain(String domain) {
			this.domain = domain;
		}

		@Override
		public String toString() {
			if (domain != null && !domain.equals("")) {
				return "." + domain;
			} else {
				return "";
			}
		}
	}

	private class ServiceUri {
		private final Hostname hostname;
		private final Domain domain;
		private final String endpoint;

		private ServiceUri(Hostname hostname, Domain domain, String endpoint) {
			this.hostname = hostname;
			this.domain = domain;
			this.endpoint = endpoint;
		}

		public URI toUri() {
			return URI.create(wrapHTTP(hostname.toString() + domain.toString()) + endpoint);
		}

		private String wrapHTTP(String host) {
			return "http://" + host;
		}

		@Override
		public String toString() {
			return "ServiceUri{" + "hostname=" + hostname + ", domain=" + domain + '}';
		}
	}
}
