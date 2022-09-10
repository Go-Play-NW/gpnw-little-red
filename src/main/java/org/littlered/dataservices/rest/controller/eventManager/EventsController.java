package org.littlered.dataservices.rest.controller.eventManager;

import org.littlered.dataservices.rest.params.UploadFileResponse;
import org.littlered.dataservices.service.FileStorageService;
import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.littlered.EventMetadataDTO;
import org.littlered.dataservices.dto.eventManager.CreateEventDTO;
import org.littlered.dataservices.entity.wordpress.shrt.BbcUsersShort;
import org.littlered.dataservices.exception.FavoritingException;
import org.littlered.dataservices.rest.params.eventManager.FavoriteEvent;
import org.littlered.dataservices.rest.params.eventManager.EventAndUser;
import org.littlered.dataservices.rest.params.eventManager.EventFind;
import org.littlered.dataservices.service.EventsJPAService;
import org.littlered.dataservices.service.EventsService;
import org.littlered.dataservices.service.FavoritesService;
import org.littlered.dataservices.service.UsersService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by Jeremy on 3/25/2017.
 */
@RestController
@RequestMapping("/events")
@Api(description = "Operations pertaining to events for the current year.")
public class EventsController {

	@Autowired
	private EventsService eventsService;

	@Autowired
	private FavoritesService favoritesService;

	@Autowired
	private UsersService usersService;

	@Autowired
	private EventsJPAService eventsJPAService;

	@Autowired
	private FileStorageService fileStorageService;

	@Value("${file.download.baseUri}")
	private String fileDownloadBaseUri;

	@Value("${file.download.path}")
	private String fileDownloadPath;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	// BEGIN ADMINISTRATOR ONLY ENDPOINTS
	@ApiOperation(value = "Find all events for a registered user by user ID", notes = "Admin only.", response = org.littlered.dataservices.entity.eventManager.EmEvents.class, responseContainer = "Iterable")
	@RequestMapping(value = "/user", method = RequestMethod.POST, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findForUser(@RequestBody EventFind eventFind, HttpServletResponse response) throws Exception {
		if(eventFind != null)
			logger.info(eventFind.toString());
		Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> events;
		try {
			events = eventsService.findForUser(eventFind.getId(), null);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			events = new ArrayList<>();
		}
		return events;
	}

	@ApiOperation(value = "Create a favorite/starred event for another user by event ID and user ID", notes = "Admin only.", response = EventAndUser.class)
	@RequestMapping(value = "/favorite/create", method = RequestMethod.POST, produces = "application/json")
	public EventAndUser createUserEventFavorite(@RequestBody EventAndUser eventAndUser, HttpServletResponse response) throws Exception {
		if(eventAndUser != null)
			logger.info(eventAndUser.toString());
		try {
			favoritesService.createOtherUserEventFavorite(eventAndUser.getUserId(), eventAndUser.getEventId());
			eventAndUser.setStatus(Constants.STATUS_SUCCESS);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			eventAndUser.setStatus(Constants.STATUS_FAILURE);
		}
		return eventAndUser;
	}

	@ApiOperation(value = "Remove a favorite/starred event for another user by event ID and user ID", notes = "Admin only.", response = EventAndUser.class)
	@RequestMapping(value = "/favorite/delete", method = RequestMethod.DELETE, produces = "application/json")
	public EventAndUser deleteUserEventFavorite(@RequestBody EventAndUser eventAndUser, HttpServletResponse response) throws Exception {
		if(eventAndUser != null)
			logger.info(eventAndUser.toString());
		try {
			favoritesService.deleteOtherUserEventFavorite(eventAndUser.getUserId(), eventAndUser.getEventId());
			eventAndUser.setStatus(Constants.STATUS_SUCCESS);
		}
		catch (SecurityException e) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			eventAndUser.setStatus(Constants.STATUS_FAILURE);
		}
		return eventAndUser;
	}

	// END ADMINISTRATOR ONLY ENDPOINTS

	@ApiOperation(value = "List all events for the current calendar year", response = org.littlered.dataservices.entity.eventManager.EmEvents.class, responseContainer = "Iterable")
	@RequestMapping(value = "/all", method = RequestMethod.GET, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findAll(HttpServletResponse response) throws Exception {
		Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> events = eventsService.findAll();
		return events;
	}

	@ApiOperation(value = "A public facing list of all events for the current calendar year. Does not require login.", response = org.littlered.dataservices.entity.eventManager.pub.EmEvents.class, responseContainer = "Iterable")
	@RequestMapping(value = "/all/public", method = RequestMethod.GET, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.pub.EmEvents> findAllPublic(HttpServletResponse response) throws Exception {
		Iterable<org.littlered.dataservices.entity.eventManager.pub.EmEvents> eventsPublic = eventsService.findAllPublic();
		return eventsPublic;
	}

	@ApiOperation(value = "A public facing list of all events for the current calendar year since a provided epoch time. Does not require login.", response = org.littlered.dataservices.entity.eventManager.pub.EmEvents.class, responseContainer = "Iterable")
	@RequestMapping(value = "/since/{epochtime}/public", method = RequestMethod.GET, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.pub.EmEvents> findAllSincePublic(@PathVariable("epochtime") String epochTime, HttpServletResponse response) throws Exception {
		Iterable<org.littlered.dataservices.entity.eventManager.pub.EmEvents> eventsPublic = eventsService.findAllPublicAfterEpochDate(Long.parseLong(epochTime));
		return eventsPublic;
	}

	@ApiOperation(value = "List a page events for the current calendar year", response = org.littlered.dataservices.entity.eventManager.EmEvents.class, responseContainer = "Iterable")
	@RequestMapping(value = "/page/{length}/{offset}", method = RequestMethod.GET, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findPage(@PathVariable("length") Long length,
																					  @PathVariable("offset") Long offset,
																					  HttpServletResponse response) throws Exception {
		Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> events = eventsService.findAllPaginated(length, offset);
		return events;
	}

	@ApiOperation(value = "List a page events for the current calendar year", response = org.littlered.dataservices.entity.eventManager.pub.EmEvents.class, responseContainer = "Iterable")
	@RequestMapping(value = "/page/public/{length}/{offset}", method = RequestMethod.GET, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.pub.EmEvents> findPagePublic(@PathVariable("length") Long length,
																								@PathVariable("offset") Long offset,
																								HttpServletResponse response) throws Exception {
		Iterable<org.littlered.dataservices.entity.eventManager.pub.EmEvents> events = eventsService.findAllPublicPaginated(length, offset);
		return events;
	}

	@ApiOperation(value = "Count of events for the current calendar year", response = Long.class)
	@RequestMapping(value = "/count", method = RequestMethod.GET, produces = "application/json")
	public Long getCount(HttpServletResponse response) throws Exception {
		return eventsService.getCount();
	}

	@ApiOperation(value = "Find an event by event ID.", response = org.littlered.dataservices.entity.eventManager.EmEvents.class)
	@RequestMapping(value = "/find", method = RequestMethod.POST, produces = "application/json")
	public org.littlered.dataservices.entity.eventManager.EmEvents findOne(@RequestBody EventFind eventFind) throws Exception {
		org.littlered.dataservices.entity.eventManager.EmEvents event = eventsService.findOne(eventFind.getId());
		return event;
	}

	@ApiOperation(value = "Find all events for the current user.", response = Long.class, responseContainer = "List")
	@RequestMapping(value = "/me", method = RequestMethod.GET, produces = "application/json")
	public Iterable<Long> findForMe(HttpServletResponse response) throws Exception {
		Iterable<Long> events = eventsService.findForMe(null);
		return events;
	}

	@ApiOperation(value = "Find all favorited events for the current user.", response = org.littlered.dataservices.entity.eventManager.EmEvents.class, responseContainer = "List")
	@RequestMapping(value = "/me/favorites", method = RequestMethod.GET, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findFavoritesForMe(HttpServletResponse response) throws Exception {
		Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> events = eventsService.findFavoritesForMe(null);
		return events;
	}

	@ApiOperation(value = "Find all events for a category.", response = org.littlered.dataservices.entity.eventManager.EmEvents.class, responseContainer = "Iterable")
	@RequestMapping(value = "/category/{category}", method = RequestMethod.GET, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findByCategory(@PathVariable("category") String category, HttpServletResponse response) throws Exception {
		Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> events = eventsService.findAllForCategory(category);
		return events;
	}

	@ApiOperation(value = "Find all events for the current year, updated after a time specified as UTC epoch.", response = org.littlered.dataservices.entity.eventManager.EmEvents.class, responseContainer = "Iterable")
	@RequestMapping(value = "/since/{epochtime}", method = RequestMethod.GET, produces = "application/json")
	public Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> findUpdatedAfterEpochTime(@PathVariable("epochtime") String epochTime, HttpServletResponse response) throws Exception {
		Iterable<org.littlered.dataservices.entity.eventManager.EmEvents> events = eventsService.findUpdatedAfterEpochTime(Long.parseLong(epochTime), null);
		return events;
	}

	@ApiOperation(value = "Create a favorite/starred favoriteEvent for myself by favoriteEvent ID", response = FavoriteEvent.class)
	@RequestMapping(value = "/me/favorite/create", method = RequestMethod.POST)
	public FavoriteEvent createMyUserEventFavorite(@RequestBody FavoriteEvent favoriteEvent, HttpServletResponse response) throws Exception {
		if(favoriteEvent != null)
			logger.info(favoriteEvent.toString());
		try {
			favoritesService.createMyUserEventFavorite(favoriteEvent.getEventId());
			favoriteEvent.setStatus(Constants.STATUS_SUCCESS);
			favoriteEvent.setMessage("Event " + favoriteEvent.getEventId() + " added to favorites.");
		}
		catch (FavoritingException fe) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			favoriteEvent.setStatus(Constants.STATUS_FAILURE);
			favoriteEvent.setMessage(fe.getLocalizedMessage());
		}
		catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			favoriteEvent.setStatus(Constants.STATUS_FAILURE);
		}
		return favoriteEvent;
	}

	@ApiOperation(value = "Delete a favorite/starred favoriteEvent for myself by favoriteEvent ID", response = FavoriteEvent.class)
	@RequestMapping(value = "/me/favorite/delete", method = RequestMethod.DELETE)
	public FavoriteEvent deleteMyUserEventFavorite(@RequestBody FavoriteEvent favoriteEvent, HttpServletResponse response) throws Exception {
		if(favoriteEvent != null)
			logger.info(favoriteEvent.toString());
		try {
			favoritesService.deleteMyUserEventFavorite(favoriteEvent.getEventId());
			favoriteEvent.setStatus(Constants.STATUS_SUCCESS);
			favoriteEvent.setMessage("Event " + favoriteEvent.getEventId() + " removed from favorites.");
		}
		catch (FavoritingException fe) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			favoriteEvent.setStatus(Constants.STATUS_FAILURE);
			favoriteEvent.setMessage(fe.getLocalizedMessage());
		}
		catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			favoriteEvent.setStatus(Constants.STATUS_FAILURE);
		}
		return favoriteEvent;
	}

	@ApiOperation(value = "Create an event for the current year.", response = String.class)
	@RequestMapping(value = "/create", method = RequestMethod.PUT, consumes = "application/json", produces = "text/plain")
	public String createEvent(@RequestBody CreateEventDTO eventCreate, HttpServletResponse response) throws Exception {

		Long userId = usersService.getCurrentUser().getId();
		BbcUsersShort user = usersService.findShortUser(userId);

		try {
			Long eventId = eventsJPAService.saveEvents(eventCreate, user);
			return eventId.toString();
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return null;
		}

	}

	@ApiOperation(value = "List all possible categories and tags for events.", response = EventMetadataDTO.class, responseContainer = "Iterable")
	@RequestMapping(value = "/eventmeta", method = RequestMethod.GET, produces = "application/json")
	public Iterable<EventMetadataDTO> findMetaTypes() throws Exception {
		Iterable<EventMetadataDTO> eventMetas = eventsJPAService.getEventMetadata();
		return eventMetas;
	}

	@ApiOperation(value = "Upload an image file for an event.", response = Boolean.class)
	@RequestMapping(value = "/image", method = RequestMethod.POST, produces = "application/json")
	public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("eventId") String eventId, HttpServletResponse response) {
		String fileName;
		try {
			fileName = fileStorageService.storeFile(file, eventId);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			e.printStackTrace();
			return null;
		}

		String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath().path(File.separator)
				.path(fileDownloadPath.concat(File.separator))
				.path(fileName)
				.toUriString();

		if (fileDownloadBaseUri != null) {
			fileDownloadUri = fileDownloadBaseUri.concat(File.separator)
					.concat(fileDownloadPath).concat(File.separator)
					.concat(fileName);
		}

		return new UploadFileResponse(fileName, fileDownloadUri,
				file.getContentType(), file.getSize());
	}

}
