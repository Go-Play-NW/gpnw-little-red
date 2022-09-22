package org.littlered.dataservices.rest.controller.social;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.littlered.dataservices.Constants;
import org.littlered.dataservices.rest.params.social.GetRolesForDiscordUserResponse;
import org.littlered.dataservices.rest.params.social.SetDiscordIdRequest;
import org.littlered.dataservices.service.SecurityService;
import org.littlered.dataservices.service.UsersJPAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

@RestController
@RequestMapping("/social/discord")
@Api(description = "Operations pertaining to the Discord social media platform.")
public class DiscordServicesController {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	@Autowired
	private UsersJPAService usersJPAService;

	@Autowired
	SecurityService securityService;

	@ApiOperation(value = "Set Discord ID for a user. Admin only.", response = String.class)
	@RequestMapping(value = "/setID", method = RequestMethod.POST)
	public String setDiscordUsername(@RequestBody SetDiscordIdRequest setDiscordIdRequest, HttpServletResponse response)
			throws Exception {

		logger.info("Setting Discord ID for user " + setDiscordIdRequest.getUserId());

		try {
			securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
			usersJPAService.addOrUpdateUserMeta(setDiscordIdRequest.getUserId(),
					Constants.DISCORD_ID_USERMETA_KEY,
					setDiscordIdRequest.getDiscordId());
		} catch (SecurityException se) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return Constants.STATUS_FAILURE;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		return Constants.STATUS_SUCCESS;
	}

	@ApiOperation(value = "Get user roles by Discord ID. Admin only.", response = GetRolesForDiscordUserResponse.class)
	@RequestMapping(value = "/user/getRoles", method = RequestMethod.GET, produces = "application/json")
	public GetRolesForDiscordUserResponse findUserRolesByDiscordId(@RequestParam("discordId") String discordId, HttpServletResponse response)
			throws Exception {

		logger.info("Getting roles for Discord ID " + discordId);

		try {
			securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
			ArrayList<String> roles = usersJPAService.findUserRolesByDiscordId(discordId);
			GetRolesForDiscordUserResponse reply = new GetRolesForDiscordUserResponse();
			reply.setDiscordUser(discordId);
			reply.setRoles(roles);
			return reply;
		} catch (SecurityException se) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	@ApiOperation(value = "Get user roles for all users by Discord ID. Admin only.", response = GetRolesForDiscordUserResponse.class)
	@RequestMapping(value = "/users/getRoles", method = RequestMethod.GET, produces = "application/json")
	public ArrayList<GetRolesForDiscordUserResponse> findUserRolesForDiscord(HttpServletResponse response)
			throws Exception {

		logger.info("Getting roles for all users with Discord IDs");

		try {
			securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
			HashMap<String, ArrayList<String>> roles = usersJPAService.findUserRolesForAllDiscordUsers();
			ArrayList<GetRolesForDiscordUserResponse> replies = new ArrayList<>();
			for (String discordId : roles.keySet()) {
				GetRolesForDiscordUserResponse reply = new GetRolesForDiscordUserResponse();
				reply.setDiscordUser(discordId);
				reply.setRoles(roles.get(discordId));
				replies.add(reply);
			}
			return replies;
		} catch (SecurityException se) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

}
