package org.littlered.dataservices.unitTests;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.junit.Test;
import org.littlered.dataservices.dto.wordpress.UsersDTO;
import org.openapitools.client.api.AttendeeApi;
import org.openapitools.client.api.EventApi;
import org.openapitools.client.api.OrderApi;
import org.openapitools.client.api.QuestionsApi;
import org.openapitools.client.auth.HttpBearerAuth;
import org.openapitools.client.model.*;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

public class EventbriteTests {

	private String eventID = "1048354164527";
	private String apiKey = "XDAPRWGTYCXDSNF723";
	private String eventbritePrivateToken = "IFNRDYFWDM7J3RPNUQPK";
	private String questionId = "106450539";

	private String baseUrl = "https://api-prod.goplaynw.org";
	private String jwt = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJzdGFyIiwiZXhwIjoxNjY0MDgzNDQ5fQ.LcBnR2Nh-k2bC0dQKO6JSO0Sem7WaL3MiqC1LnHxDG3T95LF__HnR6yY4_cMsPCFwbtxTwkjgothc1JEe5evkA";
	private String gpnwApiKey = "7mW8XeEvNyNyaGHeimx5BxJncaGHeimx5BxJnc9QeNkeJ";

	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Test
	public void testEventbrite() throws Exception {
		OrderApi orderApi = new OrderApi();
		HttpBearerAuth orderAuth = (HttpBearerAuth) orderApi.getApiClient().getAuthentications().get("httpBearer");
		orderAuth.setBearerToken(eventbritePrivateToken);
		orderApi.getApiClient().setDebugging(true);

		AttendeeApi attendeeApi = new AttendeeApi();
		HttpBearerAuth attendeeAuth = (HttpBearerAuth) attendeeApi.getApiClient().getAuthentications().get("httpBearer");
		attendeeAuth.setBearerToken(eventbritePrivateToken);
		attendeeApi.getApiClient().setDebugging(true);


		ListOrdersbyEventIDresponse response = orderApi.listOrdersbyEventID(eventID, null, null, null,  null,
				null, null, "attendees");

		HashMap<String, String> discordIds = new HashMap<>();

		for (Order order : response.getOrders()) {
			Order fullOrder = orderApi.retrieveOrderbyID(order.getId());
			logger.info("got order " + fullOrder.getId());
			for (Attendee1 attendee : order.getAttendees() ) {

				if(attendee.getProfile().getEmail() == null) {
					continue;
				}

				logger.info("got attendee " + attendee.getId());
				for (Answer answer : attendee.getAnswers()) {
					if (answer.getQuestionId().equals(questionId)) {
						discordIds.put(order.getId(), answer.getAnswer());
					}
				}

			}
		}

		HttpClient client = new DefaultHttpClient();
		for (String orderId : discordIds.keySet()) {
			// https://www.javacodegeeks.com/2012/09/simple-rest-client-in-java.html

			HttpGet httpGet = new HttpGet(baseUrl.concat("/payment/eventbrite/orderId/").concat(orderId));
			httpGet.setHeader("x-api-key", "ApiKey ".concat(gpnwApiKey));
			HttpResponse httpResponse = client.execute(httpGet);
			BufferedReader rd = new BufferedReader (new InputStreamReader(httpResponse.getEntity().getContent()));
			String line = "";
			StringBuilder source = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				logger.info(line);
				source.append(line);
			}



			// https://stackoverflow.com/questions/41979086/how-to-serialize-date-to-long-using-gson
			Gson gson = new GsonBuilder()
					.registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, typeOfT, context) -> new Date(json.getAsJsonPrimitive().getAsLong()))
					.registerTypeAdapter(Date.class, (JsonSerializer<Date>) (date, type, jsonSerializationContext) -> new JsonPrimitive(date.getTime()))
					.create();

			UsersDTO usersDTO = gson.fromJson(source.toString(), UsersDTO.class);
			if(usersDTO == null) {
				logger.severe("No discord ID found for id " + orderId);
				continue;
			}
			logger.info("found " + usersDTO.getId());


			HttpPost httpPost = new HttpPost(baseUrl.concat("/social/discord/setID"));
			httpPost.setHeader("x-api-key", "ApiKey ".concat(gpnwApiKey));
			httpPost.setHeader("Content-Type", "application/json");
			httpPost.setHeader("Accept", "*/*");

			JSONObject discordPayload = new JSONObject();
			discordPayload.put("userId", usersDTO.getId());
			discordPayload.put("discordId", discordIds.get(orderId ));
			StringEntity input = new StringEntity(discordPayload.toString());
			httpPost.setEntity(input);
			httpResponse = client.execute(httpPost);
			rd = new BufferedReader (new InputStreamReader(httpResponse.getEntity().getContent()));
			line = "";
			while ((line = rd.readLine()) != null) {
				logger.info(line);
			}
		}

		for (String email : discordIds.keySet()) {
			System.out.println(email + "," + discordIds.get(email));
		}

		logger.info("Done!");
	}

	@Test
	public void testGetCrowdfundData() throws Exception {

		OrderApi orderApi = new OrderApi();
		HttpBearerAuth orderAuth = (HttpBearerAuth) orderApi.getApiClient().getAuthentications().get("httpBearer");
		orderAuth.setBearerToken(eventbritePrivateToken);
		orderApi.getApiClient().setDebugging(false);

		AttendeeApi attendeeApi = new AttendeeApi();
		HttpBearerAuth attendeeAuth = (HttpBearerAuth) attendeeApi.getApiClient().getAuthentications().get("httpBearer");
		attendeeAuth.setBearerToken(eventbritePrivateToken);
		attendeeApi.getApiClient().setDebugging(false);


		ListOrdersbyEventIDresponse response = orderApi.listOrdersbyEventID(eventID, null, null, null, null,
				null, null, "attendees,merchandise");

		HashMap<String, ArrayList<String>> orders = new HashMap<>();

		int tickets = 0;
		BigDecimal totalRaised = BigDecimal.ZERO;
		if(response.getOrders() != null) {
			orders:
			for (Order order : response.getOrders()) {
//			Order fullOrder = orderApi.retrieveOrderbyID(order.getId());
			System.out.println("\norder " + order.getId());
				if (order.getAttendees() != null) {
					for (Attendee1 attendee : order.getAttendees()) {
						if(attendee.getTicketClassName() != null &&
								!attendee.getTicketClassName().contains("Member")) {
							continue;
						}
						// Payload includes cancelled and refunded orders, so skip those
						ArrayList<String> statuses = new ArrayList<>();
						boolean skip = false;
						if (Boolean.TRUE.equals(attendee.getCancelled())) {
							statuses.add("cancelled");
							skip = true;
						}
						if (Boolean.TRUE.equals(attendee.getRefunded())) {
							statuses.add("refunded");
							skip = true;
						}
						if (skip) {
							System.out.println("\t" + StringUtils.join(statuses, ",") + " " + attendee.getId() + " " + order.getEmail());
							continue orders;
						} else {
							System.out.println("\tattendee" + " " + attendee.getId() + " " + order.getEmail());
						}
						if (!orders.containsKey(order.getId())) {
							orders.put(order.getId(), new ArrayList<>());
						}
						orders.get(order.getId()).add(attendee.getId());
						tickets = tickets + 1;
					}
					if (order.getCosts() != null && order.getCosts().getBasePrice() != null) {
						totalRaised = totalRaised.add(order.getCosts().getBasePrice().getValue());
//						logger.info(order.getId() + "\t" + order.getEmail() + "\t" + order.getCosts().getBasePrice().getValue());
					}
				}
			}
			totalRaised = totalRaised.divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
		}
		logger.info("sold " + tickets + ", raising " + totalRaised);

	}



}
