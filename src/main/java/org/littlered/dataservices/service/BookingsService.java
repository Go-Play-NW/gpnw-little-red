package org.littlered.dataservices.service;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.entity.wordpress.*;
import org.littlered.dataservices.entity.eventManager.EmBookings;
import org.littlered.dataservices.entity.eventManager.EmEvents;
import org.littlered.dataservices.entity.eventManager.EmTickets;
import org.littlered.dataservices.entity.eventManager.EmTicketsBookings;
import org.littlered.dataservices.entity.wordpress.BbcBookingCountView;
import org.littlered.dataservices.entity.wordpress.Postmeta;
import org.littlered.dataservices.entity.wordpress.shrt.BbcEventCategories;
import org.littlered.dataservices.entity.wordpress.shrt.BbcUsersShort;
import org.littlered.dataservices.repository.eventManager.interfaces.*;
import org.littlered.dataservices.repository.wordpress.interfaces.PostMetaJPAInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersRepositoryInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersShortRepositoryInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Jeremy on 4/2/2017.
 */
@Service
@Scope("singleton")
public class BookingsService {


	@Autowired
	private BookingsRepositoryInterface bookingsDAO;

	@Autowired
	private TicketsRepositoryInterface ticketsDAO;

	@Autowired
	private TicketsBookingsRepositoryInterface ticketsBookingsDAO;

	@Autowired
	private EventsRepositoryInterface eventsDAO;

	@Autowired
	private UsersRepositoryInterface usersDAO;

	@Autowired
	private UsersShortRepositoryInterface usersShortDAO;

	@Autowired
	private OptionsRepositoryInterface optionsDAO;

	@Autowired
	private EmailService emailService;

	@Autowired
	private BookingsService bookingsService;

	@Autowired
	private SecurityService securityService;

	@Autowired
	private PostMetaJPAInterface postMetaJPAInterface;

	@Value("${display.year.filter}")
	private String yearFilter;

	@Value("${bookings.quota}")
	private String quota;

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String pendingPlayerSubject = "dbem_bookings_email_pending_subject";
	private static final String pendingPlayerBody = "dbem_bookings_mail_pending_body";
	private static final String confirmedPlayerSubject = "dbem_bookings_email_confirmed_subject";
	private static final String confirmedPlayerBody = "dbem_bookings_email_confirmed_body";
	private static final String rejectedPlayerSubject = "dbem_bookings_email_rejected_subject";
	private static final String rejectedPlayerBody = "dbem_bookings_email_rejected_body";
	private static final String cancelledPlayerSubject = "dbem_bookings_email_cancelled_subject";
	private static final String cancelledPlayerBody = "dbem_bookings_email_cancelled_body";

	private static final String pendingHostSubject = "dbem_bookings_contact_email_pending_subject";
	private static final String pendingHostBody = "dbem_bookings_contact_email_pending_body";
	private static final String confirmedHostSubject = "dbem_bookings_contact_email_confirmed_subject";
	private static final String confirmedHostBody = "dbem_bookings_contact_email_confirmed_body";
	private static final String rejectedHostSubject = "dbem_bookings_contact_email_rejected_subject";
	private static final String rejectedHostBody = "dbem_bookings_contact_email_rejected_body";
	private static final String cancelledHostSubject = "dbem_bookings_contact_email_cancelled_subject";
	private static final String cancelledHostBody = "dbem_bookings_contact_email_cancelled_body";

	public HashMap<String, String> stateTable;

	/**
	 * <p>This method is only to be called by the game registration consumer. It assumes the UI has done some sort
	 * of check that the user isn't already booked in the game. The WP UI does this.</p>
	 * <p>Admin users can call the addPlayerToGame method instead.</p>
	 * @param eventId
	 * @param userId
	 * @param uuid
	 */
	@Transactional
	public void createBooking(Long eventId, Long userId, Boolean isGm, String uuid) {
		logger.info("Creating booking for request ID " + uuid);
		EmEvents event = eventsDAO.findOne(eventId);
		logger.info("FavoriteEvent found: " + event.toString());
		org.littlered.dataservices.entity.wordpress.Users user = usersDAO.findOne(userId);
		BbcUsersShort bbcUsersShort = usersShortDAO.findOne(userId);
		logger.info("User found: " + user.toString());

		Timestamp now = new Timestamp(System.currentTimeMillis());

		EmTickets ticket = ticketsDAO.findTicketByEventId(event.getEventId());
		if (ticket == null) {
			logger.info("FavoriteEvent " + event.getEventId() + " is not open for registration yet.");
			bookingsService.stateTable.put(uuid, Constants.MESSAGE_EVENT_NOT_OPEN);
			return;
		}
		logger.info("Found ticket: " + ticket.toString());

		// Does the user have a badge?
		try {
			securityService.checkRolesForUser(user, Constants.ROLE_LIST_HAS_BADGE);
		} catch (Exception e) {
			logger.info("User " + user.getDisplayName() + " does not have a role indicating a badge.");
			bookingsService.stateTable.put(uuid, Constants.MESSAGE_NO_BADGE_ROLE);
			return;
		}

		// Has the user filled their quota?
		// TODO figure out how to get this dynamically
		int quotaInt = Integer.parseInt(quota);
		// Quota == 0 means no quota
		if (quotaInt < Constants.UNLIMITED_QUOTA) {
			List<BbcBookingCountView> quotaEvents =
					bookingsDAO.findBookingsForQuotaCount(userId, Integer.parseInt(getFilterYear()));
			if (quotaEvents != null && quotaEvents.size() > quotaInt) {
				logger.info("User " + user.getDisplayName() + " has already booked their quota of events.");
				bookingsService.stateTable.put(uuid, Constants.MESSAGE_OVER_QUOTA);
				return;
			}
		}


		// Teen game booking logic
		boolean isUserTeen = false;
		try {
			securityService.checkRolesForUser(user, Constants.ROLE_LIST_TEEN);
		} catch (Exception e) {
			isUserTeen = true;
		}

		boolean isEventTeen = false;
		for (BbcEventCategories category : event.getCategories()) {
			if (category.getSlug().equals(Constants.TEEN_CATEGORY_SLUG)) {
				isEventTeen = true;
				break;
			}
		}

		// Is user a non teen booking into a teen game while we restrict to quota?
		if (isEventTeen && !isUserTeen && quotaInt < Constants.UNLIMITED_QUOTA) {
			logger.info("User " + user.getDisplayName() + " is a non-teen trying to book a teen event while quotas are in effect.");
			bookingsService.stateTable.put(uuid, Constants.MESSAGE_TEEN_RESTRICTION);
			return;
		}


		boolean isUserVolunteer = true;
		try {
			securityService.checkRolesForUser(user, Constants.ROLE_LIST_VOLUNTEER);
		} catch (Exception e) {
			isUserVolunteer = false;
		}

		boolean isEventVolunteer = false;
		for (Postmeta postmeta : event.getMetadata()) {
			if(postmeta.getMetaKey().equals(Constants.VOLUNTEER_SHIFT_POSTMETA) && postmeta.getMetaValue().equals("1")){
				isEventVolunteer = true;
			}
		}

		// Is user a non volunteer booking into a volunteer game while we restrict to quota?
		if (isEventVolunteer && !isUserVolunteer ) { //&& quotaInt < Constants.UNLIMITED_QUOTA) {
			logger.info("User " + user.getDisplayName() + " is a non-volunteer trying to book a volunteer event while quotas are in effect.");
			bookingsService.stateTable.put(uuid, Constants.MESSAGE_VOLUNTEER_RESTRICTION);
			return;
		}


		boolean isUserVendor = true;
		try {
			securityService.checkRolesForUser(user, Constants.ROLE_LIST_VENDOR);
		} catch (Exception e) {
			isUserVendor = false;
		}

		boolean isEventVendor = false;
		for (Postmeta postmeta : event.getMetadata()) {
			if(postmeta.getMetaKey().equals(Constants.VENDOR_SHIFT_POSTMETA) && postmeta.getMetaValue().equals("1")){
				isEventVendor = true;
			}
		}

		// Is user a non vendor booking into a vendor game while we restrict to quota?
		if (isEventVendor && !isUserVendor) { // && quotaInt < Constants.UNLIMITED_QUOTA) {
			logger.info("User " + user.getDisplayName() + " is a non-vendor trying to book a vendor event while quotas are in effect.");
			bookingsService.stateTable.put(uuid, Constants.MESSAGE_VENDOR_RESTRICTION);
			return;
		}


		// Is the user already in the game?
		List<EmBookings> bookingsList = bookingsDAO.findForEvent(event.getEventId(), Integer.parseInt(getFilterYear()));
		for (EmBookings booking : bookingsList) {
			if (booking.getUser().getId().equals(userId)) {
				logger.info("User " + user.getDisplayName() + " has already signed up for event " + event.getEventName());
				bookingsService.stateTable.put(uuid, Constants.MESSAGE_ALREADY_BOOKED);
				return;
			}
		}

		// Has the user already failed to book into the game?
		// Used because we track initial attempts to get into a full game for the popularity metric
		List<EmBookings> overbookingsForUser =
				bookingsDAO.findOverbookingForUserAndEvent(userId, eventId, Integer.parseInt(getFilterYear()));
		if (overbookingsForUser != null && overbookingsForUser.size() > 1) {
			logger.info("User " + overbookingsForUser.get(0).getUser().getDisplayName()
					+ " has already tried to join the full event " + event.getEventName());
			bookingsService.stateTable.put(uuid, Constants.MESSAGE_ALREADY_OVERBOOKED);
			return;
		}

		/*
		List<Bookings> conflictingEvents = eventsDAO.findConflictingEventsForUserAndDateTime(userId, event.getEventStartDate(), event.getEventStartTime());
		if(conflictingEvents != null && conflictingEvents.size() > 0) {
			logger.info("User " + user.getDisplayName() + " has booked into one or more events at the same time as event" + eventId + ".");
			bookingsService.stateTable.put(uuid, Constants.MESSAGE_CONFLICTING_SCHEDULE.concat(":").concat(conflictingEvents.get(0).getEvent().getEventName().toString()));
			return;
		}
		*/

		Calendar adjustedEndTimeCalendar = Calendar.getInstance();
		adjustedEndTimeCalendar.setTime(getDate(event.getEventEndDate(), event.getEventEndTime()));
		adjustedEndTimeCalendar.add(Calendar.MINUTE, -5);
		Date adjustedEndTime = adjustedEndTimeCalendar.getTime();

		Calendar adjustedStartTimeCalendar = Calendar.getInstance();
		adjustedStartTimeCalendar.setTime(getDate(event.getEventStartDate(), event.getEventStartTime()));
		adjustedStartTimeCalendar.add(Calendar.MINUTE, 5);
		Date adjustedStartTime = adjustedStartTimeCalendar.getTime();

		List<EmBookings> conflictingEvents2 = bookingsDAO.findConflictingEventsForUserAndStartDateTime(
				userId, adjustedStartTime);
		conflictingEvents2.addAll(bookingsDAO.findConflictingEventsForUserAndEndDateTime(
				userId, adjustedEndTime));
		conflictingEvents2.addAll(bookingsDAO.findNestedConflictingEventsUserAndStartAndEnd(
				userId,
				getDate(event.getEventStartDate(), event.getEventStartTime()),
				getDate(event.getEventEndDate(), event.getEventEndTime())));
		if(conflictingEvents2 != null && conflictingEvents2.size() > 0) {
			logger.info("User " + user.getDisplayName() + " has booked into one or more events at the same time as event" + eventId + ".");
			bookingsService.stateTable.put(uuid, Constants.MESSAGE_CONFLICTING_SCHEDULE.concat(":").concat(conflictingEvents2.get(0).getEventId().getEventName().toString()));
			return;
		}

		Integer bookingSpaces = 1;
		Integer bookingStatus = Constants.STATUS_CODE_BOOKED;
		String bookingMessage = Constants.MESSAGE_BOOKED;

		// Is there room in the event?
		if (bookingsList.size() >= ticket.getTicketSpaces()) {
			bookingSpaces = 0;
			bookingStatus = Constants.STATUS_CODE_OVERBOOKED;
			bookingMessage = Constants.MESSAGE_OVERBOOKED;
		}

		doBooking(isGm, event, bbcUsersShort, now, ticket, bookingSpaces, bookingStatus);


		//TODO send rejection mail for overbookings
		try {
			if (bookingStatus.equals(Constants.STATUS_CODE_BOOKED)) {
				emailService.sendEmail(createConfirmedPlayerEmail(user, event));
				emailService.sendEmail(createConfirmedHostEmail(user, event, ticket, bookingsList.size()));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		bookingsService.stateTable.put(uuid, bookingMessage);

	}

	public void doBooking(Boolean isGm, EmEvents event, BbcUsersShort bbcUsersShort, Timestamp now, EmTickets ticket,
						  Integer bookingSpaces, Integer bookingStatus) {
		EmBookings booking = new EmBookings();
		booking.setEventId(event);
		booking.setUser(bbcUsersShort);
		booking.setBookingSpaces(bookingSpaces);
		booking.setBookingDate(now);
		booking.setBookingStatus(bookingStatus);
		booking.setBookingMeta("a:0:{}");
		booking.setBookingPrice(BigDecimal.ZERO);
		booking.setBookingTaxRate(BigDecimal.ZERO);
		if (isGm) {
			booking.setBookingComment("GM");
		}
		//bookingsDAO.save(booking);

		EmTicketsBookings ticketBooking = new EmTicketsBookings();
		ticketBooking.setBookingId(booking);
		ticketBooking.setTicketId(ticket.getTicketId());
		ticketBooking.setTicketBookingSpaces(bookingSpaces);
		ticketBooking.setTicketBookingPrice(new BigDecimal(0));
		ticketsBookingsDAO.save(ticketBooking);
	}

	public static Date getDate(Date date, Time time) {
		Calendar calendar=Calendar.getInstance();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTime(date);
		calendar.add(Calendar.HOUR, time.toLocalTime().getHour());
		calendar.add(Calendar.MINUTE, time.toLocalTime().getMinute());
		calendar.add(Calendar.SECOND, time.toLocalTime().getSecond());
		return calendar.getTime();
	}

	/**
	 * <p>This method first checks if a player is already booked in a game, then calls createBooking if they aren't.</p>
	 * <p>Admin only. Throws an exception if the logged in user isn't an admin.</p>
	 * @param userId
	 * @param eventId
	 * @param uuid
	 * @throws Exception if the user currently is booked in the game.
	 */
	public void addPlayerToGame(Long userId, Long eventId, Boolean isGm, String uuid) throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		if(checkIfUserIsInGame(userId, eventId).size() > 0) {
			throw new Exception("That user is already booked in that game!");
		}
		createBooking(eventId, userId, isGm, uuid);
	}

	/**
	 * <p>This method first checks if a player is already booked in a game, then calls cancelBooking if they are.</p>
	 * <p>Admin only. Throws an exception if the logged in user isn't an admin.</p>
	 * @param userId
	 * @param eventId
	 * @param uuid
	 * @throws Exception if the user is not currently booked in the game.
	 */
	//@Transactional
	public void removePlayerFromGame(Long userId, Long eventId, String uuid, boolean isMe)  throws Exception {
		if (!isMe) {
			securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		}
		logger.info("Removing booking for request ID " + uuid);
		List<EmTicketsBookings> ticketsBookings = checkIfUserIsInGame(userId, eventId);
		if(ticketsBookings.size() == 0) {
			throw new Exception("That user not booked in that game!");
		}
		EmTicketsBookings ticketsBooking = ticketsBookings.get(ticketsBookings.size() - 1);
		ticketsBooking.getBookingId().setBookingStatus(Constants.STATUS_CODE_DELETED);
		ticketsBooking.setTicketBookingSpaces(0);
		ticketsBookingsDAO.save(ticketsBooking);

		//TODO should we send an email in this instance?  This action will be performed by an admin, and a player has to ask them to do it.

	}

	public void setGmFlagForPlayerInGame(Long userId, Long eventId, Boolean setGm) throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		List<EmBookings> bookingsForPlayerAndGame = bookingsDAO.findBookingsByEventIdandUserId(eventId, userId);
		if (bookingsForPlayerAndGame == null || bookingsForPlayerAndGame.size() == 0) {
			throw new IllegalArgumentException("No booking found for user " + userId + " and game " + eventId + "!");
		}
		if (bookingsForPlayerAndGame.size() > 1) {
			throw new IllegalArgumentException("Multiple bookings found for user " + userId + " and game " + eventId + "!");
		}
		EmBookings booking = bookingsForPlayerAndGame.get(0);
		if (setGm) {
			if (booking.getBookingComment() != null && booking.getBookingComment().equals("GM")) {
				throw new IllegalArgumentException("User " + userId + " is already a GM of game " + eventId + "!");
			}
			booking.setBookingComment("GM");
		} else {
			if (booking.getBookingComment() == null || !booking.getBookingComment().equals("GM")) {
				throw new IllegalArgumentException("User " + userId + " not currently a GM of game " + eventId + "!");
			}
			booking.setBookingComment(null);
		}

		bookingsDAO.save(booking);

		// Update the GM record in postmeta
		ArrayList<Postmeta> gmPostmetas = postMetaJPAInterface.findByPostIdAndMetaKey(
				booking.getEventId().getPostId(), "GM");
		Users user = usersDAO.findOne(userId);
		if (gmPostmetas != null) {
			if (gmPostmetas.size() > 1) {
				logger.log(Level.SEVERE, "More than one GM record found for event " + eventId);
			}
			Postmeta gmPostmeta = gmPostmetas.get(0);
			gmPostmeta.setMetaValue(user.getDisplayName());
			postMetaJPAInterface.save(gmPostmeta);
		} else {
			logger.info("No GM record found for event " + eventId + ": creating one");
			Postmeta gmPostmeta = new Postmeta();
			gmPostmeta.setPostId(booking.getEventId().getPostId());
			gmPostmeta.setMetaKey("GM");
			gmPostmeta.setMetaValue(user.getDisplayName());
		}

	}

	private List<EmTicketsBookings> checkIfUserIsInGame (Long userId, Long eventId) {
		return ticketsBookingsDAO.findForEventAndUser(eventId, userId);
	}

	// automated email methods for booking activities

	private HashMap<String, String> createConfirmedPlayerEmail(org.littlered.dataservices.entity.wordpress.Users user, EmEvents event) throws Exception {

		HashMap<String, String> playerEmail = new HashMap<>();
		String subject = optionsDAO.findByName(confirmedPlayerSubject).getOptionValue();
		String body = optionsDAO.findByName(confirmedPlayerBody).getOptionValue();

		body = body.replaceAll("#_BOOKINGNAME", user.getDisplayName());
		body = body.replaceAll("#_NAME", event.getEventName());

		subject = subject.replaceAll("#_NAME", event.getEventName());
		if (subject == null || subject.equals("")) {
			return null;
		}

		playerEmail.put("subject", subject);
		playerEmail.put("body", body);
		playerEmail.put("to", user.getUserEmail());
		return playerEmail;

	}

	private HashMap<String, String> createConfirmedHostEmail(org.littlered.dataservices.entity.wordpress.Users user, EmEvents event, EmTickets ticket, Integer takenSpaces) throws Exception {

		HashMap<String, String> hostEmail = new HashMap<>();
		String subject = optionsDAO.findByName(confirmedHostSubject).getOptionValue();
		String body = optionsDAO.findByName(confirmedHostBody).getOptionValue();
		String month = new SimpleDateFormat("MMMM").format(event.getEventStartDate());
		String day = new SimpleDateFormat("dd").format(event.getEventStartDate());
		String year = new SimpleDateFormat("YYYY").format(event.getEventStartDate());
		String time = new SimpleDateFormat("h:mm a").format(event.getEventStartTime());

		String booked = takenSpaces.toString();
		String remaining = new Integer(ticket.getTicketSpaces() - takenSpaces).toString();


		body = body.replaceAll("#_BOOKINGNAME", user.getDisplayName());
		body = body.replaceAll("#_BOOKINGEMAIL", user.getUserEmail());
		body = body.replaceAll("#_NAME", event.getEventName());
		body = body.replaceAll("#F", month);
		body = body.replaceAll("#j", day);
		body = body.replaceAll("#Y", year);
		body = body.replaceAll("#_12HSTARTTIME", time);
		body = body.replaceAll("#_BOOKEDSPACES", booked);
		body = body.replaceAll("#_AVAILABLESPACES", remaining);

		subject = subject.replaceAll("#_NAME", event.getEventName());
		if (subject == null || subject.equals("")) {
			return null;
		}

		org.littlered.dataservices.entity.wordpress.Users userFull = usersDAO.findOne(event.getEventOwner().getId());

		hostEmail.put("subject", subject);
		hostEmail.put("body", body);
		hostEmail.put("to", userFull.getUserEmail());
		return hostEmail;

	}

	private HashMap<String, String> createRejectedPlayerEmail(org.littlered.dataservices.entity.wordpress.Users user, EmEvents event) throws Exception {

		HashMap<String, String> playerEmail = new HashMap<>();
		String subject = optionsDAO.findByName(rejectedPlayerSubject).getOptionValue();
		String body = optionsDAO.findByName(rejectedPlayerBody).getOptionValue();

		body = body.replaceAll("#_BOOKINGNAME", user.getDisplayName());
		body = body.replaceAll("#_NAME", event.getEventName());

		subject = subject.replaceAll("#_NAME", event.getEventName());

		playerEmail.put("subject", subject);
		playerEmail.put("body", body);
		playerEmail.put("to", user.getUserEmail());
		return playerEmail;

	}

	private HashMap<String, String> createRejectedHostEmail(org.littlered.dataservices.entity.wordpress.Users user, EmEvents event) throws Exception {

		HashMap<String, String> hostEmail = new HashMap<>();
		String subject = optionsDAO.findByName(rejectedHostSubject).getOptionValue();
		String body = optionsDAO.findByName(rejectedHostBody).getOptionValue();

		body = body.replaceAll("#_BOOKINGNAME", user.getDisplayName());
		body = body.replaceAll("#_BOOKINGEMAIL", user.getUserEmail());
		body = body.replaceAll("#_NAME", event.getEventName());

		subject = subject.replaceAll("#_NAME", event.getEventName());

		hostEmail.put("subject", subject);
		hostEmail.put("body", body);
		hostEmail.put("to", user.getUserEmail());
		return hostEmail;

	}

	private String getFilterYear() {
		String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
		if (yearFilter != null && !yearFilter.equals("")) {
			year = yearFilter;
		}
		return year;
	}

	public Integer getCountOfAvailableSlots(Long userId) {
		List<BbcBookingCountView> quotaEvents =
				bookingsDAO.findBookingsForQuotaCount(userId, Integer.parseInt(getFilterYear()));
		int filledSlots =  quotaEvents.size();
		if(quotaEvents == null) {
			filledSlots = 0;
		}
		int quotaInt = Integer.parseInt(quota);
		// Quota == 0 means no quota
		if (quotaInt < Constants.UNLIMITED_QUOTA) {
			return Math.max(quotaInt - quotaEvents.size(), 0);
		}
		return Constants.UNLIMITED_QUOTA;
	}

}
