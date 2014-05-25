/*****************************************************************************************
Infosistema - OpenBaas
Copyright(C) 2002-2014 Infosistema, S.A.
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
www.infosistema.com
info@openbaas.com
Av. José Gomes Ferreira, 11 3rd floor, s.34
Miraflores
1495-139 Algés Portugal
****************************************************************************************/
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

	Jedis jedis;

	public Email() {
		jedis = new Jedis(Const.getRedisSessionServer(), Const.getRedisSessionPort());
	}

	public boolean addUrlToUserId(String appId, String userId, String registrationCode) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
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
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
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
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
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
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
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
			message.setContent("Dear " + userName +",\nYour new password is "+ newPass+".\nPlease enter the application and change it", "text/html;charset=UTF-8");
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
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
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
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(), Const.getRedisSessionPort());
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
			message.setContent("Dear " + userName +",\nIn order to confirm your registration, please open the following URL:\n"
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
