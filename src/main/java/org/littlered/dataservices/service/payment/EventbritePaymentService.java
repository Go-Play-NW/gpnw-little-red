package org.littlered.dataservices.service.payment;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.wordpress.UsersDTO;
import org.littlered.dataservices.entity.wordpress.Usermeta;
import org.littlered.dataservices.entity.wordpress.Users;
import org.littlered.dataservices.repository.wordpress.interfaces.UsermetaJPAInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersJPAInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersRepositoryInterface;
import org.littlered.dataservices.service.UsersJPAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class EventbritePaymentService {

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private UsersJPAService usersJPAService;

	@Autowired
	private UsermetaJPAInterface usermetaJPAInterface;

	@Autowired
	private UsersRepositoryInterface usersRepositoryInterface;

	@Value("${eventbrite.api.key}")
	private String eventbriteApiKey;

	@Value("${eventbrite.event_id}")
	private String eventbriteEventId;


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

}
