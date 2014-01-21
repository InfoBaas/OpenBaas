package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Map;

public class SessionMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	SessionModel sessions;
	Email emailOp;
	private static PasswordEncryptionService service;
	private static String OPENBAASADMIN = "openbaasAdmin";
	
	// *** INSTANCE *** //
	
	private static SessionMiddleLayer instance = null;

	public static SessionMiddleLayer getInstance() {
		if (instance == null) instance = new SessionMiddleLayer();
		return instance;
	}
	
	private SessionMiddleLayer() {
		super();
		service = new PasswordEncryptionService();
		sessions = new SessionModel();
		emailOp = new Email();
	}

	// *** CREATE *** //
	
	public boolean createSession(String sessionId, String appId, String userId, String attemptedPassword) {
		boolean sucess = false;
		try{
			sucess = authenticateUser(appId, userId, attemptedPassword);
			sessions.createSession(sessionId, appId, userId);
			sucess = true;
		}catch (Exception e1){
			sucess = false;
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
			Log.debug("", this, "createAdminSession", "ADMIN HASH: " + adminHash.toString());
			Log.debug("", this, "createAdminSession", "ADMIN SALT: " + adminSalt.toString());
			if (adminId.equals(OPENBAASADMIN)
					&& service.authenticate(attemptedPassword, adminHash,
							adminSalt)) {
				sessions.createAdminSession(sessionId, adminId);
				sucess = true;
			}
		} catch (NoSuchAlgorithmException e) {
			Log.error("", this, "createAdminSession", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
		} catch (InvalidKeySpecException e) {
			Log.error("", this, "createAdminSession", "Invalid Key.", e); 
		} catch (UnsupportedEncodingException e) {
			Log.error("", this, "createAdminSession", "Unsupported Encoding.", e); 
		}
		return sucess;
	}

	//private
	
	public Boolean authenticateUser(String appId, String userId, String attemptedPassword) {
		try {
			Map<String, String> userFields = userModel.getUser(appId, userId);
			PasswordEncryptionService service = new PasswordEncryptionService();
			byte[] salt = null;
			byte[] hash = null;
			boolean authenticated = false;
			for (Map.Entry<String, String> entry : userFields.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("salt")) {
					salt = entry.getValue().getBytes("ISO-8859-1");
				} else if (entry.getKey().equalsIgnoreCase("hash")) {
					hash = entry.getValue().getBytes("ISO-8859-1");
				}
			}
			authenticated = service.authenticate(attemptedPassword, hash, salt);
			return authenticated;
		} catch (UnsupportedEncodingException e) {
			Log.error("", this, "authenticateUser", "Unsupported Encoding.", e); 
		} catch (NoSuchAlgorithmException e) {
			Log.error("", this, "authenticateUser", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
		} catch (InvalidKeySpecException e) {
			Log.error("", this, "authenticateUser", "Invalid Key.", e); 
		}
		return false;
	}


	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	public void deleteSessionForUser(String sessionToken, String userId) {
		sessions.deleteUserSession(sessionToken, userId);
	}

	public boolean deleteUserSession(String sessionToken, String userId) {
		return sessions.deleteUserSession(sessionToken, userId);
	}

	public boolean deleteAllUserSessions(String userId) {
		return sessions.deleteAllUserSessions(userId);
	}

	// *** GET LIST *** //

	// *** GET *** //
	
	public Map<String, String> getAdminFields(String OPENBAASADMIN)
			throws UnsupportedEncodingException {
		Map<String, String> adminFields = sessions.getAdminFields(OPENBAASADMIN);
		return adminFields;
	}

	public String getUserIdUsingSessionToken(String sessionToken) {
		return sessions.getUserIdUsingSessionToken(sessionToken);
	}

	// *** EXISTS *** //
	
	// *** OTHERS *** //
	
	public boolean checkAppForToken(String sessionToken, String appId) {
		try {
			String userId = getUserIdUsingSessionToken(sessionToken);
			String sessionAppId = null;
			SessionModel sessions = new SessionModel();
			if(sessions.sessionExistsForUser(userId))
				sessionAppId = sessions.getAppIdForSessionToken(sessionToken);
			return appId != null && appId.equals(sessionAppId);
		} catch (Exception e) {
			Log.error("", this, "checkAppForToken", "Error checking App dor Session.", e);
			return false;
		}
	}

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
		return sessionsModel.sessionExistsForUser(userId);
	}
	

}
