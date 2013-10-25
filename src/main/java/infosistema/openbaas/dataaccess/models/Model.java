package infosistema.openbaas.dataaccess.models;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.jettison.json.JSONObject;


//*************Singleton, takes care of Filesystem + database
public class Model {

	private static Model ref;
	private static DataModel dataModel;
	private static FileSystemModel fileModel;
	// request types
	private static final String AUDIO = "audio";
	private static final String IMAGES = "image";
	private static final String VIDEO = "video";

	// Request folders
	private static final String MEDIAFOLDER = "media";
	private static final String STORAGEFOLDER = "storage";

	// File stuff
	private static final String DEFAULTIMAGEFORMAT = ".jpg";
	private static final String DEFAULTVIDEOFORMAT = ".mpg";
	private static final String DEFAULTAUDIOFORMAT = ".mp3";

	// VIDEO RESOLUTIONS
	private static final String SMALLRESOLUTION = "360p";

	// Image Sizes
	private static final String SMALLIMAGE = "300x300";

	// AUDIO BITRATES
	private static final String MINIMUMBITRATE = "32";

	private Model() {
		dataModel = new DataModel();
		fileModel = new FileSystemModel();
	}

	public static Model getModel() {
		if (ref == null)
			ref = new Model();
		return ref;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public boolean createApp(String appId, String appName, String creationDate, boolean userEmailConfirmation) {
		return dataModel.createApp(appId, appName, creationDate, userEmailConfirmation);
	}

	public boolean appExists(String appId) {
		return dataModel.appExists(appId);
	}

	public boolean userExistsInApp(String appId, String userId, String email) {
		return dataModel.userExistsInApp(appId, userId, email);
	}
	
	public String socialUserExistsInApp(String appId, String socialId, String socialNetwork) {
		return dataModel.socialUserExistsInApp(appId, socialId, socialNetwork);
	}

	public boolean identifierInUseByUserInApp(String appId, String userId) {
		return dataModel.identifierInUseByUserInApp(appId, userId);
	}

	public Map<String, String> getApplication(String appId) {
		return dataModel.getApplication(appId);
	}

	public boolean deleteApp(String appId) {
		return dataModel.deleteApp(appId);
	}

	public boolean createUser(String appId, String userId, String userName,	String socialId, String socialNetwork, String email, byte[] salt,
			byte[] hash, String userFile) throws UnsupportedEncodingException {
		boolean databaseOK = false;
		if(userFile != null) 
			databaseOK = dataModel.createUserWithFlag(appId, userId, userName, socialId, socialNetwork,
				email, salt, hash, userFile);
		else 
			databaseOK = dataModel.createUserWithoutFlag(appId, userId, userName, socialId, socialNetwork,
					email, salt, hash);
		//boolean awsOK = fileModel.createUser(appId, userId, userName);
		if (databaseOK /*&& awsOK*/)
			return true;
		return false;
	}

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllAppIds(pageNumber, pageSize, orderBy, orderType);
	}

	public Map<String, String> getUserFields(String appId, String userId)throws UnsupportedEncodingException {
		return dataModel.getUser(appId, userId);
	}

	public ArrayList<String> getAllUserIdsForApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllUserIdsForApp(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public void updateAllAppFields(String appId, String alive, String newAppName, boolean confirmUsersEmail) {
		dataModel.updateAllAppFields(appId, alive, newAppName, confirmUsersEmail);
	}

	public void updateUser(String appId, String userId, String email, byte[] hash, byte[] salt, String alive) {
		dataModel.updateUser(appId, userId, email, hash, salt, alive);
	}

	public ArrayList<String> getAllAudioIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllAudioIds(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public boolean audioExistsInApp(String appId, String audioId) {
		return dataModel.audioExistsInApp(appId, audioId);
	}

	public Map<String, String> getAudioInApp(String appId, String audioId) {
		return dataModel.getAudioInApp(appId, audioId);
	}

	public void deleteAudio(String appId, String audioId) {
		String fileDirectory = dataModel.getFileDirectory(appId, audioId, MEDIAFOLDER, AUDIO);
		fileModel.deleteFile(fileDirectory);
		dataModel.deleteAudioInApp(appId, audioId);
	}
	public void deleteStorageFile(String appId, String storageId){
		String fileDirectory = dataModel.getFileDirectory(appId, storageId, STORAGEFOLDER, null);
		fileModel.deleteFile(fileDirectory);
		dataModel.deleteStorageInApp(appId, storageId);
	}
	public boolean deleteUserInApp(String appId, String userId) {
		Model.fileModel.deleteUser(appId, userId);
		return dataModel.deleteUserInApp(appId, userId);
	}

	public byte[] downloadAudioInApp(String appId, String audioId,String ext) {
		return fileModel.download(appId, MEDIAFOLDER, AUDIO, audioId,ext);
	}

	public byte[] downloadVideoInApp(String appId, String videoId,String ext) {
		return fileModel.download(appId, MEDIAFOLDER, VIDEO, videoId,ext);
	}

	public byte[] downloadImageInApp(String appId, String imageId,String ext) {
		return fileModel.download(appId, MEDIAFOLDER, IMAGES, imageId,ext);
	}
	
	public Integer countAllImagesInApp(String appId) {
		return dataModel.countAllImagesInApp(appId);
	}

	public boolean createAppFoldersAWS(String appId) {
		return fileModel.createAppAWS(appId);
	}

	public boolean uploadFileToServer(String appId, String id, String folderType, String requestType, 
			String fileDirectory, String location, String fileType, String fileName) {
		boolean upload = false;
		boolean databaseOk = false;
		String fileFormat = "";
		String fileExtension = "";
		String fileSize = "";
		if (folderType.equalsIgnoreCase(MEDIAFOLDER)) {
			if (requestType.equalsIgnoreCase(AUDIO))
				fileFormat = DEFAULTAUDIOFORMAT;
			else if (requestType.equalsIgnoreCase(IMAGES))
				fileFormat = DEFAULTIMAGEFORMAT;
			else if (requestType.equalsIgnoreCase(VIDEO))
				fileFormat = DEFAULTVIDEOFORMAT;
			fileDirectory += "/"+ id+"."+fileType;
			String destinationDirectory = "apps/" + appId + "/" + folderType
					+ "/" + requestType + "/" + id + fileFormat;
			File fileToUpload = new File(fileDirectory);
			upload = fileModel.upload(appId, destinationDirectory, id, fileToUpload);
			databaseOk = false;
			
			if (upload) {
				fileExtension = FilenameUtils.getExtension(fileDirectory);
				fileName = FilenameUtils.getBaseName(fileDirectory);
				fileSize = "" + fileToUpload.length();
				if (requestType.equalsIgnoreCase("audio"))
					databaseOk = dataModel.createAudioInApp(appId, id,
							destinationDirectory, fileExtension, fileSize,
							MINIMUMBITRATE, new Date().toString(), fileName, location);
				else if (requestType.equalsIgnoreCase("image"))
					databaseOk = dataModel.createImageInApp(appId, id,
							destinationDirectory, fileExtension, fileSize,
							SMALLIMAGE, new Date().toString(), fileName, location);
				else if (requestType.equalsIgnoreCase("video"))
					databaseOk = dataModel.createVideoInApp(appId, id,
							destinationDirectory, fileExtension, fileSize,
							SMALLRESOLUTION, new Date().toString(), fileName, location);
			}
		} else if (folderType.equalsIgnoreCase(STORAGEFOLDER)) {
			fileDirectory += "/"+ id+"."+fileType;
			fileFormat = FilenameUtils.getExtension(fileDirectory);
			String destinationDirectory = "apps/" + appId + "/" + folderType + "/" + id + "." + fileFormat;
			System.out.println(destinationDirectory);
			File fileToUpload = new File(fileDirectory);
			upload = fileModel.upload(appId, destinationDirectory, id,	fileToUpload);
			if (upload) {
				fileExtension = FilenameUtils.getExtension(fileDirectory);
				fileSize = "" + fileToUpload.length();
				databaseOk = dataModel.createStorageInApp(appId, id, destinationDirectory, fileExtension, 
						fileSize, new Date().toString(), fileName, location);
			}
		}
		// Finalizing
		if (upload && databaseOk)
			return true;
		return false;
	}

	public ArrayList<String> getAllImageIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllImageIdsInApp(appId, pageNumber, pageSize, orderBy, orderType);
	}

	public ArrayList<String> getAllVideoIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllVideoIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public boolean imageExistsInApp(String appId, String imageId) {
		return dataModel.imageExistsInApp(appId, imageId);
	}

	public Map<String, String> getImageInApp(String appId, String imageId) {
		return dataModel.getImageInApp(appId, imageId);
	}

	public ArrayList<String> getAllStorageIdsInApp(String appId,Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllStorageIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
	}

	// public void createSession(String sessionId, String appId, String userId)
	// {
	// dataModel.createSession(sessionId, appId, userId);
	// }

	public boolean videoExistsInApp(String appId, String videoId) {
		return dataModel.videoExistsInApp(appId, videoId);
	}

	public void deleteVideoInApp(String appId, String id, String folderType,
			String requestType) {
		String dir = dataModel.getFileDirectory(appId, id, folderType,
				requestType);
		dataModel.deleteVideoInApp(appId, id);
		fileModel.deleteFile(dir);
	}

	public void deleteFileInApp(String appId, String id, String folderType,
			String requestType) {
		String fileDirectory = dataModel.getFileDirectory(appId, id,
				folderType, requestType);
		fileModel.deleteFile(fileDirectory);

	}

	public Map<String, String> getVideoInApp(String appId, String videoId) {
		return dataModel.getVideoInApp(appId, videoId);
	}

	public String getEmailUsingUserName(String appId, String userName) {
		return dataModel.getEmailUsingUserName(appId, userName);
	}

	public String getUserIdUsingUserName(String appId, String userName) {
		return dataModel.getUserIdUsingUserName(appId, userName);
	}
	
	public String getUserIdUsingEmail(String appId, String email) {
		return dataModel.getUserIdUsingEmail(appId, email);
	}

	// public boolean sessionIdExists(String sessionId) {
	// return dataModel.sessionIdExists(sessionId);
	// }
	// public boolean sessionExistsForUser(String userId) {
	// return dataModel.sessionExistsForUser(userId);
	// }
	//
	// public void deleteSessionForUser(String adminId) {
	// dataModel.deleteSession(adminId);
	// }
	// public void createAdminSession(String sessionId, String adminId){
	// dataModel.createAdminSession(sessionId, adminId);
	// }

	public void reviveApp(String appId) {
		dataModel.reviveApp(appId);
	}

	public void updateAppName(String appId, String newAppName) {
		dataModel.updateAppName(appId, newAppName);
	}

	public void updateUser(String appId, String userId, String email) {
		dataModel.updateUser(appId, userId, email);
	}

	public void updateUser(String appId, String userId, String email,
			byte[] hash, byte[] salt) {
		dataModel.updateUser(appId, userId, email, hash, salt);
	}

	public void createDocumentForApplication(String appId) {
		dataModel.createDocumentForApplication(appId);

	}

	public boolean insertIntoAppDocument(String appId, String url,
			JSONObject data, String location) {
		return dataModel.insertIntoAppDocument(appId, url, data, location);
	}

	public String getElementInDocument(String appId, String path) {
		return dataModel.getElementInDocument(path);
	}

	public boolean dataExistsForElement(String appId, String path) {
		return dataModel.dataExistsForElement(path);
	}

	public boolean elementExistsInDocument(String appId, String url) {
		return dataModel.elementExistsInDocument(url);
	}

	public boolean updateDataInDocument(String appId, String url, String data) {
		return dataModel.updateDataInDocument(url, data);
	}

	public boolean deleteDataInElement(String appId, String url) {
		return dataModel.deleteDataInElement(appId, url);
	}

	public String patchDataInElement(String url, JSONObject inputJson, String appId, String location) {
		return dataModel.patchDataInElement(url, inputJson, appId, location);
	}

	public boolean insertDocumentRoot(String appId, JSONObject data, String location) {
		return dataModel.insertDocumentRoot(appId, data, location);
	}

	public String getAllDocInApp(String appId) {
		return dataModel.getAllDocInApp(appId);
	}

	public ArrayList<String> getAllMediaIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllMediaIds(appId, pageNumber, pageSize, orderBy, orderType);
	}

	public boolean createNonPublishableDocument(String appId, JSONObject data,
			String url, String location) {
		return dataModel.createNonPublishableDocument(appId, data, url, location);
	}

	public boolean insertIntoUserDocument(String appId, String userId,
			String url, JSONObject data, String location) {
		return dataModel.insertIntoUserDocument(appId, userId, url, data, location);
	}

	public String getElementInUserDocument(String appId, String userId,
			String url) {
		return dataModel.getElementInUserDocument(appId, userId, url);
	}

	public boolean insertUserDocumentRoot(String appId, String userId, JSONObject data, String location) {
		return dataModel.insertUserDocumentRoot(appId, userId, data, location);
	}

	public boolean createNonPublishableUserDocument(String appId,
			String userId, JSONObject data, String url, String location) {
		return dataModel.createNonPublishableUserDocument(appId, userId, data,
				url, location);
	}
	
	public void updateUserLocationAndDate(String userId, String appId, String sessionToken, String location, String date) {
		dataModel.updateUserLocationAndDate(userId, appId, sessionToken, location, date);
	}

	public boolean authenticateUser(String appId, String userId,
			String attemptedPassword) {
		try {
			return dataModel.authenticateUser(appId, userId, attemptedPassword);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public boolean storageExistsInApp(String appId, String storageId) {
		return dataModel.storageExistsInApp(appId, storageId);
	}

	public Map<String, String> getStorageInApp(String appId, String storageId) {
		return dataModel.getStorageInApp(appId, storageId);
	}

	public ArrayList<String> getAllDocsInRadius(String appId, double latitude,
			double longitude, double radius) {
		return dataModel.getAllDocsInRadius(appId, latitude, longitude, radius);
	}

	public ArrayList<String> getElementInDocumentInRadius(String appId, String url,
			double latitude, double longitude, double radius) {
		return dataModel.getElementInDocumentInRadius(appId, url, latitude, longitude, radius);
	}

	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId, double latitude,
			double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllUserDocsInRadius(appId, userId, latitude, longitude, radius, pageNumber,pageSize,orderBy,orderType);
	}

	public String getAllUserDocs(String appId, String userId) {
		return dataModel.getAllUserDocs(appId, userId);
	}

	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude,
			double longitude, double radius) {
		return dataModel.getAllAudioIdsInRadius(appId, latitude, longitude, radius);
	}
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,
			double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return dataModel.getAllImagesIdsInRadius(appId, latitude, longitude, radius,pageNumber,pageSize,orderBy,orderType);
	}

	public byte[] downloadStorageInApp(String appId, String storageId,String ext) {
		return fileModel.download(appId, STORAGEFOLDER, null, storageId,ext);
	}

	public boolean confirmUsersEmailOption(String appId) {
		return dataModel.confirmUsersEmail(appId);
	}



	public void deleteImageInApp(String appId, String imageId) {
		String fileDirectory = dataModel.getFileDirectory(appId, imageId,
				MEDIAFOLDER, IMAGES);
		fileModel.deleteFile(fileDirectory);
		dataModel.deleteImageInApp(appId, imageId);
	}

	public boolean createUserWithEmailConfirmation(String appId, String userId,
			String userName, String socialId, String socialNetwork, String email, byte[] salt, byte[] hash,
			String flag, boolean emailConfirmed) throws UnsupportedEncodingException {
		boolean databaseOK = false;
		if(flag != null) 
			databaseOK = dataModel.createUserWithFlagWithEmailConfirmation(appId, userId, userName, socialId, socialNetwork,
				email, salt, hash, flag, emailConfirmed);
		else 
			databaseOK = dataModel.createUserWithoutFlagWithEmailConfirmation(appId, userId, userName, socialId, socialNetwork,
					email, salt, hash, emailConfirmed);
		//boolean awsOK = fileModel.createUser(appId, userId, userName);
		if (databaseOK /*&& awsOK*/)
			return true;
		return false;
	}

	public void confirmUserEmail(String appId, String userId) {
		dataModel.confirmUserEmail(appId, userId);
	}

	public boolean userEmailIsConfirmed(String appId, String userId) {
		return dataModel.userEmailIsConfirmed(appId, userId);
	}

	public boolean updateConfirmUsersEmailOption(String appId,
			Boolean confirmUsersEmail) {
		return Model.dataModel.updateConfirmUsersEmailOption(appId, confirmUsersEmail);
	}

	public boolean updateUserPassword(String appId, String userId, byte[] hash,
			byte[] salt) {
 		return dataModel.updateUserPassword(appId, userId, hash, salt);
	}

	public boolean userExistsInApp(String appId, String userId) {
		return dataModel.userExistsInApp(appId, userId);
	}

	

	

}
