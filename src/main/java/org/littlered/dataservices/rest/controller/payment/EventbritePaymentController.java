package org.littlered.dataservices.rest.controller.payment;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.wordpress.UsersDTO;
import org.littlered.dataservices.entity.wordpress.Users;
import org.littlered.dataservices.rest.params.payment.EventbriteOrder;
import org.littlered.dataservices.rest.params.payment.EventbriteToken;
import org.littlered.dataservices.service.SecurityService;
import org.littlered.dataservices.service.UsersService;
import org.littlered.dataservices.service.payment.EventbritePaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.logging.Logger;

@RestController
@RequestMapping("/payment/eventbrite")
@Api(description = "Operations pertaining to the Eventbrite payment processor.")
public class EventbritePaymentController {

	@Autowired
	private UsersService usersService;

	@Autowired
	private SecurityService securityService;

	@Autowired
	private EventbritePaymentService eventbritePaymentService;

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@ApiOperation(value = "Receives and stores a user token for Eventbrite.", notes = "The user must be logged into the UI.")
	@RequestMapping(value = "/token", method = RequestMethod.POST, produces = "application/json")
	public void receiveEventbriteUserToken(@RequestBody EventbriteToken eventbriteToken, HttpServletResponse response) throws Exception {
		Users user = usersService.getCurrentUser();
		eventbritePaymentService.receiveEventbriteUserToken(user, eventbriteToken.getCode());
		logger.info("Received Eventbrite code " + eventbriteToken.getCode() + " for user " + user.getDisplayName());
	}

	@ApiOperation(value = "Receives and stores an order ID for an Eventbrite ticket.", notes = "The user must be logged into the UI.")
	@RequestMapping(value = "/order", method = RequestMethod.POST, produces = "application/json")
	public void receiveEventbriteOrderIdForMe(@RequestBody EventbriteOrder eventbriteOrder, HttpServletResponse response) throws Exception {
		Users user = usersService.getCurrentUser();
		eventbritePaymentService.receiveEventbriteOrderId(user, eventbriteOrder.getOrderId());
		logger.info("Received Eventbrite order " + eventbriteOrder.getOrderId() + " for user " + user.getDisplayName());
	}

	@ApiOperation(value = "Display information on a user, searching by Eventbrite order id. Admin only.", response = UsersDTO.class)
	@RequestMapping(value = "/orderId/{orderId}", method = RequestMethod.GET)
	public UsersDTO findUserByEmail(@PathVariable(value="orderId")String orderId, HttpServletResponse response) throws Exception {
		UsersDTO user = null;
		try {
			securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
			user = eventbritePaymentService.findUserByEventbriteOrderId(orderId);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		catch (IllegalArgumentException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
		return user;
	}

	@ApiOperation(value = "", response = HashMap.class)
	@RequestMapping(value = "/crowdfundingData", method = RequestMethod.GET)
	public HashMap<String, String> crowdfundingData() {
		return eventbritePaymentService.getCrowdfundingDataCache();
	}

	@ApiOperation(value = "", response = HashMap.class)
	@RequestMapping(value = "/updateCrowdfundingData", method = RequestMethod.GET)
	public HashMap<String, String> updateCrowdfundingData() {
		return eventbritePaymentService.getCrowdfundingData();
	}

}
