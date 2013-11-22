package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;

import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

public class AppsMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	AppModel appModel = new AppModel();	
	MediaModel mediaModel = new MediaModel();	
	// *** INSTANCE *** //
	
	private static AppsMiddleLayer instance = null;

	protected static AppsMiddleLayer getInstance() {
		if (instance == null) instance = new AppsMiddleLayer();
		return instance;
	}
	
	private AppsMiddleLayer() {
		super();
	}

	// *** CREATE *** //
	
	/**
	 * returns true if created Application sucessfully.
	 * 
	 * @param appId
	 * @param appName
	 * @return
	 */
	public boolean createApp(String appId, String appKey, String appName, boolean userEmailConfirmation,
			boolean AWS,boolean FTP,boolean FileSystem) {
		byte[] salt = null;
		byte[] hash = null;
		Boolean res= false;
		PasswordEncryptionService service = new PasswordEncryptionService();
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(appKey, salt);
			res = appModel.createApp(appId,appKey, hash, salt, appName, new Date().toString(), userEmailConfirmation,AWS,FTP,FileSystem);
		} catch (Exception e) {
			Log.error("", this, "createApp Login","", e); 
		}
		return res;
	}

	public boolean createAppFileSystem(String appId) {
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			return fileModel.createApp(appId);
		} catch(EntityAlreadyExistsException e) {
			Log.error("", this, "createAppFileSystem", "Entity Already Exists.", e); 
		} catch(AmazonServiceException e) {
			Log.error("", this, "createAppFileSystem", "Amazon Service error.", e); 
		}catch(Exception e) {
			Log.error("", this, "createAppFileSystem", "An error ocorred.", e); 
		}
		return false;
	}


	// *** UPDATE *** //
	
	public Application updateAllAppFields(String appId, String alive, String newAppName, boolean confirmUsersEmail,boolean AWS,boolean FTP,boolean FILESYSTEM) {
		if (appModel.appExists(appId)) {
			appModel.updateAppFields(appId, alive, newAppName, confirmUsersEmail,AWS,FTP,FILESYSTEM);
			return appModel.getApplication(appId);
		}
		return null;
	}

	public void updateAppName(String appId, String newAppName) {
		if (appModel.appExists(appId)) {
			appModel.updateAppFields(appId, null, newAppName, null, null, null, null);
		}
	}


	// *** DELETE *** //
	
	public boolean removeApp(String appId) {
		return appModel.deleteApp(appId);
	}


	// *** GET LIST *** //

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return appModel.getAllAppIds(pageNumber, pageSize, orderBy, orderType);
	}


	// *** GET *** //
	
	public Application getApp(String appId) {	
		Application temp = new Application(appId);
		temp = appModel.getApplication(appId);
		return temp;
	}
	
	public HashMap<String, String> getAuthApp(String appId) {	
		HashMap<String, String> temp = new HashMap<String, String>();
		temp = appModel.getApplicationAuth(appId);
		return temp;
	}


	// *** EXISTS *** //

	public boolean appExists(String appId) {
		return appModel.appExists(appId);
	}

	
	// *** OTHERS *** //
	
	public void reviveApp(String appId){
		appModel.reviveApp(appId);
	}

	public ArrayList<String> getAllMediaIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return mediaModel.getAllMediaIds(appId, null, pageNumber, pageSize, orderBy, orderType);
	}
	
	public Boolean authenticateApp(String appId, String appKey) {
		try {
			AppsMiddleLayer appsMid = MiddleLayerFactory.getAppsMiddleLayer();
			HashMap<String, String> fieldsAuth = appsMid.getAuthApp(appId);
			byte[] salt = null;
			byte[] hash = null;
			if(fieldsAuth.containsKey("hash") && fieldsAuth.containsKey("salt")){
				salt = fieldsAuth.get("salt").getBytes("ISO-8859-1");
				hash = fieldsAuth.get("hash").getBytes("ISO-8859-1");
			}
			PasswordEncryptionService service = new PasswordEncryptionService();
			Boolean authenticated = false;
			authenticated = service.authenticate(appKey, hash, salt);
			return authenticated;
		} catch (Exception e) {
			Log.error("", "", "authenticateAPP", "", e); 
		} 	
		return false;
	}

}
