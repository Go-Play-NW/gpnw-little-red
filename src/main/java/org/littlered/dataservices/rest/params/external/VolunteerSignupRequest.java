package org.littlered.dataservices.rest.params.external;

import java.math.BigInteger;
import java.util.Date;

public class VolunteerSignupRequest {
	private String dateAdded;
	private String displayName;
	private BigInteger userId;
	private String userEmail;
	private String phone;
	private String discord;
	private String otherInfo;
	private String communityStandards;

	public String getDateAdded() {
		return dateAdded;
	}

	public void setDateAdded(String dateAdded) {
		this.dateAdded = dateAdded;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public BigInteger getUserId() {
		return userId;
	}

	public void setUserId(BigInteger userId) {
		this.userId = userId;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getDiscord() {
		return discord;
	}

	public void setDiscord(String discord) {
		this.discord = discord;
	}

	public String getOtherInfo() {
		return otherInfo;
	}

	public void setOtherInfo(String otherInfo) {
		this.otherInfo = otherInfo;
	}

	public String getCommunityStandards() {
		return communityStandards;
	}

	public void setCommunityStandards(String communityStandards) {
		this.communityStandards = communityStandards;
	}
}
