package org.littlered.dataservices.repository.eventManager.interfaces;

import org.littlered.dataservices.entity.eventManager.EmBookings;
import org.littlered.dataservices.entity.wordpress.BbcBookingCountView;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Created by Jeremy on 3/25/2017.
 */
@Repository
public interface BookingsRepositoryInterface extends PagingAndSortingRepository<EmBookings, Long> {

	@Query(value = "SELECT b from org.littlered.dataservices.entity.eventManager.EmBookings b where b.user.id = ?1 AND b.eventId.eventName not like 'Verify % badge%' " +
			"AND FUNCTION('YEAR', b.eventId.eventStartDate) = ?2 and b.eventId.event_status = 1 " +
			"AND b.bookingStatus = org.littlered.dataservices.Constants.STATUS_CODE_BOOKED")
	List<EmBookings> findForUser(Long userId, Integer filterYear);

	@Query(value = "SELECT b from org.littlered.dataservices.entity.eventManager.EmBookings b where b.user.id = ?1 AND b.eventId.eventName not like 'Verify % badge%' " +
			"AND FUNCTION('YEAR', b.eventId.eventStartDate) = ?2 and b.eventId.event_status = 1 " +
			"AND b.bookingStatus = org.littlered.dataservices.Constants.STATUS_CODE_BOOKED")
	List<EmBookings> findPublicForUser(Long userId, Integer filterYear);

	@Query(value = "SELECT b from org.littlered.dataservices.entity.eventManager.EmBookings b where b.eventId.eventId= ?1 and b.bookingSpaces = 1 " +
			"AND FUNCTION('YEAR', b.eventId.eventStartDate) = ?2 " +
			"AND b.bookingStatus  = org.littlered.dataservices.Constants.STATUS_CODE_BOOKED")
	List<EmBookings> findForEvent(Long eventId, Integer filterYear);

	@Query(value = "SELECT b from org.littlered.dataservices.entity.eventManager.EmBookings b where b.eventId.eventId= ?1 and b.bookingSpaces = 1 " +
			"AND FUNCTION('YEAR', b.eventId.eventStartDate) = ?2 " +
			"AND b.bookingStatus = org.littlered.dataservices.Constants.STATUS_CODE_OVERBOOKED")
	List<EmBookings> findOverbookingForEvent(Long eventId, Integer filterYear);

	@Query(value = "SELECT b from org.littlered.dataservices.entity.eventManager.EmBookings b where b.user.id = ?1 AND b.eventId.eventId = ?2 " +
			"AND FUNCTION('YEAR', b.eventId.eventStartDate) = ?3 " +
			"and b.bookingStatus = org.littlered.dataservices.Constants.STATUS_CODE_OVERBOOKED")
	List<EmBookings> findOverbookingForUserAndEvent(Long userId, Long eventId, Integer filterYear);

	@Query(value = "SELECT bcv from BbcBookingCountView bcv where FUNCTION('YEAR', bcv.eventStartDate) = ?2 " +
			"AND bcv.personId = ?1 and bcv.bookingSpaces = 1")
	List<BbcBookingCountView> findBookingsForQuotaCount(Long userId, Integer filterYear);

	@Query(value = "SELECT b from org.littlered.dataservices.entity.eventManager.EmBookings b where b.eventId.eventId = ?1 AND b.user.id = ?2 and b.bookingSpaces = 1")
	List<EmBookings> findBookingsByEventIdandUserId(Long eventId, Long userId);

	@Query(value =
			"SELECT b FROM org.littlered.dataservices.entity.eventManager.EmBookings b " +
					"WHERE FUNCTION('TIMESTAMP', b.eventId.eventStartDate, b.eventId.eventStartTime) <= ?2 " +
					"  AND FUNCTION('TIMESTAMP', b.eventId.eventEndDate, b.eventId.eventEndTime) > ?2 " +
					"  AND b.user.id = ?1 and b.bookingStatus = 1")
	List<EmBookings> findConflictingEventsForUserAndStartDateTime(Long userId, Date startDate);

	@Query(value =
			"SELECT b FROM org.littlered.dataservices.entity.eventManager.EmBookings b " +
					"WHERE FUNCTION('TIMESTAMP', b.eventId.eventStartDate, b.eventId.eventStartTime) < ?2 " +
					"  AND FUNCTION('TIMESTAMP', b.eventId.eventEndDate, b.eventId.eventEndTime) >= ?2 " +
					"  AND b.user.id = ?1 and b.bookingStatus = 1")
	List<EmBookings> findConflictingEventsForUserAndEndDateTime(Long userId, Date endDate);

	@Query(value =
			"SELECT b FROM org.littlered.dataservices.entity.eventManager.EmBookings b " +
					"WHERE FUNCTION('TIMESTAMP', b.eventId.eventStartDate, b.eventId.eventStartTime) >= ?2 " +
					"  AND FUNCTION('TIMESTAMP', b.eventId.eventEndDate, b.eventId.eventEndTime) <= ?3 " +
					"  AND b.user.id = ?1 and b.bookingStatus = 1")
	List<EmBookings> findNestedConflictingEventsUserAndStartAndEnd(Long userId, Date startDate, Date endDate);

}
