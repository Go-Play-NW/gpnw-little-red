package org.littlered.dataservices.service;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.entity.eventManager.pub.EmEvents;
import org.littlered.dataservices.entity.wordpress.BbcUserFavorites;
import org.littlered.dataservices.exception.FavoritingException;
import org.littlered.dataservices.repository.eventManager.interfaces.BookingsRepositoryInterface;
import org.littlered.dataservices.repository.eventManager.interfaces.EventsPublicRepositoryInterface;
import org.littlered.dataservices.repository.eventManager.interfaces.EventsRepositoryInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.BbcUserFavoritesInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;

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

}
