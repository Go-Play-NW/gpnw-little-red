package org.littlered.dataservices.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import javax.transaction.Transactional;
import java.util.HashMap;

/**
 * Created by Jeremy on 8/30/2017.
 */
@Service
//@Transactional
public class EmailService {

	@Autowired
	private AwsSesService awsSesService;

	@Value("${email.useSes}")
	private String useSes;

	@Value("${spring.mail.from}")
	private String sender;

	@Value("${spring.mail.host}")
	private String host;

	@Value("${spring.mail.port}")
	private String port;

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	public void sendEmail(HashMap<String, String> email) throws Exception {

		boolean ses = new Boolean(useSes);
		if(ses) {
			awsSesService.sendEmail(email);
			return;
		}

		if(email == null){
			return;
		}

		JavaMailSenderImpl impl = new JavaMailSenderImpl();
		impl.setHost(host);
		impl.setPort(Integer.parseInt(port));

		MimeMessage mimeMessage = impl.createMimeMessage();
		try {
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
			message.setFrom(sender);
			message.setTo(email.get("to"));
			message.setSubject(email.get("subject"));
			message.setText(email.get("body"));
			message.setFrom("info@goplaynw.org");

			logger.info("Sending to " + email.get("to") + " regarding " + email.get("subject"));
			impl.send(mimeMessage);
		} catch (Exception e ){
			e.printStackTrace();
			// TODO have this complain to whatever Discord link we eventually create
//			throw e;
		}
		logger.info("Done sending email.");

	}

}
