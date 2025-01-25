package org.littlered.dataservices.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Service
public class GoogleService {
	@Value("${google_drive_application_name}")
	private String applicationName;

	@Value("${google_drive_credentials_file}")
	private String credentialsFile;

	@Value("${google_drive_volunteer_sheet_id}")
	private String volunteerSheetId;

	@Value("${google_drive_scholarship_sheet_id}")
	private String scholarshipSheetId;

	public static final String VOLUNTEER_SHEET = "VOLUNTEER_SHEET";
	public static final String SCHOLARSHIP_SHEET = "SCHOLARSHIP_SHEET";

	public void updateGoogleSheet(List<List<Object>> writeData, String sheet) throws GeneralSecurityException, IOException {
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Credentials googleCredentials;
		File credentialsFile = new File("src/main/resources/" + this.credentialsFile);
		InputStream inputStream = Files.newInputStream(credentialsFile.toPath());
		List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);
		googleCredentials = GoogleCredentials.fromStream(inputStream).createScoped(scopes);
		Sheets service = new Sheets.Builder( HTTP_TRANSPORT, GsonFactory.getDefaultInstance(),
				new HttpCredentialsAdapter(googleCredentials)).setApplicationName(applicationName).build();

		try {
			String writeRange = "Sheet1";

			ValueRange vr = new ValueRange().setValues(writeData).setMajorDimension("ROWS");
			service.spreadsheets().values()
					.append(sheetMap().get(sheet), writeRange, vr)
					.setValueInputOption("USER_ENTERED")
					.execute();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private HashMap<String,String> sheetMap() {
		HashMap<String, String> sheetMap = new HashMap<>();
		sheetMap.put(VOLUNTEER_SHEET, volunteerSheetId);
		sheetMap.put(SCHOLARSHIP_SHEET, scholarshipSheetId);
		return sheetMap;
	}

}
