package org.littlered.dataservices.rest.params.social;

import java.util.ArrayList;
import java.util.List;

public class GetRolesForDiscordUserResponse {
	private String discordUser;
	private ArrayList<String> roles;

	public String getDiscordUser() {
		return discordUser;
	}

	public void setDiscordUser(String discordUser) {
		this.discordUser = discordUser;
	}

	public ArrayList<String> getRoles() {
		return roles;
	}

	public void setRoles(ArrayList<String> roles) {
		this.roles = roles;
	}
}
