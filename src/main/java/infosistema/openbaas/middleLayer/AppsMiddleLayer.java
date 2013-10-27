package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;

import infosistema.openbaas.model.application.Application;
import infosistema.openbaas.model.application.ApplicationInterface;

public class AppsMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	
	// *** INSTANCE *** //
	
	private static AppsMiddleLayer instance = null;

	protected static AppsMiddleLayer getInstance() {
		if (instance == null) instance = new AppsMiddleLayer();
		return instance;
	}
	
	private AppsMiddleLayer() {
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
		boolean operationOk = false;
		boolean cacheOk = false;
		boolean auxOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createApp(appId, appName, new Date().toString(), userEmailConfirmation);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createApp(appId, appName, new Date().toString(), userEmailConfirmation);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createApp(appId, appName, new Date().toString(), userEmailConfirmation);
			if (auxOk)
				operationOk = true;
		}
		return operationOk;
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
			if (redisModel.appExists(appId)) {
				redisModel.updateAllAppFields(appId, alive, newAppName, confirmUsersEmail);
			}
		}
	}

	public void updateAppName(String appId, String newAppName) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			if (redisModel.appExists(appId)) {
				redisModel.updateAppName(appId, newAppName);
			}
		}
	}


	// *** DELETE *** //
	
	public boolean removeApp(String appId) {
		boolean auxOk = false;
		boolean operationOk = false;
		boolean cacheOk = redisModel.deleteApp(appId);
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			auxOk = mongoModel.deleteApp(appId);
		if (cacheOk || auxOk)
			operationOk = true;
		return operationOk;
	}


	// *** GET LIST *** //

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.getAllAppIds(pageNumber, pageSize, orderBy, orderType);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}


	// *** GET *** //
	
	public ApplicationInterface getApp(String appId) {
		Map<String, String> fields = redisModel.getApplication(appId);
		String creationDate = null;
		String appName = null;
		Boolean confirmUsersEmail = false;
		if (fields == null || fields.size() == 0) {
			fields = mongoModel.getApplication(appId);
			if (redisModel.getCacheSize() <= MAXCACHESIZE) {
				for (Entry<String, String> entry : fields.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("creationdate"))
						creationDate = entry.getValue();
					else if (entry.getKey().equalsIgnoreCase("appName"))
						appName = entry.getValue();
					else if(entry.getKey().equalsIgnoreCase("confirmUsersEmail"))
						confirmUsersEmail = Boolean.parseBoolean(entry.getValue());
				}
				redisModel.createApp(appId, appName, creationDate, confirmUsersEmail);
			}
		}
		
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


	// *** EXISTS *** //

	public boolean appExists(String appId) {
		if (redisModel.appExists(appId))
			return true;
		else {
			return mongoModel.appExists(appId);
		}
	}

	
	// *** OTHERS *** //
	
	public void reviveApp(String appId){
		if (redisModel.appExists(appId))
			redisModel.reviveApp(appId);
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			mongoModel.reviveApp(appId);
	}

	public ArrayList<String> getAllMediaIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			return mongoModel.getAllMediaIds(appId, pageNumber, pageSize, orderBy, orderType);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

}
