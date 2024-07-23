package org.littlered.dataservices.util;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.jetbrains.annotations.NotNull;
import org.littlered.dataservices.dto.eventManager.EventScheduleDataDTO;


public class CSVHelper {

	public static ByteArrayInputStream eventsToCSV(List<EventScheduleDataDTO> events) {
		final CSVFormat format = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.MINIMAL);

		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
			 CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), format);) {
			csvPrinter.printRecord(getList(getHeader()));
			for (EventScheduleDataDTO event : events) {
				List<String> data = getList(event);
				csvPrinter.printRecord(data);
			}
			csvPrinter.flush();
			return new ByteArrayInputStream(out.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException("fail to import data to CSV file: " + e.getMessage());
		}
	}

	private static EventScheduleDataDTO getHeader() {
		EventScheduleDataDTO headerRec = new EventScheduleDataDTO();
		headerRec.setFacilitatorName("Facilitator Name");
		headerRec.setGameTitle("Game Title");
		headerRec.setGameArt("@GameArt");
		headerRec.setNumberofPlayers("Number of Players");
		headerRec.setGameTime("Game Time");
		headerRec.setGamePitch("Game Pitch");
		headerRec.setGamePlayersList("@GamePlayersList");
		headerRec.setPlayer1("Player1");
		headerRec.setPlayer2("Player2");
		headerRec.setPlayer3("Player3");
		headerRec.setPlayer4("Player4");
		headerRec.setPlayer5("Player5");
		headerRec.setPlayer6("Player6");
		headerRec.setPlaytest("@Playtest");
		headerRec.setGameTags("Game Tags");
		headerRec.setContentWarnings("Content Warnings");
		headerRec.setFeaturedFacilitator("@FeaturedFacilitator");
		headerRec.setGameAuthor("Game Author");
		headerRec.setSystem("System");
		headerRec.setSafetyTools("Safety Tools");
		return headerRec;
	}

	private static @NotNull List<String> getList(EventScheduleDataDTO event) {
		return Arrays.asList(
				event.getFacilitatorName(),
				event.getGameTitle(),
				event.getGameArt(),
				event.getNumberofPlayers(),
				event.getGameTime(),
				event.getGamePitch(),
				event.getGamePlayersList(),
				event.getPlayer1(),
				event.getPlayer2(),
				event.getPlayer3(),
				event.getPlayer4(),
				event.getPlayer5(),
				event.getPlayer6(),
				event.getPlaytest(),
				event.getGameTags(),
				event.getContentWarnings(),
				event.getFeaturedFacilitator(),
				event.getGameAuthor(),
				event.getSystem(),
				event.getSafetyTools()
		);
	}
}