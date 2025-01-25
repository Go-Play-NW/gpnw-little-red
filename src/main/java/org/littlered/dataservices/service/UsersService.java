package org.littlered.dataservices.service;

import org.littlered.dataservices.Constants;
import org.littlered.dataservices.dto.wordpress.UsersDTO;
import org.littlered.dataservices.entity.wordpress.Users;
import org.littlered.dataservices.entity.wordpress.shrt.BbcUsersShort;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersRepositoryInterface;
import org.littlered.dataservices.repository.wordpress.interfaces.UsersShortRepositoryInterface;
import org.littlered.dataservices.security.password.PhpPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jeremy on 7/2/2017.
 */
@Service
//@Transactional
public class UsersService {

	@Autowired
	private UsersRepositoryInterface usersRepository;

	@Autowired
	private SecurityService securityService;

	@Autowired
	private UsersShortRepositoryInterface usersShortDAO;

	public org.littlered.dataservices.entity.wordpress.Users findMe() throws Exception {
		return getCurrentUser();
	}

	public org.littlered.dataservices.entity.wordpress.Users getCurrentUser() throws Exception {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		System.out.println("USER: " + authentication.getPrincipal());
		List<org.littlered.dataservices.entity.wordpress.Users> users = usersRepository.findByUserLogin((String)authentication.getPrincipal());
		if(users == null || users.size() == 0) {
			throw new SecurityException(Constants.unauthorizedMessage);
		}
		return users.get(0);
	}

	public UsersDTO getUserByUsername(String userName) throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		ArrayList<org.littlered.dataservices.entity.wordpress.Users> users = usersRepository.findByUserLogin(userName);
		if(users == null || users.size() == 0) {
			throw new Exception("No user found!");
		}
		return wrapUser(users.get(0));
	}

	public UsersDTO getUserByEmail(String email) throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		ArrayList<org.littlered.dataservices.entity.wordpress.Users> users = usersRepository.findByUserEmail(email);
		if(users == null || users.size() == 0) {
			throw new Exception("No user found!");
		}
		return wrapUser(users.get(0));
	}

	public UsersDTO getUserByUserId(Long userId) throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		ArrayList<org.littlered.dataservices.entity.wordpress.Users> users = usersRepository.findById(userId);
		if(users == null || users.size() == 0) {
			throw new SecurityException(Constants.unauthorizedMessage);
		}
		return wrapUser(users.get(0));
	}

	public ArrayList<UsersDTO> getAllUsers() throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		ArrayList<org.littlered.dataservices.entity.wordpress.Users> userRaws = (ArrayList<org.littlered.dataservices.entity.wordpress.Users>) usersRepository.findAll();
		ArrayList<UsersDTO> users = (ArrayList<UsersDTO>) userRaws
				.stream()
				.map(this::wrapUser)
				.collect(Collectors.toList());
		return users;
	}

	@Transactional
	public void resetPasswordForUser(Long userId, String password) throws Exception {
		securityService.checkRolesForCurrentUser(Constants.ROLE_LIST_ADMIN_ONLY);
		ArrayList<org.littlered.dataservices.entity.wordpress.Users> users = usersRepository.findById(userId);
		if(users == null || users.size() == 0) {
			throw new SecurityException(Constants.unauthorizedMessage);
		}
		org.littlered.dataservices.entity.wordpress.Users user = users.get(0);
		user.setUserPass(new PhpPasswordEncoder().encode(password));
	}

	@Transactional
	public void resetPasswordForMe(String password) throws Exception {
		ArrayList<org.littlered.dataservices.entity.wordpress.Users> users = usersRepository.findById(getCurrentUser().getId());
		if(users == null || users.size() == 0) {
			throw new SecurityException(Constants.unauthorizedMessage);
		}
		org.littlered.dataservices.entity.wordpress.Users user = users.get(0);
		user.setUserPass(new PhpPasswordEncoder().encode(password));
	}

	private UsersDTO wrapUser(org.littlered.dataservices.entity.wordpress.Users entity) {
		UsersDTO dto = new UsersDTO();
		dto.wrapEntity(entity);
		return dto;
	}

	public BbcUsersShort findShortUser(Long userId) throws Exception {
		ArrayList<BbcUsersShort> users = usersShortDAO.findById(userId);
		if(users == null || users.size() == 0) {
			throw new Exception("No matching user for ID ".concat(userId.toString()).concat("!"));
		}
		return users.get(0);
	}

	public boolean isUserNameAvailable(String username) {
		boolean available = true;
		ArrayList<Users> users = usersRepository.findByUserLogin(username);
		users.addAll(usersRepository.findByUserEmail(username));
		if(!users.isEmpty()) {
			available = false;
		}
		return available;
	}
}
