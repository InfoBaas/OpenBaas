package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;

import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.MediaModel;

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
	public boolean createApp(String appId, String appName, boolean userEmailConfirmation) {
		return appModel.createApp(appId, appName, new Date().toString(), userEmailConfirmation);
	}

	public boolean createAppAWS(String appId) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try{
			return this.aws.createApp(appId);
			}catch(EntityAlreadyExistsException e){
				System.out.print("Entity Already Exists.");
			}catch(AmazonServiceException e){
				System.out.println("Amazon Service Exception.");
			}
		else{
			System.out.println("FileSystem not yet implemented.");
			return true;
		}
		return false;
	}


	// *** UPDATE *** //
	
	public void updateAllAppFields(String appId, String alive, String newAppName, boolean confirmUsersEmail) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			if (appModel.appExists(appId)) {
				appModel.updateAllAppFields(appId, alive, newAppName, confirmUsersEmail);
			}
		}
	}

	public void updateAppName(String appId, String newAppName) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			if (appModel.appExists(appId)) {
				appModel.updateAppName(appId, newAppName);
			}
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
		Map<String, String> fields = appModel.getApplication(appId);
		
		Application temp = new Application(appId);

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
