package infosistema.openbaas.middleLayer;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONObject;

public class UsersMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	SessionModel sessions;
	Email emailOp;

	
	// *** INSTANCE *** //

	private static UsersMiddleLayer instance = null;

	public static UsersMiddleLayer getInstance() {
		if (instance == null) instance = new UsersMiddleLayer();
		return instance;
	}
	
	private UsersMiddleLayer() {
		super();
		sessions = new SessionModel();
		emailOp = new Email();
	}

	
	// *** PRIVATE *** //
	
	private Map<String, String> getUserFields(String userName, String socialId, String socialNetwork,
			String email, String userFile, byte[] salt, byte[] hash, Boolean emailConfirmed,
			Boolean baseLocationOption, String baseLocation, Boolean alive, String location) throws UnsupportedEncodingException {

		Map<String, String> fields = new HashMap<String, String>();

		if (userName != null)
			fields.put(User.USER_NAME, userName);
		if (socialId != null && socialNetwork != null)
			fields.put(User.SOCIAL_NETWORK_ID(socialNetwork), socialId);
		if (email != null)
			fields.put(User.EMAIL, email);
		if (userFile != null)
			fields.put(User.USER_FILE, userFile);
		if (salt != null)
			fields.put(User.SALT, new String(salt, "ISO-8859-1"));
		if (hash != null)
			fields.put(User.HASH, new String(hash, "ISO-8859-1"));
		if (alive != null)
			fields.put(User.ALIVE, alive.toString());				
		if (baseLocationOption != null)
			fields.put(User.BASE_LOCATION_OPTION, baseLocationOption.toString());
		if (baseLocation != null)
			fields.put(User.BASE_LOCATION, baseLocation);
		if (location != null)
			fields.put(Const.LOCATION, location);
		if (emailConfirmed != null)
			fields.put(User.EMAIL_CONFIRMED, "" + emailConfirmed);
		
		return fields;
	}

	
	// *** CREATE *** //

	public User createUserAndLogin(MultivaluedMap<String, String> headerParams, UriInfo uriInfo, String appId, String userName, 
			String email, String password, String userFile, Boolean baseLocationOption, String baseLocation) {
		User outUser = new User();

		if (baseLocationOption == null) baseLocationOption = false;
		String userId = null;
		List<String> userAgentList = null;
		List<String> locationList = null;
		String userAgent = null;
		String location = null;
		String lastLocation =null;
		userId = Utils.getRandomString(Const.getIdLength());
		while (userModel.userIdExists(appId, userId))
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
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.LOCATION))
				locationList = entry.getValue();
			else if (entry.getKey().equalsIgnoreCase("user-agent")){
				userAgentList = entry.getValue();
			}	
		}
		if (locationList != null)
			location = locationList.get(0);
		if(baseLocationOption)
			if(location!=null)
				location = baseLocation;
		if (!getConfirmUsersEmailOption(appId)) {
			SessionMiddleLayer sessionMid = SessionMiddleLayer.getInstance();
			createUser(appId, userId, userName, "NOK", "SocialNetwork", email, salt, hash, userFile, null, null, baseLocationOption, baseLocation, location);
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, password);
			if (userAgentList != null)
				userAgent = userAgentList.get(0);
			
			Boolean refresh = sessionMid.refreshSession(sessionToken, location, userAgent);
			lastLocation = updateUserLocation(userId,appId,location);
			if(lastLocation==null)
				lastLocation = outUser.getLastLocation();
			if (validation && refresh) {
				outUser.setUserID(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setEmail(email);
				outUser.setUserName(userName);
				outUser.setUserFile(userFile);
				outUser.setBaseLocation(baseLocation);
				outUser.setBaseLocationOption(baseLocationOption.toString());
				outUser.setLastLocation(lastLocation);
			}
		} else if (getConfirmUsersEmailOption(appId)) {
			boolean emailConfirmed = false;
			createUser(appId, userId, userName, "NOK", "SocialNetwork", email, salt,hash, userFile, emailConfirmed, uriInfo,baseLocationOption,baseLocation,location);
			outUser.setUserID(userId);
			outUser.setEmail(email);
			outUser.setUserName(userName);
			outUser.setUserFile(userFile);
			outUser.setBaseLocation(baseLocation);
			outUser.setBaseLocationOption(baseLocationOption.toString());
			outUser.setLastLocation(location);
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
		while (userModel.userIdExists(appId, userId))
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

		SessionMiddleLayer sessionMid = SessionMiddleLayer.getInstance();
		createUser(appId, userId, userName, socialId, socialNetwork, email, salt, hash, null, null, null, false,null,null);
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
			outUser.setEmail(email);
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
			String email, byte[] salt, byte[] hash,	String userFile, Boolean emailConfirmed, UriInfo uriInfo, Boolean baseLocationOption, String baseLocation, String location) {
		boolean sucessModel = false;
		try {
			Map<String, String> fields = getUserFields(userName, socialId, socialNetwork, email, userFile, salt, hash, emailConfirmed,
					baseLocationOption, baseLocation, true, location);
			userModel.createUser(appId, userId, fields);
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
	public Boolean updateUser(String appId, String userId, String userName, String email,
			String userFile, Boolean baseLocationOption, String baseLocation, String location) {
		Boolean res = false;
		try {
			Map<String, String> fields = getUserFields(userName, null, null, email, userFile, null, null, null, baseLocationOption, baseLocation, true, location);
			res = userModel.updateUser(appId, userId, fields);
		} catch (Exception e) {
			Log.error("", this, "updateUser", "updateUser.", e); 
		}
		return res;
	}
	
	public boolean updateUserPassword(String appId, String userId, String password) {
		byte[] salt = null;
		byte [] hash = null;
		PasswordEncryptionService service = new PasswordEncryptionService();
		boolean sucess = false;
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(password, salt);
			if (appModel.appExists(appId) && userModel.userIdExists(appId, userId)) {
				Map<String, String> fields = getUserFields(null, null, null, null, null, salt, hash, null, null, null, null, null);
				userModel.updateUser(appId, userId, fields);
			}
		} catch (Exception e) {
			Log.error("", this, "updateUserPassword", "Unsupported Encoding.", e); 
		}

		return sucess;
	}

	// *** DELETE *** //
	
	public boolean deleteUserInApp(String appId, String userId) {
		boolean operationOk = false;
		try {
			Map<String, String> fields = getUserFields(null, null, null, null, null, null, null, null, null, null, false, null);
			operationOk = userModel.updateUser(appId, userId, fields);
		} catch (Exception e) {
			Log.error("", this, "deleteUserInApp", "deleteUserInApp.", e); 
		}
		return operationOk;
	}

	// *** GET LIST *** //

	@Override
	protected List<String> getAllSearchResults(String appId, String userId, String url, JSONObject query, String orderType, ModelEnum type) throws Exception {
		if(query==null){
			query = new JSONObject();
			JSONObject jAux= new JSONObject();
			jAux.put("$exists",1);
			query.put("email", jAux); 
			query.put("hash", jAux);
			query.put("salt", jAux); 
		}
		return docModel.getDocuments(appId, userId, url, query, orderType);
	}

	
	// *** GET *** //
	
	public User getUserInApp(String appId, String userId) {
		Map<String, String> userFields = userModel.getUser(appId, userId);
		if (userFields == null) return null;
		User temp = new User(userId);

		
		temp.setUserName(userFields.get(User.USER_NAME));
		temp.setUserFile(userFields.get(User.USER_FILE));
		temp.setEmail(userFields.get(User.EMAIL));
		temp.setAlive(userFields.get(User.ALIVE));
		temp.setEmailConfirmed(userFields.get(User.EMAIL_CONFIRMED));
		temp.setBaseLocationOption(userFields.get(User.BASE_LOCATION_OPTION));
		temp.setBaseLocation(userFields.get(User.BASE_LOCATION));
		temp.setLastLocation(userFields.get(User.LOCATION));
		
		return temp;
	}

	public String getEmailUsingUserName(String appId, String userName) {
		String userId = userModel.getUserIdUsingUserName(appId, userName); 
		return userModel.getUserField(appId, userId, User.EMAIL);
	}

	public String getUserIdUsingUserName(String appId, String userName) {
		return userModel.getUserIdUsingUserName(appId, userName);
	}

	public String getUserIdUsingEmail(String appId, String email) {
		return userModel.getUserIdUsingEmail(appId, email);
	}
	
	public User getUserUsingEmail(String appId, String email) {
		return getUserInApp(appId, userModel.getUserIdUsingEmail(appId, email));
	}

	// *** EXISTS *** //

	public boolean userEmailExists(String appId, String email) {
		return userModel.userEmailExists(appId, email);
	}

	public boolean userIdExists(String appId, String userId) {
		return userModel.userIdExists(appId, userId);
	}

	public String socialUserExists(String appId, String socialId, String socialNetwork) {
		if (userModel.socialUserExists(appId, socialId, socialNetwork))
			return userModel.getUserIdUsingSocialInfo(appId, socialId,socialNetwork);
		return null;
	}


	// *** METADATA *** //
	
	// *** OTHERS *** //
	
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
		try {
			Map<String, String> fields = getUserFields(null, null, null, null, null, null, null, true, null, null, null, null);
			userModel.updateUser(appId, userId, fields); 
		} catch (UnsupportedEncodingException e) {
		}
	}

	public boolean userEmailIsConfirmed(String appId, String userId) {
		try {
			return Boolean.parseBoolean(userModel.getUserField(appId, userId, User.EMAIL_CONFIRMED));
		} catch (Exception e) {
			return !appModel.getConfirmUsersEmail(appId);
		}
	}

	public boolean updateConfirmUsersEmailOption(String appId, Boolean confirmUsersEmail) {
		boolean sucess = false;
		if (appModel.appExists(appId)) {
			appModel.updateAppFields(appId, null, null, confirmUsersEmail, null, null, null);
			sucess = true;
		}
		return sucess;
	}

	public boolean recoverUser(String appId, String userId, String email, UriInfo uriInfo, String newPass, byte[] hash, byte[] salt) {
		boolean opOk = false;
		Map<String, String> user = userModel.getUser(appId, userId);
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
			try {
				Map<String, String> fields = getUserFields(null, null, null, email, null, salt, hash, null, null, null, null, null);
				userModel.updateUser(appId, userId, fields);
			} catch (UnsupportedEncodingException e) {
				Log.error("", this, "updateUser", "Unsupported Encoding.", e); 
			}
		}
		if(dbEmail.equalsIgnoreCase(email)){
			boolean emailOk =emailOp.sendRecoveryEmail(appId, userName, userId, email, newPass, 
					uriInfo.getAbsolutePath().toASCIIString());
			if(emailOk){

				opOk = true;
			}
		}
		return opOk;
	}

	public String getRecoveryCode(String appId, String userId) {
		return this.emailOp.getRecoveryCodeOfUser(appId, userId);
	}

	public String updateUserLocation(String userId, String appId, String location) {
		User user = getUserInApp(appId,userId);
		
		try{
			if ("true".equalsIgnoreCase(user.getBaseLocationOption())) {
				location = user.getBaseLocation();
			}
			if(location!=null){
				Map<String, String> fields = getUserFields(null, null, null, null, null, null, null, null, null, null, null, location);
				userModel.updateUser(appId, userId, fields);
			}
		}catch(Exception e){
			Log.error("", this, "updateUserLocation", "updateUserLocation exception.", e); 
		}
		return location;
	}

}
