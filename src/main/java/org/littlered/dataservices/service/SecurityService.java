package org.littlered.dataservices.service;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.entity.wordpress.Usermeta;
import org.littlered.dataservices.entity.wordpress.Users;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersRepositoryInterface;
import org.littlered.dataservices.security.password.PhpPasswordEncoder;
import org.littlered.dataservices.util.php.parser.SerializedPhpParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by Jeremy on 7/2/2017.
 */
@Service
//@Transactional
public class SecurityService {

	public final Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private UsersRepositoryInterface usersRepository;

	@Autowired
	private UsersService usersService;

	@Autowired
	private EmailService emailService;

	@Value("${db.table_prefix}")
	private String tablePrefix;

	public void checkRolesForCurrentUser(List<String> authorizedRoles) throws Exception {
		Users user = usersService.getCurrentUser();
		checkRolesForUser(user, authorizedRoles);
	}

	public void checkRolesForUser(Users user, List<String> authorizedRoles) throws Exception {
		for(Usermeta userMeta : user.getUserMetas()) {
			if (userMeta.getMetaKey().equals(tablePrefix.concat("capabilities"))) {
				try {
					System.out.println(userMeta.getMetaValue());
					HashMap<String, String> roles = (HashMap<String, String>) new SerializedPhpParser(userMeta.getMetaValue()).parse();
					System.out.println(roles);
					for (String role : roles.keySet()) {
						if(authorizedRoles.contains(role)) {
							return;
						}
					}
				} catch (Exception e) {
					throw new Exception("Cannot parse roles for " + user.getUserLogin() + "!");
				}
			}
		}
		throw new SecurityException(Constants.unauthorizedMessage);
	}

	@Transactional
	public void requestResetPasswordForUser(String emailAddress, String emailSubject, String emailBody) throws Exception {

//		checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		
		List<Users> users = usersRepository.findByUserEmail(emailAddress);
		if (users.isEmpty()) {
			throw new Exception("No user found with that email!");
		}
		if (users.size() > 1) {
			throw new Exception("Multiple users found with that email!");
		}

		Users user = users.get(0);

		String uuid = UUID.randomUUID().toString();

		emailBody = emailBody.replaceAll("\\[emailAddress\\]", emailAddress);
		emailBody = emailBody.replaceAll("\\[uuid\\]", uuid);

		user.setUserActivationKey(hashActivationKey(uuid));
		usersRepository.save(user);

		try {
			HashMap<String, String> resetEmail = new HashMap<>();
			resetEmail.put("subject", emailSubject);
			resetEmail.put("body", emailBody);
			resetEmail.put("to", emailAddress);
			emailService.sendEmail(resetEmail);
		} catch (Exception e) {
			logger.severe("Error sending email for password reset for " + emailAddress + "!");
			e.printStackTrace();
		}

	}

	@Transactional
	public void performResetPasswordForUser(String emailAddress, String password, String uuid) throws Exception {

//		checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);

		List<Users> users = usersRepository.findByUserEmail(emailAddress);
		if (users.isEmpty()) {
			throw new Exception("No user found with that email!");
		}
		if (users.size() > 1) {
			throw new Exception("Multiple users found with that email!");
		}

		Users user = users.get(0);

		if (!user.getUserActivationKey().equals(hashActivationKey(uuid))) {
			throw new Exception("Permission denied");
		}

		user.setUserPass(new PhpPasswordEncoder().encode(password));
		user.setUserActivationKey("");

		usersRepository.save(user);

	}

	private String hashActivationKey(String input) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");

		// digest() method is called to calculate message digest
		//  of an input digest() return array of byte
		byte[] messageDigest = md.digest(input.getBytes());

		// Convert byte array into signum representation
		BigInteger no = new BigInteger(1, messageDigest);

		// Convert message digest into hex value
		String hashtext = no.toString(16);
		while (hashtext.length() < 32) {
			hashtext = "0" + hashtext;
		}
		return hashtext;
	}
}
