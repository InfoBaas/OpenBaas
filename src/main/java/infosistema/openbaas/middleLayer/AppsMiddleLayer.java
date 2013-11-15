package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.Date;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;

import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.utils.Log;

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
	public boolean createApp(String appId, String appName, boolean userEmailConfirmation,boolean AWS,boolean FTP,boolean FileSystem) {
		return appModel.createApp(appId, appName, new Date().toString(), userEmailConfirmation,AWS,FTP,FileSystem);		
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

}
