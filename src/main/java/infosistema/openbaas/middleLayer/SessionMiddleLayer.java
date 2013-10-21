package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.email.EmailInterface;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.models.Model;
import infosistema.openbaas.dataaccess.sessions.RedisSessions;
import infosistema.openbaas.dataaccess.sessions.SessionInterface;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Map;

public class SessionMiddleLayer {

	// *** MEMBERS *** ///

	Model model;
	SessionInterface sessions;
	EmailInterface emailOp;
	private static PasswordEncryptionService service;
	private static String OPENBAASADMIN = "openbaasAdmin";
	
	// *** INSTANCE *** ///
	
	private static SessionMiddleLayer instance = null;

	protected static SessionMiddleLayer getInstance() {
		if (instance == null) instance = new SessionMiddleLayer();
		return instance;
	}
	
	private SessionMiddleLayer() {
		model = Model.getModel(); // SINGLETON
		// simulate();
		service = new PasswordEncryptionService();
		sessions = new RedisSessions();
		emailOp = new Email();
	}

	// *** CREATE *** ///
	
	public boolean createSession(String sessionId, String appId, String userId,
			String attemptedPassword) {
		boolean sucess = false;
		boolean ok = false;
		ok = model.authenticateUser(appId, userId, attemptedPassword);
		System.out.println("AUTHENTICATED: " + ok);
		if (ok) {
			sessions.createSession(sessionId, appId, userId);
			sucess = true;
		}
		return sucess;
	}

	public void createAdmin(String OPENBAASADMIN, byte[] adminSalt, byte[] adminHash) throws UnsupportedEncodingException {
		sessions.createAdmin(OPENBAASADMIN, adminSalt, adminHash);
	}

	public boolean createAdminSession(String sessionId, String adminId,
			String attemptedPassword) {
		byte[] adminSalt = null;
		byte[] adminHash = null;
		boolean sucess = false;
		Map<String, String> adminFields = null;
		try {
			adminFields = this.getAdminFields(OPENBAASADMIN);
			for (Map.Entry<String, String> entry : adminFields.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("adminSalt")) {
					adminSalt = entry.getValue().getBytes("ISO-8859-1");
				} else if (entry.getKey().equalsIgnoreCase("adminHash")) {
					adminHash = entry.getValue().getBytes("ISO-8859-1");
				}
			}
			System.out.println("ADMIN HASH: " + adminHash.toString());
			System.out.println("ADMIN SALT: " + adminSalt.toString());
			if (adminId.equals(OPENBAASADMIN)
					&& service.authenticate(attemptedPassword, adminHash,
							adminSalt)) {
				sessions.createAdminSession(sessionId, adminId);
				sucess = true;
			}
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Hashing Algorithm does not exist.");
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			System.out.println("InvalidKey.");
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported Encoding.");
			e.printStackTrace();
		}
		return sucess;
	}
	
	// *** UPDATE *** ///
	
	// *** DELETE *** ///
	
	public void deleteSessionForUser(String sessionToken, String userId) {
		sessions.deleteUserSession(sessionToken, userId);
	}

	public boolean deleteUserSession(String sessionToken, String userId) {
		return sessions.deleteUserSession(sessionToken, userId);
	}

	public boolean deleteAllUserSessions(String userId) {
		return sessions.deleteAllUserSessions(userId);
	}

	// *** GET *** ///
	
	public Map<String, String> getAdminFields(String OPENBAASADMIN)
			throws UnsupportedEncodingException {
		Map<String, String> adminFields = sessions.getAdminFields(OPENBAASADMIN);
		return adminFields;
	}

	public String getUserUsingSessionToken(String sessionToken) {
		return sessions.getUserUsingSessionToken(sessionToken);
	}

	// *** OTHERS *** ///
	
	public boolean sessionTokenExistsForUser(String sessionToken, String userId) {
		return sessions.sessionTokenExistsForUser(sessionToken, userId);
	}

	public boolean adminExists(String OPENBAASADMIN) {
		boolean adminExists = false;
		adminExists = sessions.adminExists(OPENBAASADMIN);
		if (!adminExists)
			adminExists = sessions.adminExists(OPENBAASADMIN);
		return adminExists;
	}

	public boolean refreshSession(String sessionToken, String location, String userAgent) {
		return sessions.refreshSession(sessionToken, location, new Date().toString(), userAgent);
	}

	public boolean sessionTokenExists(String sessionToken) {
		return sessions.sessionTokenExists(sessionToken);
	}

	public boolean sessionExistsForUser(String userId) {
		return sessions.sessionExistsForUser(userId);
	}

}
