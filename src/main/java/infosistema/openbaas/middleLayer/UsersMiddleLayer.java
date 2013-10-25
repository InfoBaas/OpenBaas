package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.email.EmailInterface;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.dataaccess.models.Model;
import infosistema.openbaas.dataaccess.sessions.RedisSessions;
import infosistema.openbaas.dataaccess.sessions.SessionInterface;
import infosistema.openbaas.model.ModelEnum;
import infosistema.openbaas.model.user.User;
import infosistema.openbaas.model.user.UserInterface;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

public class UsersMiddleLayer {

	// *** MEMBERS *** ///

	Model model;
	SessionInterface sessions;
	EmailInterface emailOp;
	private static PasswordEncryptionService service;
	
	// *** INSTANCE *** ///

	private static UsersMiddleLayer instance = null;

	protected static UsersMiddleLayer getInstance() {
		if (instance == null) instance = new UsersMiddleLayer();
		return instance;
	}
	
	private UsersMiddleLayer() {
		model = Model.getModel(); // SINGLETON
		// simulate();
		sessions = new RedisSessions();
		emailOp = new Email();
	}

	// *** CREATE *** ///

	public UserInterface createUserAndLogin(MultivaluedMap<String, String> headerParams, UriInfo uriInfo, String appId, String userName, 
			String email, String password, String userFile) {
		UserInterface outUser = new User();

		String userId = null;
		List<String> userAgentList = null;
		List<String> locationList = null;
		String userAgent = null;
		String location = null;
		
		userId = Utils.getRandomString(Const.IDLENGTH);
		while (identifierInUseByUserInApp(appId, userId))
			userId = Utils.getRandomString(Const.IDLENGTH);
		byte[] salt = null;
		byte[] hash = null;
		PasswordEncryptionService service = new PasswordEncryptionService();
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(password, salt);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Hashing Algorithm failed, please review the PasswordEncryptionService.");
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			System.out.println("Invalid Key.");
			e.printStackTrace();
		}

		if (!confirmUsersEmailOption(appId)) {
			SessionMiddleLayer sessionMid = MiddleLayerFactory.getSessionMiddleLayer();
			createUser(appId, userId,	userName, "NOK", "NOK", email, salt, hash, userFile);
			String sessionToken = Utils.getRandomString(Const.IDLENGTH);
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, password);
			for (Entry<String, List<String>> entry : headerParams.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("location"))
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
				outUser.setUserID2(userId);
				outUser.setReturnToken(sessionToken);
			}
		} else if (confirmUsersEmailOption(appId)) {
			boolean emailConfirmed = false;
			createUserWithEmailConfirmation(appId, userId, userName, "NOK", "NOK", email, salt,hash, userFile, emailConfirmed, uriInfo);
			outUser.setUserID2(userId);
		}
		return outUser;
		
	}
	
	
	public UserInterface createSocialUserAndLogin(MultivaluedMap<String, String> headerParams, String appId, 
			String userName, String email, String socialId, String socialNetwork) {
		UserInterface outUser = new User();
		String userId = null;
		List<String> userAgentList = null;
		List<String> locationList = null;
		String userAgent = null;
		String location = null;
		
		userId = Utils.getRandomString(Const.IDLENGTH);
		while (identifierInUseByUserInApp(appId, userId))
			userId = Utils.getRandomString(Const.IDLENGTH);
		byte[] salt = null;
		byte[] hash = null;
		PasswordEncryptionService service = new PasswordEncryptionService();
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(socialId, salt);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Hashing Algorithm failed, please review the PasswordEncryptionService.");
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			System.out.println("Invalid Key.");
			e.printStackTrace();
		}

		SessionMiddleLayer sessionMid = MiddleLayerFactory.getSessionMiddleLayer();
		createUser(appId, userId, userName, socialId, socialNetwork, email, salt, hash, null);
		String sessionToken = Utils.getRandomString(Const.IDLENGTH);
		boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("location"))
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
			outUser.setUserID2(userId);
			outUser.setReturnToken(sessionToken);
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
			String email, byte[] salt, byte[] hash, String userFile) {
		boolean sucess = false;
		try {
			sucess = this.model.createUser(appId, userId, userName, socialId, socialNetwork, email, salt, hash, userFile);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return sucess;
	}

	public boolean createUserWithEmailConfirmation(String appId, String userId, String userName, String socialId, String socialNetwork,
			String email, byte[] salt, byte[] hash,	String flag, boolean emailConfirmed, UriInfo uriInfo) {
		boolean sucessModel = false;
		try {
			sucessModel = this.model.createUserWithEmailConfirmation(appId, userId, userName, socialId, socialNetwork, email, salt, hash, flag, emailConfirmed);
			String ref = Utils.getRandomString(Const.IDLENGTH);
			emailOp.sendRegistrationEmailWithRegistrationCode(appId, userId, userName, email, ref, uriInfo.getAbsolutePath().toASCIIString());
			this.emailOp.addUrlToUserId(appId, userId, ref);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sucessModel;
	}

	// *** UPDATE *** ///
	
	public void updateUser(String appId, String userId, String email) {
		model.updateUser(appId, userId, email);
	}

	public void updateUser(String appId, String userId, String email, byte[] hash, byte[] salt) {
		model.updateUser(appId, userId, email, hash, salt);
	}

	public void updateUser(String appId, String userId, String email, byte[] hash, byte[] salt, String alive) {
		this.model.updateUser(appId, userId, email, hash, salt, alive);
	}

	public boolean updateUserPassword(String appId, String userId, String password) {
		byte[] salt = null;
		byte [] hash = null;
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(password, salt);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.model.updateUserPassword(appId, userId, hash, salt);
	}

	// *** DELETE *** ///
	
	public boolean deleteUserInApp(String appId, String userId) {
		return this.model.deleteUserInApp(appId, userId);
	}

	// *** GET *** ///
	
	public ArrayList <String> getAllUserIdsForApp(String appId, Double latitude, Double longitude, Double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (latitude != null && longitude != null && radius != null) {
			Geolocation geo = Geolocation.getInstance();
			return geo.getObjectsInDistance(latitude, longitude, radius, appId, ModelEnum.users);
		} else {
			return model.getAllUserIdsForApp(appId, pageNumber, pageSize, orderBy, orderType);
		}
	}

	public UserInterface getUserInApp(String appId, String userId) {
		Map<String, String> userFields = null;
		try {
			userFields = this.model.getUserFields(appId, userId);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		UserInterface temp = new User(userId);
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
		return model.getEmailUsingUserName(appId, userName);
	}

	public String getUserIdUsingUserName(String appId, String userName) {
		return model.getUserIdUsingUserName(appId, userName);
	}

	public String getUserIdUsingEmail(String appId, String email) {
		return model.getUserIdUsingEmail(appId, email);
	}

	// *** OTHERS *** ///
	
	public boolean userExistsInApp(String appId, String userId, String email) {
		return this.model.userExistsInApp(appId, userId, email);
	}

	public boolean userExistsInApp(String appId, String userId) {
		return this.model.userExistsInApp(appId, userId);
	}
	
	public String socialUserExistsInApp(String appId, String socialId, String socialNetwork) {
		return this.model.socialUserExistsInApp(appId, socialId, socialNetwork);
	}

	public boolean identifierInUseByUserInApp(String appId, String userId) {
		return this.model.identifierInUseByUserInApp(appId, userId);
	}

	public boolean confirmUsersEmailOption(String appId) {
		return this.model.confirmUsersEmailOption(appId);
	}

	public String getUrlUserId(String appId, String userId) {
		return this.emailOp.getUrlUserId(appId, userId);
	}

	public void removeUrlToUserId(String appId, String userId) {
		this.emailOp.removeUrlToUserId(appId, userId);
	}

	public void confirmUserEmail(String appId, String userId) {
		this.model.confirmUserEmail(appId, userId);
	}

	public boolean userEmailIsConfirmed(String appId, String userId) {
		return model.userEmailIsConfirmed(appId, userId);
	}

	public boolean updateConfirmUsersEmailOption(String appId,
			Boolean confirmUsersEmail) {
		return this.model.updateConfirmUsersEmailOption(appId, confirmUsersEmail);
	}

	public boolean recoverUser(String appId, String userId, String email, UriInfo uriInfo,String newPass, byte[] hash, byte[] salt) {
		boolean opOk = false;
		try {
			Map<String, String> user = this.model.getUserFields(appId, userId);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return opOk;
	}

	public String getRecoveryCode(String appId, String userId) {
		return this.emailOp.getRecoveryCodeOfUser(appId, userId);
	}



}
