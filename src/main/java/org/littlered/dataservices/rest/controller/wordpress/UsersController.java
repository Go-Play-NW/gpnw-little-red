package org.littlered.dataservices.rest.controller.wordpress;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.wordpress.CreateUsersDTO;
import org.littlered.dataservices.dto.wordpress.UsersDTO;
import org.littlered.dataservices.entity.wordpress.Users;
import org.littlered.dataservices.exception.UniqueUserException;
import org.littlered.dataservices.rest.params.eventManager.*;
import org.littlered.dataservices.service.SecurityService;
import org.littlered.dataservices.service.UsersJPAService;
import org.littlered.dataservices.service.UsersService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Jeremy on 7/2/2017.
 */
@RestController
@RequestMapping("/users")
@Api(description = "Operations pertaining to users.")
public class UsersController {

	@Autowired
	private UsersService usersService;

	@Autowired
	private UsersJPAService usersJPAService;

	@Autowired
	SecurityService securityService;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@ApiOperation(value = "Display information on the current user.", response = UsersDTO.class)
	@RequestMapping(value = "/me", method = RequestMethod.GET)
	public UsersDTO findMe() throws Exception {
		Users user =  usersService.findMe();
		UsersDTO dto = new UsersDTO();
		dto.wrapEntity(user);
		return dto;
	}

	@ApiOperation(value = "Indicate if the current user has admin rights.", response = Boolean.class)
	@RequestMapping(value = "/me/isadmin", method = RequestMethod.GET)
	public Boolean isAdmin() throws Exception {
		try {
			securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		}
		catch (SecurityException se) {
			return false;
		}
		return true;
	}

	@ApiOperation(value = "Display information on a user, searching by username. Admin only.", response = UsersDTO.class)
	@RequestMapping(value = "/username/{username}", method = RequestMethod.GET)
	public UsersDTO findUserByUsername(@PathVariable(value="username")String userName, HttpServletResponse response) throws Exception {
		UsersDTO user = null;
		try {
			user = usersService.getUserByUsername(userName);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		return user;
	}

	@ApiOperation(value = "Display information on a user, searching by email. Admin only.", response = UsersDTO.class)
	@RequestMapping(value = "/email/{email}", method = RequestMethod.GET)
	public UsersDTO findUserByEmail(@PathVariable(value="email")String email, HttpServletResponse response) throws Exception {
		UsersDTO user = null;
		try {
			user = usersService.getUserByEmail(email);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		return user;
	}

	@ApiOperation(value = "Display information on a user, searching by ID. Admin only.", response = UsersDTO.class)
	@RequestMapping(value = "/id/{id}", method = RequestMethod.GET)
	public UsersDTO findUserById(@PathVariable Long id, HttpServletResponse response) throws Exception {
		UsersDTO user = null;
		try {
			user = usersService.getUserByUserId(id);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		return user;
	}

	@ApiOperation(value = "Return all registered users. Admin only.", response = Boolean.class)
	@RequestMapping(value = "/all", method = RequestMethod.GET)
	public ArrayList<UsersDTO> getAll(HttpServletResponse response) throws Exception {
		ArrayList<UsersDTO> users = null;
		try {
			users = usersService.getAllUsers();
		}
		catch (SecurityException se) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		return users;

	}

	@ApiOperation(value = "Set password for a user. Admin only.", response = Boolean.class)
	@RequestMapping(value = "/setPassword", method = RequestMethod.POST)
	public String setPasswordForUser(@RequestBody UserPassword userPassword , HttpServletResponse response) throws Exception {
		try {
			usersService.resetPasswordForUser(userPassword.getUserId(), userPassword.getPassword());
		}
		catch (SecurityException se) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		return "SUCCESS";
	}

	@ApiOperation(value = "Add a role to a user. Admin only.", response = Boolean.class)
	@RequestMapping(value = "/addRoleToUser", method = RequestMethod.POST)
	public List<String> addRoleToUser(@RequestBody UserRole userRole , HttpServletResponse response) throws Exception {

		if (userRole == null || userRole.getUserId() == null || userRole.getRole() == null || userRole.getRole().equals("")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

		try {
			securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
			List<String> roles = usersJPAService.addUserRole(userRole.getUserId(), userRole.getRole());
			return roles;
		}
		catch (SecurityException se) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
	}

	@ApiOperation(value = "Remove a role from a user. Admin only.", response = Boolean.class)
	@RequestMapping(value = "/removeRoleFromUser", method = RequestMethod.POST)
	public List<String> removeRoleFromUser(@RequestBody UserRole userRole , HttpServletResponse response) throws Exception {

		if (userRole == null || userRole.getUserId() == null || userRole.getRole() == null || userRole.getRole().equals("")) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

		try {
			securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
			List<String> roles = usersJPAService.removeUserRole(userRole.getUserId(), userRole.getRole());
			return roles;
		}
		catch (SecurityException se) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
	}

	@ApiOperation(value = "Set password for currently logged in user. Ignores userId field in input.", response = Boolean.class)
	@RequestMapping(value = "/setMyPassword", method = RequestMethod.POST)
	public String setPasswordForMe(@RequestBody UserPassword userPassword , HttpServletResponse response) throws Exception {
		try {
			usersService.resetPasswordForMe(userPassword.getPassword());
		}
		catch (SecurityException se) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}
		return "SUCCESS";
	}

	@ApiOperation(value = "Create a new user.")
	@RequestMapping(value = "/create", method = RequestMethod.PUT)
	public void createUser(@RequestBody CreateUsersDTO createUser, HttpServletResponse response) throws Exception {
		try {
			usersJPAService.create(createUser);
		} catch (UniqueUserException uue) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

}
