package sessionsAndEmailConfirmations;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class EmailOperationsClass implements EmailOperations {

	//Email Properties
		private static final String AUTH = "true";
		private static final String STARTTLS = "true";
		private static final String HOST = "mail.infosistema.com";
		private static final String PORT = "25";
		
		private static final String OPENBAASEMAIL = "I13005.openbaas@infosistema.com";
		private static final String OPENBAASEMAILPASSWORD = "Infosistema1!";
		private static final String SUBJECTEMAILCONFIRMATION = "Email Registry Confirmation";
		private static final String SUBJECTEMAILRECOVERY = "Account Recovery";
	private static final int RedisSessionsAndEmailPORT = 6380;
	Jedis jedis;
	private final static String server = "localhost";
	
	public EmailOperationsClass() {
		jedis = new Jedis(server, RedisSessionsAndEmailPORT);
	}

	@Override
	public boolean addUrlToUserId(String appId, String userId, String registrationCode) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("apps:"+appId+":users:"+userId, "registrationCode", registrationCode);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return false;
	}

	@Override
	public boolean removeUrlToUserId(String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.del("apps:"+appId+":users:"+userId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return true;
	}

	@Override
	public boolean updateUrlToUserId(String appId, String userId, String registrationCode) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("apps:"+appId+":users:"+userId, "registrationCode", registrationCode);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return false;
	}

	@Override
	public String getUrlUserId(String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		String url = null;
		try {
			url = jedis.hget("apps:"+appId+":users:"+userId, "registrationCode");
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return url;
	}

	@Override
	public boolean sendRecoveryEmail(String appId, String userName, String userId, String email,
			String newPass, String url) {
		Properties props = new Properties();
		props.put("mail.smtp.auth", AUTH);
		props.put("mail.smtp.starttls.enable", STARTTLS);
		props.put("mail.smtp.host", HOST);
		props.put("mail.smtp.port", PORT);
		
		Session session = Session.getInstance(props,
				  new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(OPENBAASEMAIL, OPENBAASEMAILPASSWORD);
					}
				  });
//		session.setDebug(true);
		Message message = new MimeMessage(session);
		try {
		message.setFrom(new InternetAddress(OPENBAASEMAIL));
		InternetAddress to[] = new InternetAddress[1];
		to[0] = new InternetAddress(email);
		message.setRecipients(Message.RecipientType.TO, to);
		message.setSubject(SUBJECTEMAILRECOVERY);
		message.setContent("Dear " + userName +"," + '\n' + "Your new password is "+ newPass+"."+ '\n' +"Please enter the application and change it", "text/html;charset=UTF-8");
		Transport.send(message);
		} catch (AddressException ex) {
		System.out.println( ex.getMessage());
		} catch (MessagingException ex) {
		System.out.println( ex.getMessage());
		}
		//addRecoveryCodeToUser(appId, userId, shortCode);
		return true;
	}
	@Override
	public boolean addRecoveryCodeToUser(String appId, String userId,
			String shortCode) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("apps:"+appId+":users:"+userId, "recoveryCode", shortCode);
			jedis.expire("apps:"+appId+":users:"+userId, 172800); //expire after 2 days
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return false;	
	}
	public String getRecoveryCodeOfUser(String appId, String userId){
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		String code = null;
		try {
			code = jedis.hget("apps:"+appId+":users:"+userId, "recoveryCode");
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return code;
	}
	@Override
	public boolean sendRegistrationEmailWithRegistrationCode(String appId, String userId,
			String userName, String email, String registrationCode, String link) {
		Properties props = new Properties();
		props.put("mail.smtp.auth", AUTH);
		props.put("mail.smtp.starttls.enable", STARTTLS);
		props.put("mail.smtp.host", HOST);
		props.put("mail.smtp.port", PORT);
		
		Session session = Session.getInstance(props,
				  new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(OPENBAASEMAIL, OPENBAASEMAILPASSWORD);
					}
				  });
//		session.setDebug(true);
		Message message = new MimeMessage(session);
		try {
		message.setFrom(new InternetAddress(OPENBAASEMAIL));
		InternetAddress to[] = new InternetAddress[1];
		to[0] = new InternetAddress(email);
		message.setRecipients(Message.RecipientType.TO, to);
		message.setSubject(SUBJECTEMAILCONFIRMATION);
		message.setContent("Dear " + userName +"," + '\n' + "In order to confirm your registration, please open the following URL:"+'\n'
				+ link.replace("account/signup", "users") + userId+"/confirmation?registrationCode="+registrationCode, "text/html;charset=UTF-8");
		Transport.send(message);
		} catch (AddressException ex) {
		System.out.println( ex.getMessage());
		} catch (MessagingException ex) {
		System.out.println( ex.getMessage());
		}
		return true;
	}
}
