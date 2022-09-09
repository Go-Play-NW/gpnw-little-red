package org.littlered.dataservices.entity.wordpress;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "bbc_booking_count_view")
public class BbcBookingCountView implements Serializable {
	private Long bookingId;
	private Long eventId;
	private Long personId;
	private Integer bookingSpaces;
	private String bookingComment;
	private Timestamp bookingDate;
	private Integer bookingStatus;
	private BigDecimal bookingPrice;
	private String bookingMeta;
	private BigDecimal bookingTaxRate;
	private BigDecimal bookingTaxes;
	private String eventName;
	private Time eventStartTime;
	private Time eventEndTime;
	private Date eventStartDate;
	private Date eventEndDate;

	@Basic
	@Column(name = "booking_id")
	public Long getBookingId() {
		return bookingId;
	}

	public void setBookingId(Long bookingId) {
		this.bookingId = bookingId;
	}

	@Id
	@Column(name = "event_id")
	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	@Id
	@Column(name = "person_id")
	public Long getPersonId() {
		return personId;
	}

	public void setPersonId(Long personId) {
		this.personId = personId;
	}

	@Basic
	@Column(name = "booking_spaces")
	public Integer getBookingSpaces() {
		return bookingSpaces;
	}

	public void setBookingSpaces(Integer bookingSpaces) {
		this.bookingSpaces = bookingSpaces;
	}

	@Basic
	@Column(name = "booking_comment", columnDefinition = "TEXT")
	public String getBookingComment() {
		return bookingComment;
	}

	public void setBookingComment(String bookingComment) {
		this.bookingComment = bookingComment;
	}

	@Basic
	@Column(name = "booking_date")
	public Timestamp getBookingDate() {
		return bookingDate;
	}

	public void setBookingDate(Timestamp bookingDate) {
		this.bookingDate = bookingDate;
	}

	@Basic
	@Column(name = "booking_status")
	public Integer getBookingStatus() {
		return bookingStatus;
	}

	public void setBookingStatus(Integer bookingStatus) {
		this.bookingStatus = bookingStatus;
	}

	@Basic
	@Column(name = "booking_price")
	public BigDecimal getBookingPrice() {
		return bookingPrice;
	}

	public void setBookingPrice(BigDecimal bookingPrice) {
		this.bookingPrice = bookingPrice;
	}

	@Basic
	@Lob
	@Column(name = "booking_meta")
	public String getBookingMeta() {
		return bookingMeta;
	}

	public void setBookingMeta(String bookingMeta) {
		this.bookingMeta = bookingMeta;
	}

	@Basic
	@Column(name = "booking_tax_rate")
	public BigDecimal getBookingTaxRate() {
		return bookingTaxRate;
	}

	public void setBookingTaxRate(BigDecimal bookingTaxRate) {
		this.bookingTaxRate = bookingTaxRate;
	}

	@Basic
	@Column(name = "booking_taxes")
	public BigDecimal getBookingTaxes() {
		return bookingTaxes;
	}

	public void setBookingTaxes(BigDecimal bookingTaxes) {
		this.bookingTaxes = bookingTaxes;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BbcBookingCountView that = (BbcBookingCountView) o;
		return Objects.equals(bookingId, that.bookingId) &&
				Objects.equals(eventId, that.eventId) &&
				Objects.equals(personId, that.personId) &&
				Objects.equals(bookingSpaces, that.bookingSpaces) &&
				Objects.equals(bookingComment, that.bookingComment) &&
				Objects.equals(bookingDate, that.bookingDate) &&
				Objects.equals(bookingStatus, that.bookingStatus) &&
				Objects.equals(bookingPrice, that.bookingPrice) &&
				Objects.equals(bookingMeta, that.bookingMeta) &&
				Objects.equals(bookingTaxRate, that.bookingTaxRate) &&
				Objects.equals(bookingTaxes, that.bookingTaxes) &&
				Objects.equals(eventName, that.eventName) &&
				Objects.equals(eventStartTime, that.eventStartTime) &&
				Objects.equals(eventEndTime, that.eventEndTime);
	}

	@Override
	public int hashCode() {

		return Objects.hash(bookingId, eventId, personId, bookingSpaces, bookingComment, bookingDate, bookingStatus, bookingPrice, bookingMeta, bookingTaxRate, bookingTaxes, eventName, eventStartTime, eventEndTime);
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
}
