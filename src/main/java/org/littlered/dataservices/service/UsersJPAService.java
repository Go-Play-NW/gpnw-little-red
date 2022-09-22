package org.littlered.dataservices.service;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.wordpress.CreateUsersDTO;
import org.littlered.dataservices.entity.wordpress.Usermeta;
import org.littlered.dataservices.entity.wordpress.Users;
import org.littlered.dataservices.entity.wordpress.shrt.BbcUsersShort;
import org.littlered.dataservices.exception.UniqueUserException;
import org.littlered.dataservices.repository.wordpress.interfaces.UsermetaJPAInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersJPAInterface;
import org.littlered.dataservices.security.password.PhpPasswordEncoder;
import org.littlered.dataservices.util.php.parser.SerializedPhpParser;
import com.marcospassos.phpserializer.Serializer;
import com.marcospassos.phpserializer.SerializerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by Jeremy on 7/2/2017.
 */
@Service
@Transactional
public class UsersJPAService {

	@Autowired
	private UsersJPAInterface usersRepository;

	@Autowired
	private UsermetaJPAInterface usermetaJPAInterface;

	@Autowired
	private EmailService emailService;

	@Value("${db.table_prefix}")
	private String tablePrefix;

	private final Logger logger = Logger.getLogger(this.getClass().getName());

	@Transactional
	public void create(CreateUsersDTO userIn) throws Exception {

		Date now = Calendar.getInstance().getTime();

		List<Users> loginCheck = usersRepository.findUsersByUserLogin(userIn.getUserLogin());
		if (loginCheck != null && loginCheck.size() > 0) {
			HashMap<String, String> badLogiMail = new HashMap<>();
			badLogiMail.put("subject", "Account Creation Failed at Go Play NW");
			badLogiMail.put("body", "Someone just tried to create a user account at Go Play NW with the user name associated with this email address. " +
					"If that was you, great! That means your account already exists there! Please try to log in with those credentials. " +
					"If that wasn't you, you don't need to do anything. We have protected your account!");
			badLogiMail.put("to", loginCheck.get(0).getUserEmail());
			emailService.sendEmail(badLogiMail);
			logger.info("Sent email for created account to " + loginCheck.get(0).getUserEmail());
			throw new UniqueUserException("That login is invalid.");
		}

		List<Users> emailCheck = usersRepository.findUsersByUserEmail(userIn.getUserEmail());
		if (emailCheck != null && emailCheck.size() > 0) {
			HashMap<String, String> badEmailMail = new HashMap<>();
			badEmailMail.put("subject", "Account Creation Failed at Go Play NW");
			badEmailMail.put("body", "Someone just tried to create a user account at Go Play NW with this email address. " +
					"If that was you, great! That means your account already exists there! Please try to log in with those credentials. " +
					"If that wasn't you, you don't need to do anything. We have protected your account!");
			badEmailMail.put("to", emailCheck.get(0).getUserEmail());
			emailService.sendEmail(badEmailMail);
			logger.info("Sent email for created account to " + emailCheck.get(0).getUserEmail());
			throw new UniqueUserException("That email address is invalid.");
		}

		Users user = new Users();
		user.setUserLogin(userIn.getUserLogin());
		user.setUserPass(new PhpPasswordEncoder().encode(userIn.getUserPass()));
		user.setUserNicename(userIn.getUserNicename());
		user.setUserEmail(userIn.getUserEmail());
		user.setUserUrl(userIn.getUserUrl());
		user.setUserStatus(0);
		user.setUserActivationKey("");
		user.setUserRegistered(new Timestamp(now.getTime()));
		user.setDisplayName(userIn.getDisplayName());

		usersRepository.saveAndFlush(user);

		Usermeta usermeta = new Usermeta();
		usermeta.setUserId(user.getId());
		usermeta.setMetaKey("first_name");
		usermeta.setMetaValue(userIn.getFirstName());
		usermetaJPAInterface.save(usermeta);

		usermeta = new Usermeta();
		usermeta.setUserId(user.getId());
		usermeta.setMetaKey("last_name");
		usermeta.setMetaValue(userIn.getLastName());
		usermetaJPAInterface.save(usermeta);

		usermeta = new Usermeta();
		usermeta.setUserId(user.getId());
		usermeta.setMetaKey("nickname");
		usermeta.setMetaValue(userIn.getNickname());
		usermetaJPAInterface.save(usermeta);

		usermeta = new Usermeta();
		usermeta.setUserId(user.getId());
		usermeta.setMetaKey(tablePrefix.concat("capabilities"));
		usermeta.setMetaValue("a:1:{s:12:\"notattending\";b:1;}");
		usermetaJPAInterface.save(usermeta);

		logger.info("Created account for " + userIn.getUserLogin());

		try {
			HashMap<String, String> newUserMail = new HashMap<>();
			newUserMail.put("subject", userIn.getEmailSubject());
			newUserMail.put("body", userIn.getEmailBody());
			newUserMail.put("to", userIn.getUserEmail());
			emailService.sendEmail(newUserMail);
			logger.info("Sent email for created account to " + userIn.getUserLogin());
		} catch (Exception e) {
			logger.severe("Error sending email for password reset for " + userIn.getUserEmail() + "!");
			e.printStackTrace();
		}

	}


	public List<String> addUserRole(Long pUserId, String pNewRole) throws Exception {

		List<Usermeta> usermetas =
				usermetaJPAInterface.findUsermetaByUserIdAndMetaKey(pUserId, tablePrefix.concat("capabilities"));

		if(usermetas.size() < 1) {
			throw new Exception("Either no matching user, or no no capabilities found");
		}

		if(usermetas.size() > 1) {
			throw new Exception("Multiple capabilities entries found for user " + pUserId);
		}

		Usermeta capabilities = usermetas.get(0);

		LinkedHashMap<String, Boolean> capList =
				(LinkedHashMap<String, Boolean>) new SerializedPhpParser(capabilities.getMetaValue()).parse();

		HashMap<String, Boolean> newCaps = new HashMap<>();
		for (String index : capList.keySet()) {
			if(index.equals(pNewRole)) {
				logger.info("User " + pUserId + " already has the role " + pNewRole + "!");
				continue;
			}
			newCaps.put(index, true);
		}
		newCaps.put(pNewRole, true);

		Serializer serializer = new SerializerBuilder()
				.registerBuiltinAdapters()
				.setCharset(StandardCharsets.ISO_8859_1)
				.build();
		capabilities.setMetaValue(serializer.serialize(newCaps));

		usermetaJPAInterface.save(capabilities);

		return new ArrayList<>(newCaps.keySet());
	}

	public Usermeta findUsermetaByUserIdAndMetaKey (Users user, String key) throws Exception {
		List<Usermeta> meta = usermetaJPAInterface.findUsermetaByUserIdAndMetaKey(user.getId(), key);
		if (meta.size() == 0) {
			return null;
		}
		return meta.get(0);

	}

	public void createUserMeta(Users user, String key, String value) {
		Usermeta usermeta = new Usermeta();
		usermeta.setUserId(user.getId());
		usermeta.setMetaKey(key);
		usermeta.setMetaValue(value);
		usermetaJPAInterface.save(usermeta);
	}

	public void deleteUserMeta(Users user, String key) {
		List<Usermeta> deleteMetas = usermetaJPAInterface.findUsermetaByUserIdAndMetaKey(user.getId(), key);
		usermetaJPAInterface.delete(deleteMetas);
	}

	public List<String> removeUserRole(Long pUserId, String pRemoveRole) throws Exception {
		List<Usermeta> usermetas =
				usermetaJPAInterface.findUsermetaByUserIdAndMetaKey(pUserId, tablePrefix.concat("capabilities"));

		if(usermetas.size() > 1) {
			throw new Exception("Multiple capabilities entries found for user " + pUserId);
		}

		Usermeta capabilities = usermetas.get(0);

		LinkedHashMap<String, Boolean> capList =
				(LinkedHashMap<String, Boolean>) new SerializedPhpParser(capabilities.getMetaValue()).parse();

		HashMap<String, Boolean> newCaps = new HashMap<>();
		for (String index : capList.keySet()) {
			if(!index.equals(pRemoveRole)) {
				newCaps.put(index, true);
			}
		}

		Serializer serializer = new SerializerBuilder()
				.registerBuiltinAdapters()
				.setCharset(Charset.forName("ISO-8859-1"))
				.build();
		capabilities.setMetaValue(serializer.serialize(newCaps));

		usermetaJPAInterface.save(capabilities);

		return new ArrayList<>(newCaps.keySet());
	}

	@Transactional
	public void updateUserDisplayName(String displayName, BbcUsersShort userShort) throws Exception {
		Users user = usersRepository.findOne(userShort.getId());
		user.setDisplayName(displayName);
		usersRepository.save(user);
	}

	@Transactional
	public void addOrUpdateUserMeta(Long userId, String metaKey, String metaValue) {
		List<Usermeta> foundUsermetas = usermetaJPAInterface.findUsermetaByUserIdAndMetaKey(userId, metaKey);
		if (foundUsermetas == null || foundUsermetas.size() == 0) {
			Usermeta usermeta = new Usermeta();
			usermeta.setUserId(userId);
			usermeta.setMetaKey(metaKey);
			usermeta.setMetaValue(metaValue);
			usermetaJPAInterface.save(usermeta);
		} else {
			Usermeta foundUsermeta = foundUsermetas.get(0);
			foundUsermeta.setMetaValue(metaValue);
			usermetaJPAInterface.save(foundUsermeta);
		}

	}
	
	public ArrayList<String> findUserRolesByDiscordId(String discordId) throws Exception {
		
		List<Usermeta> discordUsermetas = usermetaJPAInterface.findUsermetasByMetaKeyAndMetaValue(
				Constants.DISCORD_ID_USERMETA_KEY, discordId);
		if (discordUsermetas == null || discordUsermetas.size() == 0) {
			throw new IllegalArgumentException("No user found with discord ID " + discordId);
		}
		if (discordUsermetas.size() > 1) {
			throw new IllegalArgumentException("More than one user found with discord ID " + discordId);
		}
		Usermeta discordUserMeta = discordUsermetas.get(0);

		List<Usermeta> roleUsermetas =
				usermetaJPAInterface.findUsermetaByUserIdAndMetaKey(discordUserMeta.getUserId(),
						tablePrefix.concat("capabilities"));
		if(roleUsermetas.size() > 1) {
			throw new Exception("Multiple capabilities entries found for user");
		}
		Usermeta capabilities = roleUsermetas.get(0);

		LinkedHashMap<String, Boolean> capList =
				(LinkedHashMap<String, Boolean>) new SerializedPhpParser(capabilities.getMetaValue()).parse();
		ArrayList<String> roles = new ArrayList<>(capList.keySet());

		return roles;
		
	}

	public HashMap<String, ArrayList<String>> findUserRolesForAllDiscordUsers() throws Exception {

		List<Usermeta> discordUsermetas = usermetaJPAInterface.findUsermetasByMetaKey(
				Constants.DISCORD_ID_USERMETA_KEY);
		if (discordUsermetas == null || discordUsermetas.size() == 0) {
			throw new IllegalArgumentException("No users found with discord IDs");
		}

		HashMap<String, ArrayList<String>> roles = new HashMap<>();

		for (Usermeta discordUserMeta : discordUsermetas) {

			List<Usermeta> roleUsermetas =
					usermetaJPAInterface.findUsermetaByUserIdAndMetaKey(discordUserMeta.getUserId(),
							tablePrefix.concat("capabilities"));
			Usermeta capabilities = roleUsermetas.get(0);

			LinkedHashMap<String, Boolean> capList =
					(LinkedHashMap<String, Boolean>) new SerializedPhpParser(capabilities.getMetaValue()).parse();
			ArrayList<String> userRoles = new ArrayList<>(capList.keySet());
			roles.put(discordUserMeta.getMetaValue(), userRoles);
		}

		return roles;

	}


}
