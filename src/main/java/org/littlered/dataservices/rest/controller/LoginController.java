package org.littlered.dataservices.rest.controller;

import org.littlered.dataservices.rest.params.AuthenticationResponse;
import org.littlered.dataservices.rest.params.PerformResetPassword;
import org.littlered.dataservices.rest.params.RequestResetPassword;
import org.littlered.dataservices.security.JWTUtil;
import org.littlered.dataservices.service.SecurityService;
import io.jsonwebtoken.impl.DefaultClaims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Jeremy on 3/25/2017.
 */
@RestController
@Api(description = "Operations pertaining to logins and app status.")
public class LoginController {

	Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private JWTUtil jwtUtil;

	@Autowired
	private SecurityService securityService;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String hello() {
		return "roll plus login";
	}

	@ApiOperation(value = "Authenticate a user.", notes = "Uses either username or email in payload.")
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public void login(){

	}

	//@RequestMapping(value = "/refreshtoken", method = RequestMethod.GET)
	public ResponseEntity<?> refreshtoken(HttpServletRequest request) throws Exception {
		// From the HttpRequest get the claims
		DefaultClaims claims = (io.jsonwebtoken.impl.DefaultClaims) request.getAttribute("claims");

		Map<String, Object> expectedMap = getMapFromIoJsonwebtokenClaims(claims);
		String token = jwtUtil.doGenerateRefreshToken(expectedMap, expectedMap.get("sub").toString());
		return ResponseEntity.ok(new AuthenticationResponse(token));
	}

	public Map<String, Object> getMapFromIoJsonwebtokenClaims(DefaultClaims claims) {
		Map<String, Object> expectedMap = new HashMap<String, Object>();
		for (Map.Entry<String, Object> entry : claims.entrySet()) {
			expectedMap.put(entry.getKey(), entry.getValue());
		}
		return expectedMap;
	}

	@ApiOperation(value = "Request a password reset. Sends an email to the user, if found by the supplied email.", notes = "Uses email in payload.")
	@RequestMapping(value = "/password/request", method = RequestMethod.POST, produces = "text/plain")
	public String requestResetPasswordForUser(@RequestBody RequestResetPassword requestResetPassword, HttpServletResponse response){
		try {
			securityService.requestResetPasswordForUser(requestResetPassword.getEmailAddress(), requestResetPassword.getEmailSubject(),
					requestResetPassword.getEmailBody());
			response.setStatus(HttpServletResponse.SC_OK);
			return "OK";
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return e.getLocalizedMessage();
		}
		catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return e.getLocalizedMessage();
		}
	}

	@ApiOperation(value = "Perform a password reset, using the token sent to the user via email.", notes = "Uses email in payload.")
	@RequestMapping(value = "/password/reset", method = RequestMethod.POST, produces = "text/plain")
	public String performResetPasswordForUser(@RequestBody PerformResetPassword performResetPassword, HttpServletResponse response){
		try {
			if (performResetPassword.getEmailAddress().equals("necanthrope+passtest@gmail.com")) {
				logger.info("PASSTEST: |" + performResetPassword.getPassword() + "|" );
			}
			securityService.performResetPasswordForUser(performResetPassword.getEmailAddress(), performResetPassword.getPassword(),
					performResetPassword.getUuid());
			response.setStatus(HttpServletResponse.SC_OK);
			return "OK";
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return e.getLocalizedMessage();
		}
		catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return e.getLocalizedMessage();
		}
	}


}
