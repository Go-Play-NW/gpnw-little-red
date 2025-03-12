package org.littlered.dataservices.service.payment;

import org.apache.commons.lang3.StringUtils;
import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.wordpress.UsersDTO;
import org.littlered.dataservices.entity.wordpress.Options;
import org.littlered.dataservices.entity.wordpress.Usermeta;
import org.littlered.dataservices.entity.wordpress.Users;
import org.littlered.dataservices.repository.eventManager.interfaces.OptionsRepositoryInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsermetaJPAInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersJPAInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersRepositoryInterface;
import org.littlered.dataservices.service.UsersJPAService;
import org.openapitools.client.api.AttendeeApi;
import org.openapitools.client.api.OrderApi;
import org.openapitools.client.auth.HttpBearerAuth;
import org.openapitools.client.model.Attendee1;
import org.openapitools.client.model.ListOrdersbyEventIDresponse;
import org.openapitools.client.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import static org.littlered.dataservices.Constants.EVENTBRITE_CROWDFUNDING_DATA;

@Service
public class EventbritePaymentService {

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private UsersJPAService usersJPAService;

	@Autowired
	private UsermetaJPAInterface usermetaJPAInterface;

	@Autowired
	private UsersRepositoryInterface usersRepositoryInterface;

	@Autowired
	private OptionsRepositoryInterface optionsRepositoryInterface;

	@Value("${eventbrite.api.key}")
	private String eventbriteApiKey;

	@Value("${eventbrite.event_id}")
	private String eventbriteEventId;

	@Value("${eventbrite.private.token}")
	private String eventbritePrivateToken;


	public void receiveEventbriteUserToken(Users user, String code ) throws Exception {

		logger.info("Processing user token for " + user.getDisplayName());
		Usermeta tokenMeta = usersJPAService.findUsermetaByUserIdAndMetaKey(user, Constants.EVENTBRITE_TOKEN_USERMETA_KEY);
		if (tokenMeta != null) {
			tokenMeta.setMetaValue(code);
			usermetaJPAInterface.save(tokenMeta);
			return;
		}
		usersJPAService.createUserMeta(user, Constants.EVENTBRITE_TOKEN_USERMETA_KEY, code);
	}

	public void receiveEventbriteOrderId(Users user, String orderId ) throws Exception {

		logger.info("Processing order ID " + orderId + " for " + user.getDisplayName());
		Usermeta tokenMeta = usersJPAService.findUsermetaByUserIdAndMetaKey(user, Constants.EVENTBRITE_ORDER_USERMETA_PREFIX
				.concat(eventbriteEventId));
		if (tokenMeta != null && tokenMeta.getMetaValue().equals(orderId)) {
			logger.info("User " + user.getDisplayName() + " already has an order with the id " + orderId);
			return;
		}
		String key = Constants.EVENTBRITE_ORDER_USERMETA_PREFIX.concat(eventbriteEventId);
		usersJPAService.createUserMeta(user, key, orderId);
		usersJPAService.removeUserRole(user.getId(), Constants.ROLE_NOTATTENDING);
		usersJPAService.addUserRole(user.getId(), Constants.ROLE_PAIDATTENDEE);
		getCrowdfundingData();
	}

	public UsersDTO findUserByEventbriteOrderId(String orderId) throws Exception {
		logger.info("Looking up user by Eventbrite order ID " + orderId);

		List<Usermeta> discordUsermetas = usermetaJPAInterface.findUsermetasByMetaKeyAndMetaValue(
				Constants.EVENTBRITE_ORDER_USERMETA_PREFIX.concat(eventbriteEventId), orderId);

		if(discordUsermetas.size() == 0) {
			throw new IllegalArgumentException("No account with that order ID found!");
		}
		if(discordUsermetas.size() > 1) {
			throw new IllegalArgumentException("Multiple accounts with that order ID found!");
		}

		ArrayList<Users> users = usersRepositoryInterface.findById(discordUsermetas.get(0).getUserId());
		UsersDTO dto = new UsersDTO();
		dto.wrapEntity(users.get(0));
		return dto;

	}

	public HashMap<String, String> getCrowdfundingData() {

		HashMap<String, String> crowdfundingData = new HashMap<>();

		OrderApi orderApi = new OrderApi();
		HttpBearerAuth orderAuth = (HttpBearerAuth) orderApi.getApiClient().getAuthentications().get("httpBearer");
		orderAuth.setBearerToken(eventbritePrivateToken);
		orderApi.getApiClient().setDebugging(false);

		AttendeeApi attendeeApi = new AttendeeApi();
		HttpBearerAuth attendeeAuth = (HttpBearerAuth) attendeeApi.getApiClient().getAuthentications().get("httpBearer");
		attendeeAuth.setBearerToken(eventbritePrivateToken);
		attendeeApi.getApiClient().setDebugging(false);

		try {
			ListOrdersbyEventIDresponse response = orderApi.listOrdersbyEventID(eventbriteEventId, null, null, null, null,
					null, null, "attendees");

			HashMap<String, ArrayList<String>> orders = new HashMap<>();

			int tickets = 0;
			BigDecimal totalRaised = BigDecimal.ZERO;
			if (response.getOrders() != null) {
				orders:
				for (Order order : response.getOrders()) {
					if (order.getAttendees() != null) {
						for (Attendee1 attendee : order.getAttendees()) {
							// Payload includes cancelled and refunded orders, so skip those
							if (Boolean.TRUE.equals(attendee.getCancelled()) || Boolean.TRUE.equals(attendee.getRefunded())) {
								continue orders;
							}
							if (!orders.containsKey(order.getId())) {
								orders.put(order.getId(), new ArrayList<>());
							}
							orders.get(order.getId()).add(attendee.getId());
						}
						tickets = tickets + 1;
						if (order.getCosts() != null && order.getCosts().getBasePrice() != null) {
							totalRaised = totalRaised.add(order.getCosts().getBasePrice().getValue());
						}
					}
				}
				totalRaised = totalRaised.divide(BigDecimal.valueOf(100L), 2, RoundingMode.HALF_UP);
			}
			logger.fine("sold " + tickets + ", raising " + totalRaised);
			crowdfundingData.put("tickets", String.valueOf(tickets));
			crowdfundingData.put("totalRaised", String.valueOf(totalRaised));
		} catch (Exception e) {

		}
		cacheCrowdfundingData(crowdfundingData);
		return crowdfundingData;
	}

	public void cacheCrowdfundingData(HashMap<String, String> crowdfundingData) {
		String crowdfundingDataCache = crowdfundingData.get("tickets") +
				"|" + crowdfundingData.get("totalRaised");
		Options crowdFundingOptions = optionsRepositoryInterface.findByName(EVENTBRITE_CROWDFUNDING_DATA);
		if (crowdFundingOptions == null) {
			crowdFundingOptions = new Options();
			crowdFundingOptions.setAutoload("yes");
			crowdFundingOptions.setOptionName(EVENTBRITE_CROWDFUNDING_DATA);
			crowdFundingOptions.setOptionValue(crowdfundingDataCache);
		} else {
			crowdFundingOptions.setOptionValue(crowdfundingDataCache);
		}

		optionsRepositoryInterface.save(crowdFundingOptions);
	}

	public HashMap<String, String> getCrowdfundingDataCache() {
		HashMap<String, String> crowdfundingData = new HashMap<>();
		Options crowdfundingDataCache = optionsRepositoryInterface.findByName(EVENTBRITE_CROWDFUNDING_DATA);
		if (crowdfundingDataCache == null) {
			crowdfundingData.put("tickets", String.valueOf(0));
			crowdfundingData.put("totalRaised", String.valueOf(0));
		} else {
			String[] data = crowdfundingDataCache.getOptionValue().split("\\|");
			crowdfundingData.put("tickets", data[0]);
			crowdfundingData.put("totalRaised", data[1]);
		}
		return crowdfundingData;
	}
}
