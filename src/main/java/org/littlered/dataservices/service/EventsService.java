package org.littlered.dataservices.service;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.eventManager.EventScheduleDataDTO;
import org.littlered.dataservices.entity.eventManager.EmBookings;
import org.littlered.dataservices.entity.eventManager.pub.EmEvents;
import org.littlered.dataservices.entity.wordpress.BbcUserFavorites;
import org.littlered.dataservices.entity.wordpress.Postmeta;
import org.littlered.dataservices.exception.FavoritingException;
import org.littlered.dataservices.repository.eventManager.interfaces.BookingsRepositoryInterface;
import org.littlered.dataservices.repository.eventManager.interfaces.EventsPublicRepositoryInterface;
import org.littlered.dataservices.repository.eventManager.interfaces.EventsRepositoryInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.BbcUserFavoritesInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Jeremy on 4/2/2017.
 */
@Service
//@Transactional
public class EventsService {

	@Autowired
	private EventsRepositoryInterface eventsRepository;

	@Autowired
	private EventsPublicRepositoryInterface eventsPublicRepository;

	@Autowired
	private BookingsRepositoryInterface bookingsRepository;

	@Autowired
	private UsersService usersService;

	@Autowired
	private SecurityService securityService;

	@Autowired
	private BbcUserFavoritesInterface bbcUserFavoritesInterface;

	@Value("${display.year.filter}")
	private String yearFilter;

	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findAll() throws Exception {
		String year = getFilterYear();
		return eventsRepository.findByYear(Integer.parseInt(year));
	}

	public Iterable<EmEvents> findAllPublic() throws Exception {
		String year = getFilterYear();
		return eventsPublicRepository.findPublicByYear(Integer.parseInt(year));
	}

	public Iterable<EmEvents> findAllPublicAfterEpochDate(Long epochTime) throws Exception {
		String year = getFilterYear();
		return eventsPublicRepository.findUpdatedPublicAfterEpochDate(epochTime, Integer.parseInt(year));
	}

	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findAllForYear(Long year) throws Exception {
		return eventsRepository.findByYear(year.intValue());
	}

	public Long getCount() throws Exception {
		return eventsRepository.getNumberOfEventsByYear(Integer.parseInt(getFilterYear()));
	}

	public Long getCountForYear(Long year) throws Exception {
		return eventsRepository.getNumberOfEventsByYear(year.intValue());
	}

	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findAllPaginated(Long length, Long offset) throws Exception {
		String year = getFilterYear();
		return eventsRepository.findByYearPaginated(Integer.parseInt(year), new PageRequest(length.intValue(), offset.intValue()));
	}

	public Iterable<EmEvents> findAllPublicPaginated(Long length, Long offset) throws Exception {
		String year = getFilterYear();
		return eventsPublicRepository.findPublicByYearPaginated(Integer.parseInt(year), new PageRequest(length.intValue(), offset.intValue()));
	}

	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findAllForYearPaginated(Long year, Long length, Long offset) throws Exception {
		return eventsRepository.findByYearPaginated(year.intValue(), new PageRequest(length.intValue(), offset.intValue()));
	}

	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findAllForCategory(String category) throws Exception {
		String year = getFilterYear();
		return eventsRepository.findByCategoryAndYear(Integer.parseInt(year), category);
	}

	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findAllForYearAndCategory(Long year, String category) throws Exception {
		String yearStr = year.toString();
		return eventsRepository.findByCategoryAndYear(Integer.parseInt(yearStr), category);
	}

	public org.littlered.dataservices.entity.eventManager.EmEvents findOne(Long id) throws Exception {
		return eventsRepository.findOne(id);
	}

	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findForUser(Long userId, Long yearLong) throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		String year;
		if (yearLong == null) {
			year = getFilterYear();
		} else {
			year = yearLong.toString();
		}
		ArrayList<org.littlered.dataservices.entity.eventManager.EmEvents> events = new ArrayList<>();
		for (org.littlered.dataservices.entity.eventManager.EmBookings booking : bookingsRepository.findForUser(userId, Integer.parseInt(year))) {
			events.add(booking.getEventId());
		}
		return events;
	}

	public Iterable<Long> findForMe(Long yearLong) throws Exception {
		String year;
		if (yearLong == null) {
			year = getFilterYear();
		} else {
			year = yearLong.toString();
		}
		ArrayList<Long> events = new ArrayList<>();
		for (org.littlered.dataservices.entity.eventManager.EmBookings booking : bookingsRepository.findPublicForUser(usersService.getCurrentUser().getId(), Integer.parseInt(year))) {
			events.add(booking.getEventId().getEventId());
		}
		return events;
	}

	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findFavoritesForMe(Long yearLong) throws Exception {
		Long myUserId = usersService.getCurrentUser().getId();
		if (yearLong == null) {
			yearLong = Long.parseLong(getFilterYear());
		}

		return eventsRepository.findFavoritesForUser(myUserId, yearLong.intValue());
	}


	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findUpdatedAfterEpochTime(Long epochTime, Long yearLong) throws Exception {
		String year;
		if (yearLong == null) {
			year = getFilterYear();
		} else {
			year = yearLong.toString();
		}
		return eventsRepository.findUpdatedEventsAfterEpochDate(epochTime, Integer.parseInt(year));
	}

	private String getFilterYear() {
		String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
		if (yearFilter != null && !yearFilter.equals("")) {
			year = yearFilter;
		}
		return year;
	}

	public void createMyUserEventFavorite(Long eventId) throws Exception {
		Long myUserId = usersService.getCurrentUser().getId();
		createUserEventFavorite(myUserId, eventId);
	}

	public void createOtherUserEventFavorite(Long userId, Long eventId) throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		createUserEventFavorite(userId, eventId);
	}

	private void createUserEventFavorite(Long userId, Long eventId) throws Exception {
		if(eventId == null) {
			throw new FavoritingException("eventId cannot be null!");
		}

		if(userId == null) {
			throw new FavoritingException("userId cannot be null!");
		}

		org.littlered.dataservices.entity.eventManager.EmEvents event = eventsRepository.findOne(eventId);
		if (event == null) {
			throw new FavoritingException("Event with id " + eventId + " not found!");
		}

		BbcUserFavorites favorite = bbcUserFavoritesInterface.findByUserIdAndEventId(userId, eventId);
		if (favorite != null) {
			throw new FavoritingException("User " + userId + " already favorited event " + eventId + "!");
		}

		Timestamp now = new Timestamp(System.currentTimeMillis());

		favorite = new BbcUserFavorites();
		favorite.setUserId(userId);
		favorite.setEventId(event.getEventId());
		favorite.setCreateDate(now);
		favorite.setUpdateDate(now);
		favorite.setStatus((short) 1);

		bbcUserFavoritesInterface.save(favorite);
	}

	public void deleteMyUserEventFavorite(Long eventId) throws Exception {
		Long myUserId = usersService.getCurrentUser().getId();
		deleteUserEventFavorite(myUserId, eventId);
	}

	public void deleteOtherUserEventFavorite(Long userId, Long eventId) throws Exception{
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		deleteUserEventFavorite(userId, eventId);
	}

	private void deleteUserEventFavorite(Long userId, Long eventId) throws Exception{

		if(eventId == null) {
			throw new FavoritingException("eventId cannot be null!");
		}

		if(userId == null) {
			throw new FavoritingException("userId cannot be null!");
		}

		org.littlered.dataservices.entity.eventManager.EmEvents event = eventsRepository.findOne(eventId);
		if (event == null) {
			throw new FavoritingException("Event with id " + eventId + " not found!");
		}

		BbcUserFavorites favorite = bbcUserFavoritesInterface.findByUserIdAndEventId(userId, eventId);
		if (favorite == null) {
			throw new FavoritingException("User " + userId + " has not favorited event " + eventId + "!");
		}
		bbcUserFavoritesInterface.delete(favorite);

	}

	public Map<BigInteger, BigInteger> getBookingCounts() {
		List<Object[]> counts = eventsRepository.getBookingCounts(Integer.parseInt(getFilterYear()));
		Map<BigInteger, BigInteger> countMap = counts.stream().collect(Collectors.toMap(e -> ((BigInteger)e[0]), e -> ((BigInteger)e[1])));
		return countMap;
	}

	public BigInteger getBookingCountForEvent(Long eventId) {
		List<Object[]> counts = eventsRepository.getBookingCountForEvent(eventId);
		if (counts == null || counts.size() == 0) {
			return null;
		}
		return ((BigInteger)counts.get(0)[1]);
	}

	public List<EventScheduleDataDTO> getEventData(String focus) throws Exception {

//		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);

		List<EventScheduleDataDTO> badges = new ArrayList<>();

		SimpleDateFormat format = new SimpleDateFormat("E h:mm a", Locale.US);

		Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> events = findAll();
		for (org.littlered.dataservices.entity.eventManager.EmEvents event : events) {
			if (event.getEventStatus() != 1) {
				continue;
			}
			if (focus == null && event.getEventName().startsWith("Volunteer")) {
				continue;
			}
			if (focus != null && focus.equals("volunteer") && !event.getEventName().startsWith("Volunteer")) {
				continue;
			}
			EventScheduleDataDTO badge = new EventScheduleDataDTO();
			badge.setEventId(event.getEventId());
			badge.setFacilitatorName(event.getEventOwner().getDisplayName());
			badge.setGameTitle(event.getEventName());
			badge.setGameTime(format.format(event.getEventStart()));

			String gamePitch = event.getPostContent();
			gamePitch = gamePitch.replaceAll("\r", "");
			gamePitch = gamePitch.replaceAll("\n", "");
			badge.setGamePitch(gamePitch);

			String minplayers = null;
			String maxplayers = null;
			for(Postmeta meta: event.getMetadata()) {
				if (meta.getMetaKey().equals("event_image") && meta.getMetaValue() != null && !meta.getMetaValue().equals("")) {
					badge.setGameArt("images/".concat(meta.getMetaValue()));
				}
				if (meta.getMetaKey().equals("Min_Players")) {
					minplayers = meta.getMetaValue();
				}
				if (meta.getMetaKey().equals("Players")) {
					maxplayers = meta.getMetaValue();
				}
				if (meta.getMetaValue().equals("Playtest")) {
					if (meta.getMetaValue().equals(("1"))) {
						badge.setPlaytest("/layout_graphics/playtest.png");
					}
				}
				if (meta.getMetaKey().equals("event_tags") && meta.getMetaValue().contains("Playtest")) {
					badge.setPlaytest("/layout_graphics/playtest.png");
				}
				if (meta.getMetaKey().equals("System")) {
					badge.setSystem(meta.getMetaValue());
				}
				if (meta.getMetaKey().equals("event_tags") && meta.getMetaValue() != null && !meta.getMetaValue().equals("")) {
					badge.setGameTags("Tags: ".concat(meta.getMetaValue()));
				}
				if (meta.getMetaKey().equals("trigger_warnings") && meta.getMetaValue() != null && !meta.getMetaValue().equals("")) {
					badge.setContentWarnings("Content Warnings: ".concat(meta.getMetaValue()));
				}
				if (meta.getMetaKey().equals("safety_tools") && meta.getMetaValue() != null) {
					badge.setSafetyTools(meta.getMetaValue());
				}
			}
			if (minplayers != null && maxplayers != null) {
				badge.setNumberofPlayers(minplayers.concat("-").concat(maxplayers));
				String slotsOpen = minplayers.concat("-").concat(maxplayers);
				if (!minplayers.equals(maxplayers)) {
					badge.setGamePlayersList("/players_name-list/".concat(slotsOpen).concat(".png"));
				} else {
					badge.setGamePlayersList("/players_name-list/".concat(maxplayers).concat(".png"));
				}
			}

			int playercount = 1;
			for(EmBookings booking : event.getBookings()) {
				if (booking.getBookingStatus() != 1) {
					continue;
				}
				if (booking.getBookingComment() != null && booking.getBookingComment().equalsIgnoreCase("GM")) {
					continue;
				}
				if (playercount > 6) {
					break;
				}
				String bookingname = booking.getUser().getDisplayName().concat(" "); //.concat(booking.getBookingId().toString());
				switch(playercount) {
					case 1: badge.setPlayer1(bookingname);
						break;
					case 2: badge.setPlayer2(bookingname);
						break;
					case 3: badge.setPlayer3(bookingname);
						break;
					case 4: badge.setPlayer4(bookingname);
						break;
					case 5: badge.setPlayer5(bookingname);
						break;
					case 6: badge.setPlayer6(bookingname);
						break;
				}
				playercount++;
			}
			badges.add(badge);
		}

		return badges;
	}
}
