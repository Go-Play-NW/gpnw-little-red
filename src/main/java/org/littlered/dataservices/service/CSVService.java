package org.littlered.dataservices.service;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.littlered.dataservices.dto.eventManager.EventScheduleDataDTO;
import org.littlered.dataservices.entity.eventManager.EmEvents;
import org.littlered.dataservices.util.CSVHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class CSVService {

	@Autowired
	private EventsService eventsService;

	public ByteArrayInputStream load() throws Exception {
		List<EventScheduleDataDTO> tutorials = eventsService.getEventData(null);

		ByteArrayInputStream in = CSVHelper.eventsToCSV(tutorials);
		return in;
	}
}