package org.littlered.dataservices.service;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.littlered.EventMetadataDTO;
import org.littlered.dataservices.dto.eventManager.CreateEventDTO;
import org.littlered.dataservices.entity.eventManager.EmEvents;
import org.littlered.dataservices.entity.eventManager.EmTickets;
import org.littlered.dataservices.entity.wordpress.Postmeta;
import org.littlered.dataservices.entity.wordpress.Posts;
import org.littlered.dataservices.entity.wordpress.TermRelationships;
import org.littlered.dataservices.entity.wordpress.shrt.BbcUsersShort;
import org.littlered.dataservices.repository.wordpress.interfaces.PostMetaJPAInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.PostsJPAInterface;
import org.littlered.dataservices.repository.eventManager.interfaces.EventsJPARepositoryInterface;
import org.littlered.dataservices.repository.eventManager.interfaces.TermRelationshipsJPARepositoryInterface;
import org.littlered.dataservices.repository.eventManager.interfaces.TicketsRepositoryInterface;
import org.littlered.dataservices.util.UnicodeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/**
 * Created by Jeremy on 4/2/2017.
 */
@Service
@Transactional
@Configuration
public class EventsJPAService {

	@Autowired
	private PostsJPAInterface postsService;

	@Autowired
	private EventsJPARepositoryInterface eventsJPARepositoryService;

	@Autowired
	private TicketsRepositoryInterface ticketsService;

	@Autowired
	private PostMetaJPAInterface postMetaJPAService;

	@Autowired
	private TermRelationshipsJPARepositoryInterface termRelationshipsJPARepository;

	@Autowired
	private BookingsService bookingsService;

	@Autowired
	private EventsSlugService eventsSlugService;

	@Autowired
	private UsersJPAService usersJPAService;

	@Value("${display.year.filter}")
	private String yearFilter;

	@Value("${display.timezone}")
	private String defaultEventTimezone;

	@Value("${default.event.date}")
	private String defaultEventDate;

	@Value("${default.event.time}")
	private String defaultEventTime;

	@Value("${site.baseUri}")
	private String siteBaseUri;

	@Autowired
	private EntityManagerFactory emf;

	@Transactional
	public Long saveEvents(CreateEventDTO saveEvent, BbcUsersShort user) throws Exception {

		String defaultEventDateFormat = "yyyy-MM-dd";

		String defaultEventStartDate = yearFilter.concat("-").concat(defaultEventDate);
		String defaultEventEndDate = yearFilter.concat("-").concat(defaultEventDate);
		// TODO evaluate use of this field
		String defaultEventRsvpDate = yearFilter.concat("-").concat(defaultEventDate);

		String defaultEventTimeFormat = "HH:mm:ss";
		String defaultEventStartTime = defaultEventTime;
		String defaultEventEndTime = defaultEventTime;
		// TODO evaluate use of this field
		String defaultEventRsvpTime = defaultEventTime;

		Timestamp now = Timestamp.from(Instant.now());

		SimpleDateFormat dateFormat = new SimpleDateFormat(defaultEventDateFormat);
		SimpleDateFormat timeFormat = new SimpleDateFormat(defaultEventTimeFormat);
		String dateTimeFormatString = defaultEventDateFormat.concat(" ").concat(defaultEventTimeFormat);
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat(dateTimeFormatString);

		Date startDate;
		Date endDate;
		Time startTime;
		Time endTime;
		Date startDateTime;
		Date endDateTime;

		if (saveEvent.getEventStartDate() == null && saveEvent.getEventStartTime() == null) {
			startDate = dateFormat.parse(defaultEventStartDate);
			endDate = dateFormat.parse(defaultEventEndDate);

			startTime = new Time(timeFormat.parse(defaultEventStartTime).getTime());
			endTime = new Time(timeFormat.parse(defaultEventEndTime).getTime());

			startDateTime = new Time(dateTimeFormat.parse(defaultEventStartDate
					.concat(" ").concat(defaultEventStartTime)).getTime());
			endDateTime = new Time(dateTimeFormat.parse(defaultEventEndDate
					.concat(" ").concat(defaultEventEndTime)).getTime());
		} else {
			startDate = dateFormat.parse(saveEvent.getEventStartDate());

			startTime = new Time(timeFormat.parse(saveEvent.getEventStartTime()).getTime());

			String startDateString = saveEvent.getEventStartDate().concat(" " )
					.concat(saveEvent.getEventStartTime());
			startDateTime = new Date(dateTimeFormat.parse(startDateString).getTime());

			// Calculate end dates and times from start date + event duration
			Calendar endTimeCalendar = Calendar.getInstance();
			endTimeCalendar.setTimeZone(TimeZone.getTimeZone(defaultEventTimezone));
			endTimeCalendar.setTime(startDateTime);
			endTimeCalendar.add(Calendar.HOUR_OF_DAY, Integer.parseInt(saveEvent.getLength()));

			endDate = dateFormat.parse(dateFormat.format(endTimeCalendar.getTime()));
			endTime = new Time(endTimeCalendar.getTimeInMillis());
			endDateTime = endTimeCalendar.getTime();
		}


		if(saveEvent.getRunNumberOfTimes() > 4) {
			throw new Exception("Maximum number of times to run is four!");
		}

		EntityManager em = emf.createEntityManager();
		EntityTransaction etx = em.getTransaction();

		etx.begin();

		usersJPAService.updateUserDisplayName(saveEvent.getGm(), user);
		usersJPAService.addUserRole(user.getId(), Constants.ROLE_GM);

		usersJPAService.removeUserRole(user.getId(), Constants.ROLE_SUBSCRIBER);
		usersJPAService.removeUserRole(user.getId(), Constants.ROLE_SPAMMER);
		usersJPAService.removeUserRole(user.getId(), Constants.ROLE_NOTATTENDING);
		usersJPAService.removeUserRole(user.getId(), Constants.ROLE_NOTATTENDINGTHISYEAR);

		if (saveEvent.getGmAge().equals("13+")) {
			usersJPAService.addUserRole(user.getId(), Constants.ROLE_TEEN);
		}

		if (saveEvent.getGmAge().equals("18+")) {
			usersJPAService.removeUserRole(user.getId(), Constants.ROLE_TEEN);
		}

		try {
			Long eventId = 0l;
			for (int i = 0; i < saveEvent.getRunNumberOfTimes(); i++) {

				String eventSlug = eventsSlugService.slugify(saveEvent.getEventName());


				Posts post = new Posts();
				post.setPostAuthor(user.getId());
				post.setPostDate(now);
				post.setPostDateGmt(now);
				post.setPostContent(UnicodeUtils.replaceSmartQuotes(saveEvent.getEventDescription()));
				post.setPostTitle(UnicodeUtils.replaceSmartQuotes(saveEvent.getEventName()));
				post.setPostStatus("draft");
				post.setCommentStatus("open");
				post.setPostName(eventSlug);
				post.setPostModified(now);
				post.setPostModifiedGmt(now);
				post.setPostParent(0L);
				post.setGuid(siteBaseUri.concat("/").concat(eventSlug)); // investigate if this is used
				post.setPostType("event");
				post.setCommentCount(0L);
				post.setMenuOrder(0);
				post.setPostContentFiltered("");
				post.setPostExcerpt("");
				post.setPostMimeType("");
				post.setPostPassword("");
				post.setToPing("");
				post.setPinged("");
				post.setPingStatus("closed");
				postsService.saveAndFlush(post);

				EmEvents event = new EmEvents();
				// Fields from wp post object
				event.setEventOwner(user);
				event.setEventName(UnicodeUtils.replaceSmartQuotes(saveEvent.getEventName()));
				event.setPostContent(UnicodeUtils.replaceSmartQuotes(saveEvent.getEventDescription()));
				event.setPostId(post.getId());
				event.setEventSlug(eventSlug);
				event.setEventStatus(0);
				event.setEventStartDate(new java.sql.Date(startDate.getTime()));
				event.setEventEndDate(new java.sql.Date(endDate.getTime()));
				event.setEventStart(new Timestamp(startDateTime.getTime()));
				event.setEventEnd(new Timestamp(endDateTime.getTime()));
				event.setEventStartTime(startTime);
				event.setEventEndTime(endTime);
				event.setEventTimezone(defaultEventTimezone);
				event.setEventRsvp((byte) 0b1);
				event.setEventSpaces(0);
				event.setLocationId(0L);
				event.setEventDateCreated(now);
				event.setEventDateModified(now);
				event.setGroupId(0L);
				event.setEventAllDay(0);
				event.setEventPrivate((byte) 0b0);
				event.setEventRsvpSpaces(0);
				eventsJPARepositoryService.saveAndFlush(event);

				eventId = event.getEventId();

				EmTickets ticket = new EmTickets();
				ticket.setEventId(event.getEventId());
				ticket.setTicketPrice(BigDecimal.ZERO);
				ticket.setTicketStart(now);
				ticket.setTicketEnd(now);
				ticket.setTicketMin(1);
				ticket.setTicketMax(1);

				// Change to Players field and sanitize for non numeric values
				// set to 0 for non-numeric values
				int numPlayers = 0;
				try {
					numPlayers = Integer.parseInt(saveEvent.getPlayers());
				} catch (Exception e) {
					System.out.println(saveEvent.getPlayers() + " IS NOT A NUMBER");
				}
				ticket.setTicketSpaces(numPlayers + 1);

				ticket.setTicketName("Standard Ticket");
				ticketsService.save(ticket);

				bookingsService.doBooking(true, event, user, now, ticket, 1, Constants.STATUS_CODE_BOOKED);


				Postmeta postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("System");
				postmeta.setMetaValue(saveEvent.getSystem());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("GM");
				postmeta.setMetaValue(saveEvent.getGm());
				postMetaJPAService.save(postmeta);
				//update usermeta displayname with getGm value
				// add role gm to user

				// ROLES: if a teen at con time, add teen.  if not a teen at con time, and it's there, remove teen
				// add GM as a role for anyone submitting a game here

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("Characters");
				postmeta.setMetaValue(saveEvent.getCharacters());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("Players");
				postmeta.setMetaValue(saveEvent.getPlayers());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("Length");
				postmeta.setMetaValue(saveEvent.getLength());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("player_age");
				postmeta.setMetaValue(saveEvent.getPlayerAge());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("gm_age");
				postmeta.setMetaValue(saveEvent.getGmAge());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("Playtest");
				postmeta.setMetaValue(saveEvent.getPlaytest());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("Scheduling_Pref");
				postmeta.setMetaValue(saveEvent.getSchedulingPref());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("Min_Players");
				postmeta.setMetaValue(saveEvent.getMinPlayers());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("safety_tools");
				postmeta.setMetaValue(saveEvent.getSafetyTools());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("format");
				postmeta.setMetaValue(saveEvent.getFormat());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("run_number_of_times");
				postmeta.setMetaValue(String.valueOf(saveEvent.getRunNumberOfTimes()));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("room");
				postmeta.setMetaValue("");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("table");
				postmeta.setMetaValue("");
				postMetaJPAService.save(postmeta);

				// WP/EM bookeeping from here on
				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_eventId");
				postmeta.setMetaValue(event.getEventId().toString());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_start_time");
				postmeta.setMetaValue(timeFormat.format(startDateTime));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_end_time");
				postmeta.setMetaValue(timeFormat.format(endDateTime));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_all_day");
				postmeta.setMetaValue("0");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_start_date");
				postmeta.setMetaValue(dateFormat.format(startDateTime));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_end_date");
				postmeta.setMetaValue(dateFormat.format(endDateTime));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_rsvp");
				postmeta.setMetaValue("1");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_rsvp_date");
				postmeta.setMetaValue(defaultEventRsvpDate);
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_rsvp_time");
				postmeta.setMetaValue(defaultEventRsvpTime);
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_rsvp_spaces");
				postmeta.setMetaValue("0");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_spaces");
				postmeta.setMetaValue("0");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_locationId");
				postmeta.setMetaValue("0");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_recurrenceId");
				postmeta.setMetaValue("");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_status");
				postmeta.setMetaValue("1");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_private");
				postmeta.setMetaValue("0");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_blogId");
				postmeta.setMetaValue(null);
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_groupId");
				postmeta.setMetaValue("0");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_recurrence");
				postmeta.setMetaValue("0");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_recurrence_interval");
				postmeta.setMetaValue("");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_recurrence_freq");
				postmeta.setMetaValue("");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_recurrence_days");
				postmeta.setMetaValue("0");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_recurrence_byday");
				postmeta.setMetaValue("");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_recurrence_byweekno");
				postmeta.setMetaValue("");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_recurrence_rsvp_days");
				postmeta.setMetaValue("");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_start_ts");
				postmeta.setMetaValue(String.valueOf(startDateTime.getTime()));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_end_ts");
				postmeta.setMetaValue(String.valueOf(endDateTime.getTime()));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_wp_old_slug");
				postmeta.setMetaValue(eventSlug);
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_start");
				postmeta.setMetaValue(dateTimeFormat.format(startDateTime));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_end");
				postmeta.setMetaValue(dateTimeFormat.format(endDateTime));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_timezone");
				postmeta.setMetaValue("America\\/Los_Angeles");
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_start_local");
				postmeta.setMetaValue(dateTimeFormat.format(startDateTime));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("_event_end_local");
				postmeta.setMetaValue(dateTimeFormat.format(endDateTime));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("user_display_name");
				postmeta.setMetaValue(saveEvent.getUserDisplayName());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("other_info_private");
				postmeta.setMetaValue(saveEvent.getOtherInfo());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("request_private_room");
				postmeta.setMetaValue(saveEvent.getPrivateRoom());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("request_media_room");
				postmeta.setMetaValue(Boolean.toString(saveEvent.isRequestMediaRoom()));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("additional_gms");
				postmeta.setMetaValue(saveEvent.getAdditionalGms());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("request_media_equipment");
				postmeta.setMetaValue(saveEvent.getRequestMediaEquipment());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("additional_requirements");
				postmeta.setMetaValue(saveEvent.getAdditionalRequirements());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("table_type");
				postmeta.setMetaValue(saveEvent.getTableType());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("content_advisory");
				postmeta.setMetaValue(Boolean.toString(saveEvent.isContentAdvisory()));
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("trigger_warnings");
				postmeta.setMetaValue(saveEvent.getTriggerWarnings());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("accessability_options");
				postmeta.setMetaValue(saveEvent.getAccessabilityOptions());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("event_facilitators");
				postmeta.setMetaValue(saveEvent.getEventFacilitators());
				postMetaJPAService.save(postmeta);

				postmeta = new Postmeta();
				postmeta.setPostId(post.getId());
				postmeta.setMetaKey("event_tags");
				postmeta.setMetaValue(saveEvent.getEventTags());
				postMetaJPAService.save(postmeta);

			if (saveEvent.getEventMetadataIds() != null) {
				for (Long metaId : saveEvent.getEventMetadataIds()) {
					TermRelationships rel = new TermRelationships();
					rel.setObjectId(post.getId());
					rel.setTermTaxonomyId(metaId);
					rel.setTermOrder(0);
					termRelationshipsJPARepository.save(rel);
				}
			}

		}

		etx.commit();
		return eventId;


		} catch(Exception e) {
			e.printStackTrace();
			etx.rollback();
			throw e;
		}

	}

	public List<EventMetadataDTO> getEventMetadata() throws Exception {
		List<EventMetadataDTO> ret = new ArrayList<>();

		List<Object[]> rawData = eventsJPARepositoryService.findEventMetadata();
		for (Object[] row : rawData) {
			EventMetadataDTO data = new EventMetadataDTO();
			data.setName((String)row[0]);
			data.setSlug((String)row[1]);
			data.setId((Long)row[2]);
			data.setType((String)row[3]);
			ret.add(data);
		}

		return ret;
	}

}
