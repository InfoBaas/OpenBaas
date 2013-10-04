package resourceModelLayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import modelInterfaces.Application;
import modelInterfaces.Audio;
import modelInterfaces.Image;
import modelInterfaces.User;
import modelInterfaces.Video;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;

import rest_Models.DefaultApplication;
import rest_Models.DefaultUser;
import rest_Models.JPG;
import rest_Models.MP3;
import rest_Models.MPEG;
import rest_Models.PasswordEncryptionService;
import rest_Models.Storage;
import sessionsAndEmailConfirmations.EmailOperations;
import sessionsAndEmailConfirmations.EmailOperationsClass;
import sessionsAndEmailConfirmations.RedisSessions;
import sessionsAndEmailConfirmations.SessionDBInterface;
import Model.Model;

import com.sun.jersey.core.header.FormDataContentDisposition;

public class AppsMiddleLayer {

	// ****************SIMULATION OBJECT**********************
	// static Map<String, Application> apps;
	// *******************************************************
	private static final int IDLENGTH = 3;
	// ******************SINGLETON****************************
	Model model;
	SessionDBInterface sessions;
	EmailOperations emailOp;
	// *******************************************************
	private static PasswordEncryptionService service;

	private static final String AUDIOTYPE = "audio";
	private static final String VIDEOTYPE = "video";
	private static final String IMAGETYPE = "image";
	private static final String STORAGEFOLDER = "storage";
	private static final String MEDIAFOLDER = "media";
	private static final String IMAGESFOLDER = "/media/images";
	private static final String AUDIOFOlDER = "/media/audio";
	private static final String VIDEOFOLDER = "/media/video";
	private static String OPENBAASADMIN = "openbaasAdmin";
	
	public AppsMiddleLayer() {
		model = Model.getModel(); // SINGLETON
		// simulate();
		service = new PasswordEncryptionService();
		sessions = new RedisSessions();
		emailOp = new EmailOperationsClass();
	}

	/**
	 * returns true if created Application sucessfully.
	 * 
	 * @param appId
	 * @param appName
	 * @return
	 */
	public boolean createApp(String appId, String appName, boolean userEmailConfirmation) {
		return model.createApp(appId, appName, new Date().toString(), userEmailConfirmation);
	}

	public boolean appExists(String appId) {
		return model.appExists(appId);
	}

	public Application getApp(String appId) {
		Map<String, String> fields = model.getApplication(appId);
		Application temp = new DefaultApplication(appId);

		if (fields == null) {
			temp = null;
		} else {
			for (Entry<String, String> entry : fields.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("creationdate"))
					temp.setCreationDate(entry.getValue());
				else if (entry.getKey().equalsIgnoreCase("alive")
						&& entry.getValue().equalsIgnoreCase("false"))
					temp = null;
				else if (entry.getKey().equalsIgnoreCase("appName"))
					temp.setAppName(entry.getValue());
				else if (entry.getKey().equalsIgnoreCase("confirmUsersEmail"))
					temp.setConfirmUsersEmail(entry.getValue());
			}
		}
		return temp;
	}

	public boolean removeApp(String appId) {
		return this.model.deleteApp(appId);
	}

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

	public boolean identifierInUseByUserInApp(String appId, String userId) {
		return this.model.identifierInUseByUserInApp(appId, userId);
	}

	public boolean userExistsInApp(String appId, String userId, String email) {
		return this.model.userExistsInApp(appId, userId, email);
	}

	public User getUserInApp(String appId, String userId) {
		Map<String, String> userFields = null;
		try {
			userFields = this.model.getUserFields(appId, userId);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		User temp = new DefaultUser(userId);
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

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllAppIds(pageNumber, pageSize, orderBy, orderType);
	}

	public void updateAllAppFields(String appId, String alive, String newAppName, boolean confirmUsersEmail) {
		this.model.updateAllAppFields(appId, alive, newAppName, confirmUsersEmail);
	}

	public void updateUser(String appId, String userId, String email,
			byte[] hash, byte[] salt, String alive) {
		this.model.updateUser(appId, userId, email, hash, salt, alive);
	}

	public ArrayList<String> getAllAudioIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllAudioIds(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public boolean audioExistsInApp(String appId, String audioId) {
		return this.model.audioExistsInApp(appId, audioId);
	}

	public Audio getAudioInApp(String appId, String audioId) {
		Map<String, String> audioFields = this.model.getAudioInApp(appId, audioId);
		Audio temp = new MP3(audioId);
		for (Map.Entry<String, String> entry : audioFields.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("type"))
				temp.setAudioType(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("size"))
				temp.setSize(Long.parseLong(entry.getValue()));
			else if (entry.getKey().equalsIgnoreCase("defaultBitRate"))
				temp.setDefaultBitRate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("dir"))
				temp.setDir(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("creationDate"))
				temp.setCreationDate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("fileName")) 
				temp.setFileName(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("location"))
					temp.setLocation(entry.getValue());
		}
		return temp;
	}

	public void deleteAudioInApp(String appId, String audioId) {
		this.model.deleteAudio(appId, audioId);
	}

	public boolean deleteUserInApp(String appId, String userId) {
		return this.model.deleteUserInApp(appId, userId);
	}

	public byte[] downloadAudioInApp(String appId, String audioId,String ext) {
		return this.model.downloadAudioInApp(appId, audioId,ext);
	}
	
	public byte[] downloadImageInApp(String appId, String imageId,String ext) {
		return this.model.downloadImageInApp(appId, imageId,ext);
	}

	public boolean createAppAWS(String appId) {
		return this.model.createAppFoldersAWS(appId);

	}

	public String uploadAudioFileToServer(String appId, String fileDirectory, String location, String fileType,
			String fileName) {
		String audioId = this.getRandomString(IDLENGTH);
		if (this.model.uploadFileToServer(appId, audioId, MEDIAFOLDER,
				AUDIOTYPE, fileDirectory, location, fileType, fileName))
			return audioId;
		else {
			return null;
		}
	}

	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}

	public ArrayList<String> getAllImageIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllImageIdsInApp(appId, pageNumber, pageSize, orderBy, orderType);
	}

	public boolean uploadImageFileToServerWithGeoLocation(String appId, String location, String fileType,
			String fileName, String imageId) {
		boolean opOk  = false;
		if (this.model.uploadFileToServer(appId, imageId, MEDIAFOLDER,
				IMAGETYPE, "apps/"+appId+IMAGESFOLDER, location, fileType, fileName))
			opOk = true;
		return opOk;
	}
	public boolean uploadImageFileToServerWithoutGeoLocation(String appId, String fileType,
			String fileName, String imageId) {
		boolean opOk = false;
		if (this.model.uploadFileToServer(appId, imageId, MEDIAFOLDER,
				IMAGETYPE, "apps/"+appId+IMAGESFOLDER, null, fileType, fileName))
			opOk = true;
		return opOk;
	}
	public ArrayList<String> getAllVideoIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllVideoIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public boolean imageExistsInApp(String appId, String imageId) {
		return this.model.imageExistsInApp(appId, imageId);
	}

	public Image getImageInApp(String appId, String imageId) {
		Map<String, String> imageFields = this.model.getImageInApp(appId,
				imageId);
		Image temp = new JPG();
		for (Map.Entry<String, String> entry : imageFields.entrySet()) {
			if(entry.getKey().equalsIgnoreCase("location"))
				temp.setLocation(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("creationDate"))
				temp.setCreationDate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("dir"))
				temp.setDir(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("id"))
				temp.setId(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("size"))
				temp.setSize(Long.parseLong(entry.getValue()));
			else if (entry.getKey().equalsIgnoreCase("fileName")) 
				temp.setFileName(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("type"))
				temp.setImageType(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("id"))
				temp.setId(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("resolution"))
				temp.setResolution(entry.getValue());
			
		}
		return temp;
	}

	public boolean uploadVideoFileToServerWithoutGeoLocation(String appId, String videoId, String fileType,
			String fileName) {
		String dir = "apps/"+appId+VIDEOFOLDER;
		boolean opOk = false;
		if (this.model.uploadFileToServer(appId, videoId, MEDIAFOLDER,
				VIDEOTYPE,dir, null, fileType,fileName)) 
			opOk = true;
		return opOk;
	}
	public boolean uploadVideoFileToServerWithGeoLocation(String appId, String videoId, String fileType, String fileName,
			String location){
		String dir = "apps/"+appId+VIDEOFOLDER;
		boolean opOk = false;
		if(this.model.uploadFileToServer(appId, videoId, MEDIAFOLDER, 
				VIDEOFOLDER, dir, location, fileType, fileName))
			opOk = true;
		return opOk;
	}
	public ArrayList<String> getAllStorageIdsInApp(String appId,Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllStorageIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public String uploadStorageFileToServer(String appId, String fileIdentfier, String fileType, String fileName) {
		if (this.model.uploadFileToServer(appId, fileIdentfier, STORAGEFOLDER,
				null, "apps/"+appId+"/storage", null, fileType, fileName))
			return fileIdentfier;
		else
			return null;
	}
	public String uploadStorageFileToServer(String appId, String fileIdentfier, String fileType, String fileName, String location) {
		if (this.model.uploadFileToServer(appId, fileIdentfier, STORAGEFOLDER,
				null, "apps/"+appId+"/storage", location, fileType, fileName))
			return fileIdentfier;
		else
			return null;
	}
	public boolean videoExistsInApp(String appId, String videoId) {
		return this.model.videoExistsInApp(appId, videoId);
	}

	public void deleteVideoInApp(String appId, String videoId) {
		this.model.deleteVideoInApp(appId, videoId, MEDIAFOLDER, VIDEOTYPE);
	}

	public byte[] downloadVideoInApp(String appId, String videoId,String ext) {
		return this.model.downloadVideoInApp(appId, videoId,ext);
	}

	public Video getVideoInApp(String appId, String videoId) {
		Map<String, String> imageFields = this.model.getVideoInApp(appId,
				videoId);
		Video temp = new MPEG();
		
		for (Map.Entry<String, String> entry : imageFields.entrySet()) {
			if(entry.getKey().equalsIgnoreCase("location"))
				temp.setLocation(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("creationDate"))
				temp.setCreationDate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("dir"))
				temp.setDir(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("id"))
				temp.setId(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("size"))
				temp.setSize(Long.parseLong(entry.getValue()));
			else if (entry.getKey().equalsIgnoreCase("fileName")) 
				temp.setFileName(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("type"))
				temp.setType(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("resolution"))
				temp.setResolution(entry.getValue());
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

	public boolean sessionTokenExistsForUser(String sessionToken, String userId) {
		return sessions.sessionTokenExistsForUser(sessionToken, userId);
	}

	public void deleteSessionForUser(String sessionToken, String userId) {
		sessions.deleteUserSession(sessionToken, userId);
	}
	public boolean adminExists(String OPENBAASADMIN) {
		boolean adminExists = false;
		adminExists = sessions.adminExists(OPENBAASADMIN);
		if (!adminExists)
			adminExists = sessions.adminExists(OPENBAASADMIN);
		return adminExists;
	}
	public void createAdmin(String OPENBAASADMIN, byte[] adminSalt,
			byte[] adminHash) throws UnsupportedEncodingException {
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
	public void reviveApp(String appId){
		this.model.reviveApp(appId);
	}
	public Map<String, String> getAdminFields(String OPENBAASADMIN)
			throws UnsupportedEncodingException {
		Map<String, String> adminFields = sessions.getAdminFields(OPENBAASADMIN);
		return adminFields;
	}
	public void updateAppName(String appId, String newAppName) {
		model.updateAppName(appId, newAppName);
	}

	public void updateUser(String appId, String userId, String email) {
		model.updateUser(appId, userId, email);
	}

	public void updateUser(String appId, String userId, String email,
			byte[] hash, byte[] salt) {
		model.updateUser(appId, userId, email, hash, salt);
		
	}

	public boolean deleteUserSession(String sessionToken, String userId) {
		return sessions.deleteUserSession(sessionToken, userId);
	}

	public boolean deleteAllUserSessions(String userId) {
		return sessions.deleteAllUserSessions(userId);
	}
	public boolean sessionExistsForUser(String userId) {
		return sessions.sessionExistsForUser(userId);
	}

	public void createDocumentForApplication(String appId) {
		model.createDocumentForApplication(appId);
		
	}

	public boolean insertIntoAppDocument(String appId, String url, JSONObject data, String location) {
		return model.insertIntoAppDocument(appId, url, data, location);
	}

	public String getElementInAppDocument(String appId, List<PathSegment> path) {
		String url = createAppDocPathFromListWithComas(appId, path);
		return model.getElementInDocument(appId, url);
	}
	public String getElementInUserDocument(String appId, String userId, List<PathSegment> path){
		String url = createUserDocPathFromListWithComas(appId, userId, path);
		return model.getElementInUserDocument(appId, userId, url);
	}
	public boolean dataExistsForElement(String appId, List<PathSegment> path) {
		String url = createAppDocPathFromListWithComas(appId, path);
		return model.dataExistsForElement(appId, url);
	}
	public boolean dataExistsForUserElement(String appId, String userId,
			List<PathSegment> path) {
		String url = createUserDocPathFromListWithComas(appId, userId, path);
		return model.dataExistsForElement(appId, url);
		
	}
	public boolean elementExistsInDocument(String appId, String url) {
		String [] path = url.split("/");
		StringBuilder tempPath = new StringBuilder();
		for(int i = 0; i < path.length; i++)
			tempPath.append(path[i] + ",");
		tempPath.deleteCharAt(tempPath.length()-1); //delete last comma
		return model.elementExistsInDocument(appId, tempPath.toString());
	}
	public String createAppDocPathFromListWithComas(String appId, List<PathSegment> path){
		StringBuilder sb = new StringBuilder();
		sb.append(appId + ",");
		for(int i = 0; i < path.size(); i++)
			sb.append(path.get(i).getPath()).append(',');
		sb.deleteCharAt(sb.length()-1); //delete last comma
		return sb.toString();
	}
	public String createUserDocPathFromListWithComas(String appId,
			String userId, List<PathSegment> path) {
		StringBuilder sb = new StringBuilder();
		sb.append(appId + "," + "userId" + ",");
		for(int i = 0; i < path.size(); i++)
			sb.append(path.get(i).getPath()).append(',');
		sb.deleteCharAt(sb.length()-1); //delete last comma
		return sb.toString();
	}
	public String convertDocPathWithSlashestoComas(String appId, String url){
		StringBuilder sb = new StringBuilder();
		String [] array = url.split("/");
		for(int i = 0; i < array.length; i++){
			sb.append(array[i]).append(",");
		}
		sb.deleteCharAt(sb.length()-1); //delete last comma
		return sb.toString();
	}
	public String createAppDocPathFromListWithSlashes(String appId,
			List<PathSegment> path) {
		StringBuilder sb = new StringBuilder();
		sb.append(appId + "/");
		for(int i = 0; i < path.size(); i++)
			sb.append(path.get(i).getPath()).append('/');

		sb.deleteCharAt(sb.length()-1); //delete last comma
		return sb.toString();
	}

	public String createUserDocPathFromListWithSlashes(String appId,
			String userId, List<PathSegment> path) {
		StringBuilder sb = new StringBuilder();
		sb.append(appId + "/" + userId + "/");
		for(int i = 0; i < path.size(); i++)
			sb.append(path.get(i).getPath()).append('/');

		sb.deleteCharAt(sb.length()-1); //delete last comma
		return sb.toString();
	}
	public boolean updateDataInDocument(String appId, String url, String data) {
		String path = convertDocPathWithSlashestoComas(appId, url);
		return model.updateDataInDocument(appId, path, data);
	}

	public boolean deleteDataInElement(String appId, List<PathSegment> path) {
		String url = createAppDocPathFromListWithComas(appId, path);
		return model.deleteDataInElement(appId, url);
	}
	public boolean deleteUserDataInElement(String appId, String userId, List<PathSegment> path) {
		String url = createUserDocPathFromListWithComas(appId, userId, path);
		return model.deleteDataInElement(appId, url);
	}
	public String patchDataInElement(String appId, List<PathSegment> path,
			JSONObject inputJson, String location) {
		String url = createAppDocPathFromListWithComas(appId, path);
		return model.patchDataInElement(url, inputJson, location);
	}

	public boolean insertAppDocumentRoot(String appId, JSONObject data, String location) {
		return model.insertDocumentRoot(appId, data, location);
	}

	public String getAllDocInApp(String appId) {
		return model.getAllDocInApp(appId);
	}

	public ArrayList<String> getAllMediaIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return model.getAllMediaIds(appId, pageNumber, pageSize, orderBy, orderType);
	}

	public boolean createNonPublishableAppDocument(String appId, JSONObject data, 
			List<PathSegment> path, String location) {
		String url = createAppDocPathFromListWithComas(appId, path);
		return model.createNonPublishableDocument(appId, data, url, location);
	}

	public boolean insertIntoUserDocument(String appId, String userId, String url,
			JSONObject data, String location) {
		return model.insertIntoUserDocument(appId, userId, url, data, location);
	}

	public boolean insertUserDocumentRoot(String appId, String userId,
			JSONObject data, String location) {
		return model.insertUserDocumentRoot(appId, userId, data, location);
	}

	public boolean createNonPublishableUserDocument(String appId,
			String userId, JSONObject data, String url, String location) {
		return model.createNonPublishableUserDocument(appId, userId, data, url, location);
	}

	public void refreshSession(String sessionToken) {
		sessions.refreshSession(sessionToken, new Date().toString());
	}

	public void addLocationToSession(String location, String sessionToken, String userAgent) {
		sessions.addLocationToSession(location, sessionToken, userAgent);
	}

	public boolean refreshSession(String sessionToken, String location, String userAgent) {
		return sessions.refreshSession(sessionToken, location, new Date().toString(), userAgent);
	}

	public String getUserUsingSessionToken(String sessionToken) {
		return sessions.getUserUsingSessionToken(sessionToken);
	}

	public boolean sessionTokenExists(String sessionToken) {
		return sessions.sessionTokenExists(sessionToken);
	}

	public boolean storageExistsInApp(String appId, String storageId) {
		return this.model.storageExistsInApp(appId, storageId);
	}

	public Storage getStorageInApp(String appId, String storageId) {
		Map<String, String> storageFields = this.model.getStorageInApp(appId, storageId);
		Storage temp = new Storage();
		for (Map.Entry<String, String> entry : storageFields.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("id"))
				temp.setId(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("size"))
				temp.setSize(Long.parseLong(entry.getValue()));
			else if (entry.getKey().equalsIgnoreCase("dir"))
				temp.setDir(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("fileName")) 
				temp.setFileName(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("location"))
					temp.setLocation(entry.getValue());
		}
		return temp;
	}

	public ArrayList<String> getAllDocsInRadius(String appId, double latitude,
			double longitude, double radius) {
		return model.getAllDocsInRadius(appId, latitude, longitude, radius);
	}

	public ArrayList<String> getElementInAppInRadius(String appId, List<PathSegment> path, double latitude,
			double longitude, double radius) {
		String url = createAppDocPathFromListWithComas(appId, path);
		return model.getElementInDocumentInRadius(appId, url, latitude, longitude, radius);
	}

	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId, double latitude,
			double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return model.getAllUserDocsInRadius(appId, userId, latitude, longitude, radius, pageNumber,pageSize,orderBy,orderType);
	}

	public String getAllUserDocs(String appId, String userId) {
		return model.getAllUserDocs(appId, userId);
	}

	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude,
			double longitude, double radius) {
		return model.getAllAudioIdsInRadius(appId, latitude, longitude, radius);
	}
	
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,
			double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return model.getAllImagesIdsInRadius(appId, latitude, longitude, radius,pageNumber,pageSize,orderBy,orderType);
	}

	public String createLocalFile(InputStream uploadedInputStream,FormDataContentDisposition fileDetail, 
			String appId, String extension, String dir) {
		String id = this.getRandomString(IDLENGTH);
		File dirFolders = new File(dir);
		dirFolders.mkdirs();
		File f = new File(dir + id + "." + extension);
		while (f.exists()) {
			id = this.getRandomString(IDLENGTH);
			f = new File(dir + id);
		}
		OutputStream out;
		try {
			out = new FileOutputStream(f);
			IOUtils.copy(uploadedInputStream, out);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return id;
	}
	public String createLocalFile2(InputStream uploadedInputStream,FormDataContentDisposition fileDetail, 
			String appId, String extension, String dir, String imageId) {
		String id = this.getRandomString(IDLENGTH);
		id = imageId;
		File dirFolders = new File(dir);
		dirFolders.mkdirs();
		File f = new File(dir + id + "." + extension);
		while (f.exists()) {
			id = this.getRandomString(IDLENGTH);
			id = imageId;
			f = new File(dir + id);
		}
		OutputStream out;
		try {
			out = new FileOutputStream(f);
			IOUtils.copy(uploadedInputStream, out);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return id;
	}

	public byte[] downloadStorageInApp(String appId, String storageId,String ext) {
		return this.model.downloadStorageInApp(appId, storageId,ext);
	}

	public void deleteStorageInApp(String appId, String storageId) {
		this.model.deleteStorageFile(appId, storageId);
	}

	public void deleteImageInApp(String appId, String imageId) {
		this.model.deleteImageInApp(appId, imageId);
	}

	public boolean confirmUsersEmailOption(String appId) {
		return this.model.confirmUsersEmailOption(appId);
	}

	public boolean createUserWithEmailConfirmation(String appId, String userId,String userName, 
			String email, byte[] salt, byte[] hash,	String flag, boolean emailConfirmed, UriInfo uriInfo) {
		boolean sucessModel = false;
		boolean sucessIdEmail = false;
		try {
			sucessModel = this.model.createUserWithEmailConfirmation(appId, userId, userName, email, salt, hash, flag, emailConfirmed);
			String ref = this.getRandomString(IDLENGTH);
			emailOp.sendRegistrationEmailWithRegistrationCode(appId, userId, userName, email, ref, uriInfo.getAbsolutePath().toASCIIString());
			sucessIdEmail = this.emailOp.addUrlToUserId(appId, userId, ref);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sucessModel;
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

	public boolean userExistsInApp(String appId, String userId) {
		return this.model.userExistsInApp(appId, userId);
	}

	
}
