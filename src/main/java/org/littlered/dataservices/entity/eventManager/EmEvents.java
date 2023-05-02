package org.littlered.dataservices.entity.eventManager;

import org.littlered.dataservices.entity.wordpress.shrt.BbcEventCategories;
import org.littlered.dataservices.entity.wordpress.Postmeta;
import org.littlered.dataservices.entity.wordpress.shrt.BbcUsersShort;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Set;

/**
 * Created by Jeremy on 3/19/2017.
 */
@Entity
public class EmEvents implements Serializable {
	private Long eventId;
	private String eventSlug;
	private BbcUsersShort eventOwner;
	private Integer eventStatus;
	private String eventName;
	private Time eventStartTime;
	private Time eventEndTime;
	private Date eventStartDate;
	private Date eventEndDate;
	private String postContent;
	private Byte eventRsvp;
	private Integer eventSpaces;
	private Long locationId;
	private Long eventCategoryId;
	private String eventAttributes;
	private Timestamp eventDateCreated;
	private Timestamp eventDateModified;
	private Long blogId;
	private Long groupId;
	private Long postId;
	private Integer eventAllDay;
	private Byte eventPrivate;
	private Date eventRsvpDate;
	private Time eventRsvpTime;
	private String eventRoom;
	private String eventTable;
	private Integer eventRsvpSpaces;
	private Integer bookingExempt;
	private Timestamp lastUpdated;
	private Timestamp eventStart;
	private Timestamp eventEnd;
	private String eventTimezone;

	private Set<EmBookings> bookings;
	private Set<BbcEventCategories> categories;
	private Set<Postmeta> metadata;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "event_id")
	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	@Basic
	@Column(name = "event_slug")
	public String getEventSlug() {
		return eventSlug;
	}

	public void setEventSlug(String eventSlug) {
		this.eventSlug = eventSlug;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "event_owner", referencedColumnName = "ID")
	public BbcUsersShort getEventOwner() {
		return eventOwner;
	}

	public void setEventOwner(BbcUsersShort eventOwner) {
		this.eventOwner = eventOwner;
	}

	@Basic
//	@Column(name = "event_status")
	@Column(name = "event_status", columnDefinition = "TINYINT", length = 1)
	public Integer getEventStatus() {
		return eventStatus;
	}

	public void setEventStatus(Integer eventStatus) {
		this.eventStatus = eventStatus;
	}

	@Basic
	@Column(name = "event_name", columnDefinition = "TEXT")
	public String getEventName() {
		return eventName;
	}

	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	@Basic
	@Column(name = "event_start_time")
	public Time getEventStartTime() {
		return eventStartTime;
	}

	public void setEventStartTime(Time eventStartTime) {
		this.eventStartTime = eventStartTime;
	}

	@Basic
	@Column(name = "event_end_time")
	public Time getEventEndTime() {
		return eventEndTime;
	}

	public void setEventEndTime(Time eventEndTime) {
		this.eventEndTime = eventEndTime;
	}

	@Basic
	@Column(name = "event_start_date")
	public Date getEventStartDate() {
		return eventStartDate;
	}

	public void setEventStartDate(Date eventStartDate) {
		this.eventStartDate = eventStartDate;
	}

	@Basic
	@Column(name = "event_end_date")
	public Date getEventEndDate() {
		return eventEndDate;
	}

	public void setEventEndDate(Date eventEndDate) {
		this.eventEndDate = eventEndDate;
	}

	@Basic
	@Lob
	@Column(name = "post_content")
	public String getPostContent() {
		return postContent;
	}

	public void setPostContent(String postContent) {
		this.postContent = postContent;
	}

	@Basic
	@Column(name = "event_rsvp")
	public Byte getEventRsvp() {
		return eventRsvp;
	}

	public void setEventRsvp(Byte eventRsvp) {
		this.eventRsvp = eventRsvp;
	}

	@Basic
	@Column(name = "event_spaces")
	public Integer getEventSpaces() {
		return eventSpaces;
	}

	public void setEventSpaces(Integer eventSpaces) {
		this.eventSpaces = eventSpaces;
	}

	@Basic
	@Column(name = "location_id")
	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	@Basic
	@Column(name = "event_category_id")
	public Long getEventCategoryId() {
		return eventCategoryId;
	}

	public void setEventCategoryId(Long eventCategoryId) {
		this.eventCategoryId = eventCategoryId;
	}

	@Basic
	@Column(name = "event_attributes", columnDefinition = "TEXT")
	public String getEventAttributes() {
		return eventAttributes;
	}

	public void setEventAttributes(String eventAttributes) {
		this.eventAttributes = eventAttributes;
	}

	@Basic
	@Column(name = "event_date_created")
	public Timestamp getEventDateCreated() {
		return eventDateCreated;
	}

	public void setEventDateCreated(Timestamp eventDateCreated) {
		this.eventDateCreated = eventDateCreated;
	}

	@Basic
	@Column(name = "event_date_modified")
	public Timestamp getEventDateModified() {
		return eventDateModified;
	}

	public void setEventDateModified(Timestamp eventDateModified) {
		this.eventDateModified = eventDateModified;
	}

	@Basic
	@Column(name = "blog_id")
	public Long getBlogId() {
		return blogId;
	}

	public void setBlogId(Long blogId) {
		this.blogId = blogId;
	}

	@Basic
	@Column(name = "group_id")
	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	@Basic
	@Column(name = "post_id")
	public Long getPostId() {
		return postId;
	}

	public void setPostId(Long postId) {
		this.postId = postId;
	}

	@Basic
	//@Column(name = "event_all_day")
	@Column(name = "event_all_day", columnDefinition = "TINYINT", length = 1)
	public Integer getEventAllDay() {
		return eventAllDay;
	}

	public void setEventAllDay(Integer eventAllDay) {
		this.eventAllDay = eventAllDay;
	}

	@Basic
	@Column(name = "event_private")
	public Byte getEventPrivate() {
		return eventPrivate;
	}

	public void setEventPrivate(Byte eventPrivate) {
		this.eventPrivate = eventPrivate;
	}

	@Basic
	@Column(name = "event_rsvp_date")
	public Date getEventRsvpDate() {
		return eventRsvpDate;
	}

	public void setEventRsvpDate(Date eventRsvpDate) {
		this.eventRsvpDate = eventRsvpDate;
	}

	@Basic
	@Column(name = "event_rsvp_time")
	public Time getEventRsvpTime() {
		return eventRsvpTime;
	}

	public void setEventRsvpTime(Time eventRsvpTime) {
		this.eventRsvpTime = eventRsvpTime;
	}

	@Basic
	@Column(name = "event_room", columnDefinition = "TEXT")
	public String getEventRoom() {
		return eventRoom;
	}

	public void setEventRoom(String eventRoom) {
		this.eventRoom = eventRoom;
	}

	@Basic
	@Column(name = "event_table", columnDefinition = "TEXT")
	public String getEventTable() {
		return eventTable;
	}

	public void setEventTable(String eventTable) {
		this.eventTable = eventTable;
	}

	@Basic
	@Column(name = "event_rsvp_spaces")
	public Integer getEventRsvpSpaces() {
		return eventRsvpSpaces;
	}

	public void setEventRsvpSpaces(Integer eventRsvpSpaces) {
		this.eventRsvpSpaces = eventRsvpSpaces;
	}

	@Basic
	@Column(name = "booking_exempt")
	public Integer getBookingExempt() {
		return bookingExempt;
	}

	public void setBookingExempt(Integer bookingExempt) {
		this.bookingExempt = bookingExempt;
	}


	@OneToMany(mappedBy = "eventId", cascade = CascadeType.ALL)
	public Set<EmBookings> getBookings() {
		return bookings;
	}

	public void setBookings(Set<EmBookings> bookings) {
		this.bookings = bookings;
	}

	@OneToMany( fetch = FetchType.LAZY)
	@JoinColumn(name = "event_id", referencedColumnName = "event_id")
	public Set<BbcEventCategories> getCategories() {
		return categories;
	}

	public void setCategories(Set<BbcEventCategories> categories) {
		this.categories = categories;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EmEvents that = (EmEvents) o;

		if (eventId != null ? !eventId.equals(that.eventId) : that.eventId != null) return false;
		if (eventSlug != null ? !eventSlug.equals(that.eventSlug) : that.eventSlug != null) return false;
		if (eventOwner != null ? !eventOwner.equals(that.eventOwner) : that.eventOwner != null) return false;
		if (eventStatus != null ? !eventStatus.equals(that.eventStatus) : that.eventStatus != null) return false;
		if (eventName != null ? !eventName.equals(that.eventName) : that.eventName != null) return false;
		if (eventStartTime != null ? !eventStartTime.equals(that.eventStartTime) : that.eventStartTime != null)
			return false;
		if (eventEndTime != null ? !eventEndTime.equals(that.eventEndTime) : that.eventEndTime != null) return false;
		if (eventStartDate != null ? !eventStartDate.equals(that.eventStartDate) : that.eventStartDate != null)
			return false;
		if (eventEndDate != null ? !eventEndDate.equals(that.eventEndDate) : that.eventEndDate != null) return false;
		if (postContent != null ? !postContent.equals(that.postContent) : that.postContent != null) return false;
		if (eventRsvp != null ? !eventRsvp.equals(that.eventRsvp) : that.eventRsvp != null) return false;
		if (eventSpaces != null ? !eventSpaces.equals(that.eventSpaces) : that.eventSpaces != null) return false;
		if (locationId != null ? !locationId.equals(that.locationId) : that.locationId != null) return false;
		if (eventCategoryId != null ? !eventCategoryId.equals(that.eventCategoryId) : that.eventCategoryId != null)
			return false;
		if (eventAttributes != null ? !eventAttributes.equals(that.eventAttributes) : that.eventAttributes != null)
			return false;
		if (eventDateCreated != null ? !eventDateCreated.equals(that.eventDateCreated) : that.eventDateCreated != null)
			return false;
		if (eventDateModified != null ? !eventDateModified.equals(that.eventDateModified) : that.eventDateModified != null)
			return false;
		if (blogId != null ? !blogId.equals(that.blogId) : that.blogId != null) return false;
		if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) return false;
		if (postId != null ? !postId.equals(that.postId) : that.postId != null) return false;
		if (eventAllDay != null ? !eventAllDay.equals(that.eventAllDay) : that.eventAllDay != null) return false;
		if (eventPrivate != null ? !eventPrivate.equals(that.eventPrivate) : that.eventPrivate != null) return false;
		if (eventRsvpDate != null ? !eventRsvpDate.equals(that.eventRsvpDate) : that.eventRsvpDate != null)
			return false;
		if (eventRsvpTime != null ? !eventRsvpTime.equals(that.eventRsvpTime) : that.eventRsvpTime != null)
			return false;
		if (eventRoom != null ? !eventRoom.equals(that.eventRoom) : that.eventRoom != null) return false;
		if (eventTable != null ? !eventTable.equals(that.eventTable) : that.eventTable != null) return false;
		if (eventRsvpSpaces != null ? !eventRsvpSpaces.equals(that.eventRsvpSpaces) : that.eventRsvpSpaces != null)
			return false;
		if (bookingExempt != null ? !bookingExempt.equals(that.bookingExempt) : that.bookingExempt != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = eventId != null ? eventId.hashCode() : 0;
		result = 31 * result + (eventSlug != null ? eventSlug.hashCode() : 0);
		result = 31 * result + (eventOwner != null ? eventOwner.hashCode() : 0);
		result = 31 * result + (eventStatus != null ? eventStatus.hashCode() : 0);
		result = 31 * result + (eventName != null ? eventName.hashCode() : 0);
		result = 31 * result + (eventStartTime != null ? eventStartTime.hashCode() : 0);
		result = 31 * result + (eventEndTime != null ? eventEndTime.hashCode() : 0);
		result = 31 * result + (eventStartDate != null ? eventStartDate.hashCode() : 0);
		result = 31 * result + (eventEndDate != null ? eventEndDate.hashCode() : 0);
		result = 31 * result + (postContent != null ? postContent.hashCode() : 0);
		result = 31 * result + (eventRsvp != null ? eventRsvp.hashCode() : 0);
		result = 31 * result + (eventSpaces != null ? eventSpaces.hashCode() : 0);
		result = 31 * result + (locationId != null ? locationId.hashCode() : 0);
		result = 31 * result + (eventCategoryId != null ? eventCategoryId.hashCode() : 0);
		result = 31 * result + (eventAttributes != null ? eventAttributes.hashCode() : 0);
		result = 31 * result + (eventDateCreated != null ? eventDateCreated.hashCode() : 0);
		result = 31 * result + (eventDateModified != null ? eventDateModified.hashCode() : 0);
		result = 31 * result + (blogId != null ? blogId.hashCode() : 0);
		result = 31 * result + (groupId != null ? groupId.hashCode() : 0);
		result = 31 * result + (postId != null ? postId.hashCode() : 0);
		result = 31 * result + (eventAllDay != null ? eventAllDay.hashCode() : 0);
		result = 31 * result + (eventPrivate != null ? eventPrivate.hashCode() : 0);
		result = 31 * result + (eventRsvpDate != null ? eventRsvpDate.hashCode() : 0);
		result = 31 * result + (eventRsvpTime != null ? eventRsvpTime.hashCode() : 0);
		result = 31 * result + (eventRoom != null ? eventRoom.hashCode() : 0);
		result = 31 * result + (eventTable != null ? eventTable.hashCode() : 0);
		result = 31 * result + (eventRsvpSpaces != null ? eventRsvpSpaces.hashCode() : 0);
		result = 31 * result + (bookingExempt != null ? bookingExempt.hashCode() : 0);
		return result;
	}

	@Basic
	@Column(name = "last_updated")
	public Timestamp getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Timestamp lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", referencedColumnName = "post_id")
	@Where(clause = "meta_key in (" +
			"'System', " +
			"'GM'," +
			"'Maturity', " +
			"'Characters', " +
			"'Min_Players', " +
			"'Players', " +
			"'Length', " +
			"'player_age', " +
			"'gm_age', " +
			"'Playtest', " +
			"'short_desc', " +
			"'room', " +
			"'table', " +
			"'exempt', " +
			"'event_image', " +
			"'trigger_warnings', " +
			"'content-warnings', " +
			"'event_tags', " +
			"'game-tags', " +
			"'format', " +
			"'safety_tools') " +
			" or meta_key like '%\\_shift'")
	@OrderBy(value = "metaId")
	public Set<Postmeta> getMetadata() {
		return metadata;
	}

	public void setMetadata(Set<Postmeta> categories) {
		this.metadata = categories;
	}

	public Timestamp getEventStart() {
		return eventStart;
	}

	public void setEventStart(Timestamp eventStart) {
		this.eventStart = eventStart;
	}

	public Timestamp getEventEnd() {
		return eventEnd;
	}

	public void setEventEnd(Timestamp eventEnd) {
		this.eventEnd = eventEnd;
	}

	public String getEventTimezone() {
		return eventTimezone;
	}

	public void setEventTimezone(String eventTimezone) {
		this.eventTimezone = eventTimezone;
	}
}
