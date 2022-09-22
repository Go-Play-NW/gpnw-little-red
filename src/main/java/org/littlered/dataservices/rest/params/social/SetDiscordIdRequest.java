package org.littlered.dataservices.rest.params.social;

public class SetDiscordIdRequest {

	private Long userId;
	private String discordId;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getDiscordId() {
		return discordId;
	}

	public void setDiscordId(String discordId) {
		this.discordId = discordId;
	}
}
