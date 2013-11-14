package infosistema.openbaas.middleLayer;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.data.models.files.FileInterface;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

public class UsersMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	SessionModel sessions;
	Email emailOp;
	private static PasswordEncryptionService service;
	
	// *** INSTANCE *** //

	private static UsersMiddleLayer instance = null;

	protected static UsersMiddleLayer getInstance() {
		if (instance == null) instance = new UsersMiddleLayer();
		return instance;
	}
	
	private UsersMiddleLayer() {
		super();
		sessions = new SessionModel();
		emailOp = new Email();
	}

	// *** CREATE *** //

	public User createUserAndLogin(MultivaluedMap<String, String> headerParams, UriInfo uriInfo, String appId, String userName, 
			String email, String password, String userFile) {
		User outUser = new User();

		String userId = null;
		List<String> userAgentList = null;
		List<String> locationList = null;
		String userAgent = null;
		String location = null;
		
		userId = Utils.getRandomString(Const.getIdLength());
		while (identifierInUseByUserInApp(appId, userId))
			userId = Utils.getRandomString(Const.getIdLength());
		byte[] salt = null;
		byte[] hash = null;
		PasswordEncryptionService service = new PasswordEncryptionService();
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(password, salt);
		} catch (NoSuchAlgorithmException e) {
			Log.error("", this, "createUserAndLogin", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
		} catch (InvalidKeySpecException e) {
			Log.error("", this, "createUserAndLogin", "Invalid Key.", e); 
		}

		if (!getConfirmUsersEmailOption(appId)) {
			SessionMiddleLayer sessionMid = MiddleLayerFactory.getSessionMiddleLayer();
			createUser(appId, userId, userName, "NOK", "SocialNetwork", email, salt, hash, userFile, null, null);
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, password);
			for (Entry<String, List<String>> entry : headerParams.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(Const.LOCATION))
					locationList = entry.getValue();
				else if (entry.getKey().equalsIgnoreCase("user-agent")){
					userAgentList = entry.getValue();
				}	
			}
			if (locationList != null)
				location = locationList.get(0);
			if (userAgentList != null)
				userAgent = userAgentList.get(0);
			
			Boolean refresh = sessionMid.refreshSession(sessionToken, location, userAgent);

			if (validation && refresh) {
				outUser.setUserID(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setUserEmail(email);
				outUser.setUserName(userName);
			}
		} else if (getConfirmUsersEmailOption(appId)) {
			boolean emailConfirmed = false;
			createUser(appId, userId, userName, "NOK", "SocialNetwork", email, salt,hash, userFile, emailConfirmed, uriInfo);
			outUser.setUserID(userId);
			outUser.setUserEmail(email);
			outUser.setUserName(userName);
		}
		return outUser;
		
	}
	
	public User createSocialUserAndLogin(MultivaluedMap<String, String> headerParams, String appId, 
			String userName, String email, String socialId, String socialNetwork) {
		User outUser = new User();
		String userId = null;
		List<String> userAgentList = null;
		List<String> locationList = null;
		String userAgent = null;
		String location = null;
		
		userId = Utils.getRandomString(Const.getIdLength());
		while (identifierInUseByUserInApp(appId, userId))
			userId = Utils.getRandomString(Const.getIdLength());
		byte[] salt = null;
		byte[] hash = null;
		PasswordEncryptionService service = new PasswordEncryptionService();
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(socialId, salt);
		} catch (NoSuchAlgorithmException e) {
			Log.error("", this, "createSocialUserAndLogin", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
		} catch (InvalidKeySpecException e) {
			Log.error("", this, "createSocialUserAndLogin", "Invalid Key.", e); 
		}

		SessionMiddleLayer sessionMid = MiddleLayerFactory.getSessionMiddleLayer();
		createUser(appId, userId, userName, socialId, socialNetwork, email, salt, hash, null, null, null);
		String sessionToken = Utils.getRandomString(Const.getIdLength());
		boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.LOCATION))
				locationList = entry.getValue();
			else if (entry.getKey().equalsIgnoreCase("user-agent")){
				userAgentList = entry.getValue();
			}	
		}
		if (locationList != null)
			location = locationList.get(0);
		if (userAgentList != null)
			userAgent = userAgentList.get(0);
		
		sessionMid.refreshSession(sessionToken, location, userAgent);

		if (validation) {
			outUser.setUserID(userId);
			outUser.setReturnToken(sessionToken);
			outUser.setUserEmail(email);
			outUser.setUserName(userName);
		}
		
		return outUser;
		
	}

	/**
	 * Password already comes hashed, it's safer than having the password
	 * floating around.
	 * 
	 * @param appId
	 * @param userId
	 * @param email
	 * @param email2
	 * @param password
	 * @return
	 */
	public boolean createUser(String appId, String userId, String userName, String socialId, String socialNetwork,
			String email, byte[] salt, byte[] hash,	String flag, Boolean emailConfirmed, UriInfo uriInfo) {
		boolean sucessModel = false;
		try {
			userModel.createUser(appId, userId, userName, socialId, socialNetwork, email, salt, hash,
					(new Date()).toString(), flag, emailConfirmed);
			String ref = Utils.getRandomString(Const.getIdLength());
			if (uriInfo != null) {
				emailOp.sendRegistrationEmailWithRegistrationCode(appId, userId, userName, email, ref, uriInfo.getAbsolutePath().toASCIIString());
			}
			this.emailOp.addUrlToUserId(appId, userId, ref);
		} catch (Exception e) {
			Log.error("", this, "createUser", "An error ocorred.", e); 
		}
		return sucessModel;
	}

	// *** UPDATE *** //
	
	public void updateUser(String appId, String userId, String email) {
		userModel.updateUser(appId, userId, email);
	}

	public void updateUser(String appId, String userId, String email, byte[] hash, byte[] salt) {
		try {
			userModel.updateUser(appId, userId, email, hash, salt);
		} catch (UnsupportedEncodingException e) {
			Log.error("", this, "updateUser", "Unsupported Encoding.", e); 
		}
	}

	public void updateUser(String appId, String userId, String email, byte[] hash, byte[] salt, String alive) {
		try {
			userModel.updateUser(appId, userId, email, hash, salt,	alive);
		} catch (UnsupportedEncodingException e) {
			Log.error("", this, "updateUser", "Unsupported Encoding.", e); 
		}
	}

	public boolean updateUserPassword(String appId, String userId, String password) {
		byte[] salt = null;
		byte [] hash = null;
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(password, salt);
		} catch (NoSuchAlgorithmException e) {
			Log.error("", this, "updateUserPassword", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
		} catch (InvalidKeySpecException e) {
			Log.error("", this, "updateUserPassword", "Invalid Key.", e); 
		}
		boolean sucess = false;
		String email = userModel.getEmailUsingUserId(appId, userId);
		try {
			if (appModel.appExists(appId) && userModel.userExistsInApp(appId, email)) {
				userModel.updateUserPassword(appId, userId, hash, salt);
			}
		} catch (UnsupportedEncodingException e) {
			Log.error("", this, "updateUserPassword", "Unsupported Encoding.", e); 
		}

		return sucess;
	}

	// *** DELETE *** //
	
	public boolean deleteUserInApp(String appId, String userId) {
		FileInterface fileModel = getAppFileInterface(appId);
		try {
			fileModel.deleteUser(appId, userId);
		} catch(Exception e) { }
		boolean operationOk = false;
		String email = userModel.getEmailUsingUserId(appId, userId);
		if (userModel.userExistsInApp(appId, email)) {
			operationOk = userModel.deleteUser(appId, userId);
		}
		return operationOk;
	}

	// *** GET LIST *** //

	
	// *** GET *** //
	
	public ArrayList <String> getAllUserIdsForApp(String appId, Double latitude, Double longitude, Double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (latitude != null && longitude != null && radius != null) {
			Geolocation geo = Geolocation.getInstance();
			return geo.getObjectsInDistance(latitude, longitude, radius, appId, ModelEnum.users);
		} else {
			return userModel.getAllUserIdsForApp(appId,pageNumber,pageSize,orderBy,orderType);
		}
	}

	public User getUserInApp(String appId, String userId) {
		Map<String, String> userFields = null;
		try {
			userFields = getUserFields(appId, userId);
		} catch (UnsupportedEncodingException e) {
			Log.error("", this, "getUserInApp", "Unsupported Encoding.", e); 
		}
		User temp = new User(userId);
		for (Map.Entry<String, String> entry : userFields.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("email"))
				temp.setUserEmail(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("creationDate"))
				temp.setCreationDate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("updatedDate"))
				temp.setUpdatedDate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("alive"))
				temp.setAlive(entry.getValue());
		}
		return temp;
	}

	public String getEmailUsingUserName(String appId, String userName) {
		return userModel.getEmailUsingUserName(appId, userName);
	}

	public String getUserIdUsingUserName(String appId, String userName) {
		return userModel.getUserIdUsingUserName(appId, userName);
	}

	public String getUserIdUsingEmail(String appId, String email) {
		return userModel.getUserIdUsingEmail(appId, email);
	}
	
	public User getUserUsingEmail(String appId, String email) {
		return userModel.getUserUsingEmail(appId, email);
	}

	// *** EXISTS *** //

	
	// *** OTHERS *** //
	
	public boolean userExistsInApp(String appId, String userId, String email) {
		return userModel.userExistsInApp(appId, email);
	}

	public boolean userExistsInApp(String appId, String userId) {
		return userModel.userExistsInApp(appId, userModel.getEmailUsingUserId(appId, userId));
	}

	public String socialUserExistsInApp(String appId, String socialId, String socialNetwork) {
		if (userModel.socialUserExistsInApp(appId, socialId, socialNetwork))
			return userModel.getUserIdUsingSocialInfo(appId, socialId,socialNetwork);
		return null;
	}

	public boolean identifierInUseByUserInApp(String appId, String userId) {
		return userModel.identifierInUseByUserInApp(appId, userId);
	}

	public boolean getConfirmUsersEmailOption(String appId) {
		if (appModel.appExists(appId))
			return appModel.getConfirmUsersEmail(appId);
		else
			return false;
	}

	public String getUrlUserId(String appId, String userId) {
		return this.emailOp.getUrlUserId(appId, userId);
	}

	public void removeUrlToUserId(String appId, String userId) {
		this.emailOp.removeUrlToUserId(appId, userId);
	}

	public void confirmUserEmail(String appId, String userId) {
		String email = userModel.getEmailUsingUserId(appId, userId);
		if (userModel.userExistsInApp(appId, email)) {
			userModel.confirmUserEmail(appId, userId);
		}
	}

	public boolean userEmailIsConfirmed(String appId, String userId) {
		return userModel.userEmailIsConfirmed(appId, userId);
	}

	public boolean updateConfirmUsersEmailOption(String appId, Boolean confirmUsersEmail) {
		boolean sucess = false;
		if (appModel.appExists(appId)) {
			appModel.updateConfirmUsersEmailOption(appId, confirmUsersEmail);
			sucess = true;
		}
		return sucess;
	}

	public boolean recoverUser(String appId, String userId, String email, UriInfo uriInfo,String newPass, byte[] hash, byte[] salt) {
		boolean opOk = false;
		try {
			Map<String, String> user = this.getUserFields(appId, userId);
			String dbEmail = null;
			String userName = null;
			for(Map.Entry<String,String> entry : user.entrySet()){
				if(entry.getKey().equalsIgnoreCase("email")){
					dbEmail = entry.getValue();
				}
				else if(entry.getKey().equalsIgnoreCase("userName"))
					userName = entry.getValue();
			}
			if (email != null && newPass != null) {
				updateUser(appId, userId, email, hash, salt);
			}
			if(dbEmail.equalsIgnoreCase(email)){
				boolean emailOk =emailOp.sendRecoveryEmail(appId, userName, userId, email, newPass, 
						uriInfo.getAbsolutePath().toASCIIString());
				if(emailOk){

					opOk = true;
				}
			}
		} catch (UnsupportedEncodingException e) {
			Log.error("", this, "recoverUser", "Unsupported Encoding.", e); 
		}
		return opOk;
	}

	public String getRecoveryCode(String appId, String userId) {
		return this.emailOp.getRecoveryCodeOfUser(appId, userId);
	}

	private Map<String, String> getUserFields(String appId, String userId)throws UnsupportedEncodingException {
		return userModel.getUser(appId, userId);
	}

}
