package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import infosistema.openbaas.dataaccess.models.Model;
import infosistema.openbaas.model.application.Application;
import infosistema.openbaas.model.application.ApplicationInterface;

public class AppsMiddleLayer {

	// *** MEMBERS *** ///

	private Model model;
	
	// *** INSTANCE *** ///
	
	private static AppsMiddleLayer instance = null;

	protected static AppsMiddleLayer getInstance() {
		if (instance == null) instance = new AppsMiddleLayer();
		return instance;
	}
	
	private AppsMiddleLayer() {
		model = Model.getModel(); // SINGLETON
	}

	// *** CREATE *** ///
	
	// *** UPDATE *** ///
	
	// *** DELETE *** ///
	
	// *** GET *** ///
	
	// *** OTHERS *** ///
	
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

	public void updateAllAppFields(String appId, String alive, String newAppName, boolean confirmUsersEmail) {
		this.model.updateAllAppFields(appId, alive, newAppName, confirmUsersEmail);
	}

	public boolean removeApp(String appId) {
		return this.model.deleteApp(appId);
	}

	public ApplicationInterface getApp(String appId) {
		Map<String, String> fields = model.getApplication(appId);
		ApplicationInterface temp = new Application(appId);

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

	public boolean appExists(String appId) {
		return model.appExists(appId);
	}

	public void reviveApp(String appId){
		this.model.reviveApp(appId);
	}

	public boolean createAppAWS(String appId) {
		return this.model.createAppFoldersAWS(appId);

	}

	public void updateAppName(String appId, String newAppName) {
		model.updateAppName(appId, newAppName);
	}

	public ArrayList<String> getAllMediaIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return model.getAllMediaIds(appId, pageNumber, pageSize, orderBy, orderType);
	}

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllAppIds(pageNumber, pageSize, orderBy, orderType);
	}

}
