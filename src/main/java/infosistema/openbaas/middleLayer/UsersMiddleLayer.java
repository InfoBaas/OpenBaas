package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.email.EmailInterface;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
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

import com.mongodb.util.JSONSerializers;

public class UsersMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	SessionInterface sessions;
	EmailInterface emailOp;
	private static PasswordEncryptionService service;
	
	// *** INSTANCE *** //

	private static UsersMiddleLayer instance = null;

	protected static UsersMiddleLayer getInstance() {
		if (instance == null) instance = new UsersMiddleLayer();
		return instance;
	}
	
	private UsersMiddleLayer() {
		super();
		sessions = new RedisSessions();
		emailOp = new Email();
	}

	// *** CREATE *** //

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
			if(userFile != null) 
				sucess = createUserWithFlag(appId, userId, userName, socialId, socialNetwork, email, salt, hash, userFile);
			else 
				sucess = createUserWithoutFlag(appId, userId, userName, socialId, socialNetwork, email, salt, hash);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return sucess;
	}

	public boolean createUserWithEmailConfirmation(String appId, String userId, String userName, String socialId, String socialNetwork,
			String email, byte[] salt, byte[] hash,	String flag, boolean emailConfirmed, UriInfo uriInfo) {
		boolean sucessModel = false;
		try {
			if(flag != null) 
				sucessModel = createUserWithFlagWithEmailConfirmation(appId, userId, userName, socialId, socialNetwork,
					email, salt, hash, flag, emailConfirmed);
			else 
				sucessModel = createUserWithoutFlagWithEmailConfirmation(appId, userId, userName, socialId, socialNetwork,
						email, salt, hash, emailConfirmed);
			String ref = Utils.getRandomString(Const.IDLENGTH);
			emailOp.sendRegistrationEmailWithRegistrationCode(appId, userId, userName, email, ref, uriInfo.getAbsolutePath().toASCIIString());
			this.emailOp.addUrlToUserId(appId, userId, ref);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sucessModel;
	}

	// *** UPDATE *** //
	
	public void updateUser(String appId, String userId, String email) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.updateUser(appId, userId, email);
			if (redisModel.userExistsInApp(appId, email))
				redisModel.updateUser(appId, userId, email);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public void updateUser(String appId, String userId, String email, byte[] hash, byte[] salt) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			try {
				mongoModel.updateUser(appId, userId, email, hash, salt);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			if (redisModel.userExistsInApp(appId, email))
				try {
					redisModel.updateUser(appId, userId, email, hash, salt);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public void updateUser(String appId, String userId, String email, byte[] hash, byte[] salt, String alive) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			try {
				mongoModel.updateUser(appId, userId, email, hash, salt, alive);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if (redisModel.userExistsInApp(appId, email))
				try {
					redisModel.updateUser(appId, userId, email, hash, salt,	alive);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
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
		boolean sucess = false;
		String email = mongoModel.getEmailUsingUserId(appId, userId);
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			try {
				sucess = mongoModel.updateUserPassword(appId, userId, hash, salt);
				if (redisModel.appExists(appId) && redisModel.userExistsInApp(appId, email)) {
					redisModel.updateUserPassword(appId, userId, hash, salt);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
		}
		return sucess;
	}

	// *** DELETE *** //
	
	public boolean deleteUserInApp(String appId, String userId) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			this.aws.deleteUser(appId, userId);
		else {
			System.out.println("FileSystem not yet implemented.");
		}
		boolean auxOk = false;
		boolean cacheOk = false;
		boolean operationOk = false;
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			auxOk = mongoModel.deleteUser(appId, userId);
			String email = redisModel.getEmailUsingUserId(appId, userId);
			if (redisModel.userExistsInApp(appId, email)) {
				cacheOk = redisModel.deleteUser(appId, userId);
				if (auxOk && cacheOk)
					operationOk = true;
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return operationOk;
	}

	// *** GET LIST *** //

	
	// *** GET *** //
	
	public ArrayList <String> getAllUserIdsForApp(String appId, Double latitude, Double longitude, Double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (latitude != null && longitude != null && radius != null) {
			Geolocation geo = Geolocation.getInstance();
			return geo.getObjectsInDistance(latitude, longitude, radius, appId, ModelEnum.users);
		} else {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				return mongoModel.getAllUserIdsForApp(appId,pageNumber,pageSize,orderBy,orderType);
			else if (!auxDatabase.equalsIgnoreCase(MONGODB))
				System.out.println("Database not implemented.");
			return null;
		}
	}

	public UserInterface getUserInApp(String appId, String userId) {
		Map<String, String> userFields = null;
		try {
			userFields = getUserFields(appId, userId);
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
		String email = redisModel.getEmailUsingUserName(appId, userName);
		if (email == null)
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				email = mongoModel.getEmailUsingUserName(appId, userName);
		return email;
	}

	public String getUserIdUsingUserName(String appId, String userName) {
		String userId = redisModel.getUserIdUsingUserName(appId, userName);
		if (userId == null)
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				userId = mongoModel.getUserIdUsingUserName(appId, userName);
		return userId;
	}

	public String getUserIdUsingEmail(String appId, String email) {
		String userId = redisModel.getUserIdUsingEmail(appId, email);
		if (userId == null)
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				userId = mongoModel.getUserIdUsingEmail(appId, email);
		return userId;
	}

	// *** EXISTS *** //

	
	// *** OTHERS *** //
	
	public boolean userExistsInApp(String appId, String userId, String email) {
		if (redisModel.userExistsInApp(appId, email))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.userExistsInApp(appId, email);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public boolean userExistsInApp(String appId, String userId) {
		if (redisModel.userExistsInApp(appId, redisModel.getEmailUsingUserId(appId, userId)))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.userExistsInApp(appId, mongoModel.getEmailUsingUserId(appId, userId));
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public String socialUserExistsInApp(String appId, String socialId, String socialNetwork) {
		if (redisModel.socialUserExistsInApp(appId, socialId, socialNetwork))
			return redisModel.getUserIdUsingSocialInfo(appId, socialId,socialNetwork);
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.getUserIdUsingSocialInfo(appId, socialId, socialNetwork);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public boolean identifierInUseByUserInApp(String appId, String userId) {
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.identifierInUseByUserInApp(appId, userId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public boolean confirmUsersEmailOption(String appId) {
		if(redisModel.appExists(appId))
			return redisModel.confirmUsersEmail(appId);
		else if(auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.confirmUsersEmail(appId);
		else{
			return false;
		}
	}

	public String getUrlUserId(String appId, String userId) {
		return this.emailOp.getUrlUserId(appId, userId);
	}

	public void removeUrlToUserId(String appId, String userId) {
		this.emailOp.removeUrlToUserId(appId, userId);
	}

	public void confirmUserEmail(String appId, String userId) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.confirmUserEmail(appId, userId);
			String email = redisModel.getEmailUsingUserId(appId, userId);
			if (redisModel.userExistsInApp(appId, email)) {
				redisModel.confirmUserEmail(appId, userId);
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public boolean userEmailIsConfirmed(String appId, String userId) {
		if (redisModel.userEmailIsConfirmed(appId, userId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.userEmailIsConfirmed(appId, userId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public boolean updateConfirmUsersEmailOption(String appId, Boolean confirmUsersEmail) {
		boolean sucess = false;
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			sucess = mongoModel.updateConfirmUsersEmailOption(appId, confirmUsersEmail);
			if (redisModel.appExists(appId)) {
				redisModel.updateConfirmUsersEmailOption(appId, confirmUsersEmail);
			}
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return opOk;
	}

	public String getRecoveryCode(String appId, String userId) {
		return this.emailOp.getRecoveryCodeOfUser(appId, userId);
	}

	private Map<String, String> getUserFields(String appId, String userId)throws UnsupportedEncodingException {
		Map<String, String> userFields = redisModel.getUser(appId, userId);
		String email = null;
		String creationDate = null;
		String userName = null;
		String socialId = null;
		String socialNetwork = null;
		
		byte[] hash = null;
		byte[] salt = null;
		String flag = null;
		
		if (userFields == null || userFields.size() == 0) {
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				userFields = mongoModel.getUser(appId, userId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : userFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("email"))
							email = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("userName"))
							userName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("hash"))
							hash = JSONSerializers.getStrict()
									.serialize(entry.getValue()).getBytes();
						else if (entry.getKey().equalsIgnoreCase("salt"))
							salt = JSONSerializers.getStrict()
									.serialize(entry.getValue()).getBytes();
						else if(entry.getKey().equalsIgnoreCase("flag"))
							flag = entry.getValue();
						else if(entry.getKey().equalsIgnoreCase("socialId"))
							socialId = entry.getValue();
						else if(entry.getKey().equalsIgnoreCase("socialNetwork"))
							socialNetwork = entry.getValue();
					}
					if(flag != null)
						redisModel.createUserWithFlag(appId, userId,userName, socialId, socialNetwork, email, salt, hash, creationDate, flag);
					else{
						redisModel.createUserWithoutFlag(appId, userId, userName, socialId, socialNetwork, email, salt, hash, creationDate);
					}
				} else {
					System.out.println("Warning: Cache is full.");
				}
			} else {
				System.out.println("Database not implemented.");
			}
		}
		return userFields;
	}

}
