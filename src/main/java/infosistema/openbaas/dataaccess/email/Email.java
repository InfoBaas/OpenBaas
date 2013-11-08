package infosistema.openbaas.dataaccess.email;

import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

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

public class Email {

	//Email Properties
	private static final int RedisSessionsAndEmailPORT = 6380;
	Jedis jedis;
	private final static String server = "localhost";

	public Email() {
		jedis = new Jedis(server, RedisSessionsAndEmailPORT);
	}

	public boolean addUrlToUserId(String appId, String userId, String registrationCode) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("apps:"+appId+":users:"+userId, "registrationCode", registrationCode);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return false;
	}

	public boolean removeUrlToUserId(String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.del("apps:"+appId+":users:"+userId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return true;
	}

	public boolean updateUrlToUserId(String appId, String userId, String registrationCode) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("apps:"+appId+":users:"+userId, "registrationCode", registrationCode);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return false;
	}

	public String getUrlUserId(String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	public boolean sendRecoveryEmail(String appId, String userName, String userId, String email,
			String newPass, String url) {
		Properties props = new Properties();
		props.put("mail.smtp.auth", Const.getEmailAuth());
		props.put("mail.smtp.starttls.enable", Const.getEmailStartTLS());
		props.put("mail.smtp.host", Const.getEmailHost());
		props.put("mail.smtp.port", Const.getEmailPort());

		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(Const.getEmailOpenBaasEmail(), Const.getEmailOpenBaasEmailPassword());
			}
		});
		
		Message message = new MimeMessage(session);
		try {
			message.setFrom(new InternetAddress(Const.getEmailOpenBaasEmail()));
			InternetAddress to[] = new InternetAddress[1];
			to[0] = new InternetAddress(email);
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject(Const.getEmailSubjectEmailRecovery());
			message.setContent("Dear " + userName +"," + '\n' + "Your new password is "+ newPass+"."+ '\n' +"Please enter the application and change it", "text/html;charset=UTF-8");
			Transport.send(message);
		} catch (AddressException e) {
			Log.error("", this, "sendRecoveryEmail", "Address erros.", e); 
		} catch (MessagingException e) {
			Log.error("", this, "sendRecoveryEmail", "An error ocorred.", e); 
		}
		return true;
	}
	
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
	
	public boolean sendRegistrationEmailWithRegistrationCode(String appId, String userId,
			String userName, String email, String registrationCode, String link) {
		Properties props = new Properties();
		props.put("mail.smtp.auth", Const.getEmailAuth());
		props.put("mail.smtp.starttls.enable", Const.getEmailStartTLS());
		props.put("mail.smtp.host", Const.getEmailHost());
		props.put("mail.smtp.port", Const.getEmailPort());

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(Const.getEmailOpenBaasEmail(), Const.getEmailOpenBaasEmailPassword());
			}
		});
		Message message = new MimeMessage(session);
		try {
			message.setFrom(new InternetAddress(Const.getEmailOpenBaasEmail()));
			InternetAddress to[] = new InternetAddress[1];
			to[0] = new InternetAddress(email);
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject(Const.getEmailSubjectEmailConfirmation());
			message.setContent("Dear " + userName +"," + '\n' + "In order to confirm your registration, please open the following URL:"+'\n'
					+ link.replace("account/signup", "users") + userId+"/confirmation?registrationCode="+registrationCode, "text/html;charset=UTF-8");
			Transport.send(message);
		} catch (AddressException e) {
			Log.error("", this, "sendRegistrationEmailWithRegistrationCode", "Address erros.", e); 
		} catch (MessagingException e) {
			Log.error("", this, "sendRegistrationEmailWithRegistrationCode", "An error ocorred.", e); 
		}
		return true;
	}
}
