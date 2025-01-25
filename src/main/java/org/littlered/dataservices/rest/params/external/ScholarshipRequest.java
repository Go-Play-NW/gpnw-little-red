package org.littlered.dataservices.rest.params.external;

import java.math.BigInteger;

public class ScholarshipRequest {
	private String dateAdded;
	private String displayName;
	private BigInteger userId;
	private String userEmail;
	private String assistanceTypes;
	private String identities;
	private String identitiesOther;
	private String gamingXp;
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

	public String getAssistanceTypes() {
		return assistanceTypes;
	}

	public void setAssistanceTypes(String assistanceTypes) {
		this.assistanceTypes = assistanceTypes;
	}

	public String getIdentities() {
		return identities;
	}

	public void setIdentities(String identities) {
		this.identities = identities;
	}

	public String getIdentitiesOther() {
		return identitiesOther;
	}

	public void setIdentitiesOther(String identitiesOther) {
		this.identitiesOther = identitiesOther;
	}

	public String getGamingXp() {
		return gamingXp;
	}

	public void setGamingXp(String gamingXp) {
		this.gamingXp = gamingXp;
	}

	public String getCommunityStandards() {
		return communityStandards;
	}

	public void setCommunityStandards(String communityStandards) {
		this.communityStandards = communityStandards;
	}
}
