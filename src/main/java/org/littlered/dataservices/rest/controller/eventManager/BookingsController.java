package org.littlered.dataservices.rest.controller.eventManager;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.eventManager.EventBookingReply;
import org.littlered.dataservices.entity.wordpress.Users;
import org.littlered.dataservices.rest.params.eventManager.GameBooking;
import org.littlered.dataservices.rest.params.eventManager.EventBookingData;
import org.littlered.dataservices.service.BookingsService;
import org.littlered.dataservices.service.UsersService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Jeremy on 5/6/2018.
 */
@RestController
@RequestMapping("/bookings")
@Api(description = "Operations pertaining to bookings.")
public class BookingsController {

	@Autowired
	private BookingsService bookingsService;

	@Autowired
	private UsersService usersService;

	@Value("${display.year.filter}")
	private String yearFilter;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@ApiOperation(
			value = "Book current user for a game",
			notes = "Book the currently logged in user into a game. Not to be confused with addUserToGame, which is meant to be called by administrators.",
			response = EventBookingReply.class)
	@RequestMapping(value = "/bookMeIntoGame", method = RequestMethod.POST, produces = "application/json")
	public EventBookingReply bookAuthenticatedUserIntoGame(@RequestBody GameBooking gameBooking) throws Exception {
		if (gameBooking != null)
			logger.info(gameBooking.toString());

		String uuid = UUID.randomUUID().toString();
		if(bookingsService.stateTable == null) {
			bookingsService.stateTable = new HashMap<>();
		}
		bookingsService.stateTable.put(uuid, null);
		gameBooking.setUuid(uuid);

		Long userId = usersService.getCurrentUser().getId();

		synchronized (this) {
			bookingsService.createBooking(gameBooking.getGameId(), userId, false, uuid);
			//this.notify();
		}

		for (int i = 0; i < 13 ; i++) {
			if (bookingsService.stateTable.get(uuid) != null) {
				break;
			}
			Thread.sleep(5000);
		}

		EventBookingReply reply = new EventBookingReply();
		reply.setGuid(uuid);

		if (bookingsService.stateTable.containsKey(uuid)) {
			String message = bookingsService.stateTable.get(uuid);
			if (message == null) {
				reply.setStatus(Constants.STATUS_UNKNOWN);
			}
			reply.setMessage(message);
			if (message.equals(Constants.MESSAGE_BOOKED)) {
				reply.setStatus(Constants.STATUS_SUCCESS);
			}
			else {
				reply.setStatus(Constants.STATUS_FAILURE);
			}
			bookingsService.stateTable.remove(uuid);
		} else {
			reply.setStatus(Constants.STATUS_UNKNOWN);
		}
		return reply;
	}


	@ApiOperation(
			value = "Remove current user from a game",
			notes = "Remove the currently logged in user from a game. Not to be confused with removeUserFromGame, which is meant to be called by administrators.",
			response = EventBookingReply.class)
	@RequestMapping(value = "/removeMeFromGame", method = RequestMethod.DELETE, produces = "application/json")
	public EventBookingReply removeAuthenticatedUserFromGame(@RequestBody GameBooking gameBooking, HttpServletResponse response) throws Exception {
		if(gameBooking != null)
			logger.info(gameBooking.toString());

		String uuid = UUID.randomUUID().toString();
		EventBookingReply reply = new EventBookingReply();
		reply.setGuid(uuid);
		Long userId = usersService.getCurrentUser().getId();

		try {
			bookingsService.removePlayerFromGame(userId, gameBooking.getGameId(), UUID.randomUUID().toString(), false);
			reply.setStatus(Constants.STATUS_SUCCESS);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			reply.setMessage(e.getLocalizedMessage());
			reply.setStatus(Constants.STATUS_FAILURE);
		}
		catch (Exception e) {
			reply.setStatus(Constants.STATUS_FAILURE);
			reply.setMessage(e.getLocalizedMessage());
		}
		return reply;
	}

	@ApiOperation(
			value = "Add a registered user to a game",
			notes = "Admin only. \nStatus and Message fields are for output only, and will be ignored on input.",
			response = EventBookingData.class)
	@RequestMapping(value = "/addUserToGame", method = RequestMethod.POST, produces = "application/json")
	public EventBookingData addUserToGame(@RequestBody EventBookingData eventBookingData, HttpServletResponse response) throws Exception {
		if(eventBookingData != null)
			logger.info(eventBookingData.toString());
		UUID uuid = UUID.randomUUID();
		if(bookingsService.stateTable == null) {
			bookingsService.stateTable = new HashMap<>();
		}
		try {
			if (eventBookingData.getIsGm() == null) {
				eventBookingData.setIsGm(false);
			}
			bookingsService.addPlayerToGame(eventBookingData.getUserId(), eventBookingData.getEventId(),
					eventBookingData.getIsGm(), uuid.toString());
			eventBookingData.setStatus(Constants.STATUS_SUCCESS);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			eventBookingData.setStatus(Constants.STATUS_FAILURE);
		}

		if (bookingsService.stateTable.containsKey(uuid.toString())) {
			String message = bookingsService.stateTable.get(uuid.toString());
			eventBookingData.setMessage(message);
			if (message.equals(Constants.MESSAGE_BOOKED)) {
				eventBookingData.setStatus(Constants.STATUS_SUCCESS);
			}
			else {
				eventBookingData.setStatus(Constants.STATUS_FAILURE);
			}
			bookingsService.stateTable.remove(uuid.toString());
		} else {
			eventBookingData.setStatus(Constants.STATUS_UNKNOWN);
		}

		return eventBookingData;
	}

	@ApiOperation(
			value = "Remove a registered user from a game",
			notes = "Admin only. \nStatus and Message fields are for output only, and will be ignored on input.",
			response = EventBookingData.class)
	@RequestMapping(value = "/removeUserFromGame", method = RequestMethod.DELETE, produces = "application/json")
	public EventBookingData removeUserFromGame(@RequestBody EventBookingData eventBookingData, HttpServletResponse response) throws Exception {
		if(eventBookingData != null)
			logger.info(eventBookingData.toString());
		try {
			bookingsService.removePlayerFromGame(eventBookingData.getUserId(), eventBookingData.getEventId(), UUID.randomUUID().toString(), true);
			eventBookingData.setStatus(Constants.STATUS_SUCCESS);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			eventBookingData.setMessage(e.getLocalizedMessage());
			eventBookingData.setStatus(Constants.STATUS_FAILURE);
		}
		catch (Exception e) {
			eventBookingData.setStatus(Constants.STATUS_FAILURE);
			eventBookingData.setMessage(e.getLocalizedMessage());
		}
		return eventBookingData;
	}

	@ApiOperation(
			value = "Set the GM flag for a user who is booked in a game",
			notes = "Admin only. \nStatus and Message fields are for output only, and will be ignored on input.",
			response = EventBookingData.class)
	@RequestMapping(value = "/setGmStatusForPlayerInGame", method = RequestMethod.POST, produces = "application/json")
	public EventBookingData setGmStatusForPlayerInGame(@RequestBody EventBookingData eventBookingData, HttpServletResponse response) throws Exception {
		if(eventBookingData != null)
			logger.info(eventBookingData.toString());
		try {
			bookingsService.setGmFlagForPlayerInGame(eventBookingData.getUserId(), eventBookingData.getEventId(), eventBookingData.getIsGm());
			eventBookingData.setStatus(Constants.STATUS_SUCCESS);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			eventBookingData.setStatus(Constants.STATUS_FAILURE);
		}
		catch (Exception e) {
			eventBookingData.setStatus(Constants.STATUS_FAILURE);
			eventBookingData.setMessage(e.getLocalizedMessage());
		}
		return eventBookingData;
	}

	@ApiOperation(value = "Count of events for a given year", response = Long.class)
	@RequestMapping(value = "/myAvailableSlots", method = RequestMethod.GET, produces = "application/json")
	public Integer getMyCountOfAvailableSlots() throws Exception {
		Users user =  usersService.findMe();
		return bookingsService.getCountOfAvailableSlots(user.getId());
	}

}
