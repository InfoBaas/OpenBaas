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
package infosistema.openbaas.middleLayer;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.models.ModelAbstract;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

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

	public Result createUserAndLogin(MultivaluedMap<String, String> headerParams, UriInfo uriInfo, String appId,
			String userName, String email, String password, String userFile, Boolean baseLocationOption,
			String baseLocation, Map<String, String> extraMetadata) {
		User outUser = new User();
		Result res = null;
		
		if (baseLocationOption == null) baseLocationOption = false;
		String userId = null;
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
		try {
			location = headerParams.getFirst(Const.LOCATION);
		} catch (Exception e) { }
		try {
			userAgent = headerParams.getFirst(Const.USER_AGENT);
		} catch (Exception e) { }
		if(baseLocationOption)
			if(location!=null)
				location = baseLocation;
		if (!getConfirmUsersEmailOption(appId)) {
			SessionMiddleLayer sessionMid = SessionMiddleLayer.getInstance();
			res = createUser(appId, userId, userName, "NOK", "SocialNetwork", email, salt, hash, userFile, null, null,
					baseLocationOption, baseLocation, location, extraMetadata);
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, password);
			
			Boolean refresh = sessionMid.refreshSession(sessionToken, location, userAgent);
			lastLocation = updateUserLocation(userId, appId, location, extraMetadata);
			if(lastLocation==null)
				lastLocation = outUser.getLocation();
			if (validation && refresh) {
				outUser.set_id(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setEmail(email);
				outUser.setUserName(userName);
				outUser.setUserFile(userFile);
				outUser.setBaseLocation(baseLocation);
				outUser.setBaseLocationOption(baseLocationOption.toString());
				outUser.setLocation(lastLocation);
				outUser.setOnline("true");
			}
		} else if (getConfirmUsersEmailOption(appId)) {
			boolean emailConfirmed = false;
			res = createUser(appId, userId, userName, "NOK", "SocialNetwork", email, salt,hash, userFile, emailConfirmed,
					uriInfo, baseLocationOption, baseLocation, location, extraMetadata);
			outUser.set_id(userId);
			outUser.setEmail(email);
			outUser.setUserName(userName);
			outUser.setUserFile(userFile);
			outUser.setBaseLocation(baseLocation);
			outUser.setBaseLocationOption(baseLocationOption.toString());
			outUser.setLocation(location);
			outUser.setOnline("true");
		}
		return new Result(outUser, res.getMetadata());
		
	}
	
	public Result createSocialUserAndLogin(MultivaluedMap<String, String> headerParams, String appId, 
			String userName, String email, String socialId, String socialNetwork, Map<String, String> extraMetadata) {
		User outUser = new User();
		String userId = null;
		String userAgent = null;
		String location = null;
		String lastLocation =null;
		Result res = null; 
		
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
		res = createUser(appId, userId, userName, socialId, socialNetwork, email, salt, hash, null, null, null, false, null, null, extraMetadata);
		String sessionToken = Utils.getRandomString(Const.getIdLength());
		boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
		try {
			location = headerParams.getFirst(Const.LOCATION);
		} catch (Exception e) { }
		try {
			userAgent = headerParams.getFirst(Const.USER_AGENT);
		} catch (Exception e) { }
		
		sessionMid.refreshSession(sessionToken, location, userAgent);
		lastLocation = updateUserLocation(userId, appId, location, extraMetadata);
		if(lastLocation==null)
			lastLocation = outUser.getLocation();

		if (validation) {
			outUser.set_id(userId);
			outUser.setEmail(email);
			outUser.setUserName(userName);
			outUser.setBaseLocationOption("false");
			outUser.setLocation(lastLocation);
			outUser.setReturnToken(sessionToken);
			outUser.setOnline("true");
		}
		return new Result(outUser,res.getMetadata());
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
	public Result createUser(String appId, String userId, String userName, String socialId, String socialNetwork,
			String email, byte[] salt, byte[] hash,	String userFile, Boolean emailConfirmed, UriInfo uriInfo,
			Boolean baseLocationOption, String baseLocation, String location, Map<String, String> extraMetadata) {
		try {
			Metadata metadata = null;
			Object data = null;
			Map<String, String> fields = getUserFields(userName, socialId, socialNetwork, email, userFile, salt, hash, emailConfirmed,
					baseLocationOption, baseLocation, true, location);
			data = userModel.createUser(appId, userId, fields, extraMetadata);			
			metadata = Metadata.getMetadata(new JSONObject(((JSONObject) data).getString(ModelAbstract._METADATA)));
			
			((JSONObject) data).remove(ModelAbstract._METADATA);
			data = (DBObject)JSON.parse(data.toString());
			String ref = Utils.getRandomString(Const.getIdLength());
			if (uriInfo != null) {
				emailOp.sendRegistrationEmailWithRegistrationCode(appId, userId, userName, email, ref, uriInfo.getAbsolutePath().toASCIIString());
			}
			this.emailOp.addUrlToUserId(appId, userId, ref);
			return new Result(data, metadata);
		} catch (Exception e) {
			Log.error("", this, "createUser", "An error ocorred.", e); 
		}
		return null;
	}

	// *** UPDATE *** //
	public Result updateUser(String appId, String userId, String userName, String email, String userFile,
			Boolean baseLocationOption, String baseLocation, String location, Map<String, String> extraMetadata) {
		Metadata metadata = null;
		Object data = null;
		try {
			Map<String, String> fields = getUserFields(userName, null, null, email, userFile, null, null, null, baseLocationOption, baseLocation, true, location);
			data = userModel.updateUser(appId, userId, fields, extraMetadata);
			metadata = Metadata.getMetadata(new JSONObject(((JSONObject) data).getString(ModelAbstract._METADATA)));
			((JSONObject) data).remove(ModelAbstract._METADATA);
			data = (DBObject)JSON.parse(data.toString());
		} catch (Exception e) {
			Log.error("", this, "updateUser", "updateUser.", e); 
			return null;
		}
		return new Result(data, metadata);
	}
	
	public Result updateUserPassword(String appId, String userId, String password, Map<String, String> extraMetadata) {
		byte[] salt = null;
		byte [] hash = null;
		PasswordEncryptionService service = new PasswordEncryptionService();
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(password, salt);
			if (appModel.appExists(appId) && userModel.userIdExists(appId, userId)) {
				Metadata metadata = null;
				Object data = null;
				Map<String, String> fields = getUserFields(null, null, null, null, null, salt, hash, null, null, null, null, null);
				data = userModel.updateUser(appId, userId, fields, extraMetadata);
				metadata = Metadata.getMetadata(new JSONObject(((JSONObject) data).getString(ModelAbstract._METADATA)));
				((JSONObject) data).remove(ModelAbstract._METADATA);
				data = (DBObject)JSON.parse(data.toString());
				return new Result(data, metadata);
			}
		} catch (Exception e) {
			Log.error("", this, "updateUserPassword", "Unsupported Encoding.", e); 
		}
		return null;
	}

	// *** DELETE *** //
	
	public boolean deleteUserInApp(String appId, String userId) {
		boolean operationOk = false;
		try {
			Map<String, String> fields = getUserFields(null, null, null, null, null, null, null, null, null, null, false, null);
			operationOk = userModel.updateUser(appId, userId, fields, null) != null;
		} catch (Exception e) {
			Log.error("", this, "deleteUserInApp", "deleteUserInApp.", e); 
		}
		return operationOk;
	}

	// *** GET LIST *** //

	@Override
	protected List<DBObject> getAllSearchResults(String appId, String userId, String url, Double latitude, Double longitude, 
			Double radius, JSONObject query, String orderType, String orderBy, ModelEnum type, List<String> toShow) throws Exception {
		List<DBObject> result = null;
		SessionModel sessionModel = new SessionModel();
		Boolean online = false;
		if (query == null || query.length() == 0) {
			query = new JSONObject();
			JSONObject jAux = new JSONObject();
			jAux.put("$exists",1);
			query.put(User.EMAIL, jAux); 
			query.put(User.HASH, jAux);
			query.put(User.SALT, jAux); 
		}
		result = docModel.getDocuments(appId, userId, url, latitude, longitude, radius, query, orderType, orderBy, toShow);
		if(toShow.contains(User.ONLINE)){
			Iterator<DBObject> it = result.iterator();
			while(it.hasNext()){
				DBObject dbo = it.next();
				String _id = (String) dbo.get(User._ID);
				online = sessionModel.isUserOnline(_id);
				((DBObject) dbo.get(User.DATA)).put("online", online.toString());
			}
		}
		return result; 
	}

	
	// *** GET *** //
	
	public Result getUserInApp(String appId, String userId) {
		
		JSONObject user = userModel.getUser(appId, userId, true);
		if (user == null) return null;
		User data = new User(userId);
		try {
			if(user.has(User.USER_NAME))
				data.setUserName(user.getString(User.USER_NAME));
			if(user.has(User.USER_FILE))
				data.setUserFile(user.getString(User.USER_FILE));
			if(user.has(User.EMAIL))
				data.setEmail(user.getString(User.EMAIL));
			if(user.has(User.ALIVE))
				data.setAlive(user.getString(User.ALIVE));
			if(user.has(User.EMAIL_CONFIRMED))
				data.setEmailConfirmed(user.getString(User.EMAIL_CONFIRMED));
			if(user.has(User.BASE_LOCATION_OPTION))
				data.setBaseLocationOption(user.getString(User.BASE_LOCATION_OPTION));
			if(user.has(User.BASE_LOCATION))
				data.setBaseLocation(user.getString(User.BASE_LOCATION));
			if(user.has(User.LOCATION))
				data.setLocation(user.getString(User.LOCATION));
			if(user.has(User.ONLINE))
				data.setOnline(user.getString(User.ONLINE));
			Metadata metadata = null;
			if (user.has(ModelAbstract._METADATA))
				metadata = Metadata.getMetadata(new JSONObject(user.getString(ModelAbstract._METADATA)));
			return new Result(data, metadata);
		} catch (Exception e) {
			Log.error("", this, "getUserInApp", "An error ocorred.", e);
		}

		return null;
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
	
	public Result getUserUsingEmail(String appId, String email) {
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

	public void confirmUserEmail(String appId, String userId, Map<String, String> extraMetadata) {
		try {
			Map<String, String> fields = getUserFields(null, null, null, null, null, null, null, true, null, null, null, null);
			userModel.updateUser(appId, userId, fields, extraMetadata); 
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
			appModel.updateAppFields(appId, null, null, confirmUsersEmail, null, null,null, null,null);
			sucess = true;
		}
		return sucess;
	}

	public boolean recoverUser(String appId, String userId, String email, UriInfo uriInfo, String newPass,
			byte[] hash, byte[] salt, Map<String, String> extraMetadata) {
		boolean opOk = false;
		try {
			JSONObject user = userModel.getUser(appId, userId, false);
			String dbEmail = null;
			String userName = null;
				dbEmail = user.getString(User.EMAIL);
			userName = user.getString(User.USER_NAME);
			if (email != null && newPass != null) {
				try {
					Map<String, String> fields = getUserFields(null, null, null, email, null, salt, hash, null, null, null, null, null);
					userModel.updateUser(appId, userId, fields, extraMetadata);
				} catch (UnsupportedEncodingException e) {
					Log.error("", this, "updateUser", "Unsupported Encoding.", e); 
				}
			}
			if(dbEmail.equalsIgnoreCase(email)){
				boolean emailOk =emailOp.sendRecoveryEmail(appId, userName, userId, email, newPass, uriInfo.getAbsolutePath().toASCIIString());
				if(emailOk) {
					opOk = true;
				}
			}
		} catch (JSONException e) {
			Log.error("", this, "recoverUser", "An error ocorred.", e);
		}
		return opOk;
	}

	public String getRecoveryCode(String appId, String userId) {
		return this.emailOp.getRecoveryCodeOfUser(appId, userId);
	}

	public String updateUserLocation(String userId, String appId, String location, Map<String, String> extraMetadata) {
		try{
			User user = (User)getUserInApp(appId,userId).getData();
			if ("true".equalsIgnoreCase(user.getBaseLocationOption())) {
				location = user.getBaseLocation();
			}
			if(location!=null){
				Map<String, String> fields = getUserFields(null, null, null, null, null, null, null, null, null, null, null, location);
				userModel.updateUser(appId, userId, fields, extraMetadata);
			}
		}catch(Exception e){
			Log.error("", this, "updateUserLocation", "updateUserLocation exception.", e); 
		}
		return location;
	}

}
