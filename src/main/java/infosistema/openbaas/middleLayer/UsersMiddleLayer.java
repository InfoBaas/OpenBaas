package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.email.EmailInterface;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.models.Model;
import infosistema.openbaas.dataaccess.sessions.RedisSessions;
import infosistema.openbaas.dataaccess.sessions.SessionInterface;
import infosistema.openbaas.model.user.User;
import infosistema.openbaas.model.user.UserInterface;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

public class UsersMiddleLayer {

	// *** MEMBERS *** ///

	Model model;
	SessionInterface sessions;
	EmailInterface emailOp;
	private static final Utils utils = new Utils();
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
	public boolean createUser(String appId, String userId, String userName,
			String email, byte[] salt, byte[] hash, String userFile) {
		boolean sucess = false;
		try {
			sucess = this.model.createUser(appId, userId, userName, email, salt, hash, userFile);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return sucess;
	}

	public boolean createUserWithEmailConfirmation(String appId, String userId,String userName, 
			String email, byte[] salt, byte[] hash,	String flag, boolean emailConfirmed, UriInfo uriInfo) {
		boolean sucessModel = false;
		try {
			sucessModel = this.model.createUserWithEmailConfirmation(appId, userId, userName, email, salt, hash, flag, emailConfirmed);
			String ref = utils.getRandomString(Const.IDLENGTH);
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
	
	/**
	 * Horrible complexity, paginate this.
	 * 
	 * @param appId
	 * @param pageSize 
	 * @param pageNumber 
	 * @param orderType 
	 * @param orderBy 
	 * @return
	 */
	public ArrayList <String> getAllUserIdsForApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return model.getAllUserIdsForApp(appId,pageNumber,pageSize,orderBy,orderType);
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
