package org.testobject.fastbill;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.testobject.fastbill.jersey.RequestBuilder;
import org.testobject.fastbill.jersey.ResponseReader;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

class SubscriptionServiceImpl implements SubscriptionService {

	private final WebResource endpointResource;

	public SubscriptionServiceImpl(WebResource endpointResource) {
		this.endpointResource = endpointResource;
	}

	@Override
	public long createSubscription(long customerId, String productId) {
		Map<String, Object> request = new RequestBuilder("subscription.create")
				.addData("CUSTOMER_ID", customerId)
				.addData("ARTICLE_NUMBER", productId).build();

		ResponseReader response = new ResponseReader(endpointResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, request));

		return ((Number) response.getData("SUBSCRIPTION_ID")).longValue();
	}
	
	@Override
	public void updateSubscription(Subscription subscription) {
		Map<String, Object> request = new RequestBuilder("subscription.update")
			.addData("SUBSCRIPTION_ID", subscription.getId())
			.addData("NEXT_EVENT", Util.timestampToDate(subscription.getNext())).build();
		
		new ResponseReader(endpointResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, request));
	}


	@Override
	public Subscription getSubscription(long subscriptionId) {
		Map<String, Object> request = new RequestBuilder("subscription.get")
				.addFilter("SUBSCRIPTION_ID", subscriptionId)
				.build();

		ResponseReader response = new ResponseReader(endpointResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, request));

		List<Map<String, Object>> data = response.getData("SUBSCRIPTIONS");
		if (data.isEmpty()) {
			return null;
		}
		if (data.size() > 1) {
			throw new IllegalArgumentException("expected one result bu was " + data.size());
		}

		Map<String, Object> subscription = data.get(0);
		return toSubscription(subscription);
	}

	@Override
	public List<Subscription> getSubscriptions(long customerId) {
		Map<String, Object> request = new RequestBuilder("subscription.get")
				.addFilter("CUSTOMER_ID", customerId)
				.build();
		
		ResponseReader response = new ResponseReader(endpointResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, request));

		List<Map<String, Object>> data = response.getData("SUBSCRIPTIONS");
		List<Subscription> subscriptions = new ArrayList<Subscription>(data.size());
		
		for (Map<String, Object> subscriptiondata : data) {
			subscriptions.add(toSubscription(subscriptiondata));
		}

		return subscriptions;
	}

	@Override
	public void cancelSubscription(long subscriptionId) {
		Map<String, Object> request = new RequestBuilder("subscription.cancel")
				.addData("SUBSCRIPTION_ID", subscriptionId).build();

		new ResponseReader(endpointResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, request));
	}

	public void setUsagedata(long subscriptionId, String productId, int quantity, BigDecimal unitPrice, String description,
	      String usageDate, String currencyCode) {
	  Map<String, Object> request = new RequestBuilder("subscription.setusagedata")
	  .addData("SUBSCRIPTION_ID", subscriptionId)
	  .addData("ARTICLE_NUMBER", productId)
	  .addData("QUANTITY", quantity)
	  .addData("UNIT_PRICE", unitPrice)
	  .addData("DESCRIPTION", description)
	  .addData("USAGE_DATE", usageDate)
	  .addData("CURRENCY_CODE", currencyCode).build();

	  ResponseReader response = new ResponseReader(endpointResource.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
	      .post(ClientResponse.class, request));
	  }


	private static Subscription toSubscription(Map<String, Object> subscription) {
		long subscriptionId = ((Number) subscription.get("SUBSCRIPTION_ID")).longValue();
		long customerId = ((Number) subscription.get("CUSTOMER_ID")).longValue();
		long start = Util.secondsToTimestamp((String) subscription.get("START"));
		long next = Util.secondsToTimestamp((String) subscription.get("NEXT_EVENT"));
		long last = Util.secondsToTimestamp((String) subscription.get("LAST_EVENT"));
		String product = (String) subscription.get("ARTICLE_NUMBER");
		String status = subscription.get("STATUS").toString();
		String hash = subscription.get("HASH").toString();
		
		return new Subscription(subscriptionId, customerId, start, next, last, product, status, hash);
	}


}
