package org.littlered.dataservices.rest.controller.external;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.littlered.dataservices.rest.params.external.ScholarshipRequest;
import org.littlered.dataservices.rest.params.external.VolunteerSignupRequest;
import org.littlered.dataservices.service.GoogleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Controller
@RequestMapping("/services/google")
@Api(description = "Operations pertaining to Google services.")
public class GoogleController {

	@Autowired
	private GoogleService googleService;

	private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

	@ApiOperation(value = "Log volunteer signup data.", notes = "Information will appear in this year's volunteer Google Sheet.")
	@RequestMapping(value = "/add-volunteer", method = RequestMethod.POST)
	public void writeVolunteerData(@RequestBody VolunteerSignupRequest signupRequest, HttpServletResponse response) {

		List<List<Object>> writeData = new ArrayList<>();
		List<Object> row = new ArrayList<>();
		row.add(dateFormat.format(Calendar.getInstance().getTime()));
		row.add(signupRequest.getDisplayName());
		row.add(signupRequest.getUserId());
		row.add(signupRequest.getUserEmail());
		row.add(signupRequest.getPhone());
		row.add(signupRequest.getDiscord());
		row.add(signupRequest.getOtherInfo());
		row.add("Agreed");
		writeData.add(row);

		try {
			googleService.updateGoogleSheet(writeData, GoogleService.VOLUNTEER_SHEET);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	@ApiOperation(value = "Log scholarship request data.", notes = "Information will appear in this year's scholarship request Google Sheet.")
	@RequestMapping(value = "/add-scholarship", method = RequestMethod.POST)
	public void writeScholarshipData(@RequestBody ScholarshipRequest scholarshipRequest, HttpServletResponse response) {

		List<List<Object>> writeData = new ArrayList<>();
		List<Object> row = new ArrayList<>();
		row.add(dateFormat.format(Calendar.getInstance().getTime()));
		row.add(scholarshipRequest.getDisplayName());
		row.add(scholarshipRequest.getUserId());
		row.add(scholarshipRequest.getUserEmail());
		row.add(scholarshipRequest.getIdentities());
		row.add(scholarshipRequest.getIdentitiesOther());
		row.add(scholarshipRequest.getGamingXp());
		row.add("Agreed");
		writeData.add(row);

		try {
			googleService.updateGoogleSheet(writeData, GoogleService.SCHOLARSHIP_SHEET);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
