package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.models.Model;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.PathSegment;
import org.codehaus.jettison.json.JSONObject;

public class DocumentMiddleLayer {

	// *** MEMBERS *** ///

	private Model model;
	
	// *** INSTANCE *** ///
	
	private static DocumentMiddleLayer instance = null;

	protected static DocumentMiddleLayer getInstance() {
		if (instance == null) instance = new DocumentMiddleLayer();
		return instance;
	}
	
	private DocumentMiddleLayer() {
		model = Model.getModel(); // SINGLETON
	}

	// *** CREATE *** ///
	
	// *** UPDATE *** ///
	
	// *** DELETE *** ///
	
	// *** GET *** ///
	
	// *** OTHERS *** ///
	
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

	public boolean createNonPublishableAppDocument(String appId, JSONObject data, 
			List<PathSegment> path, String location) {
		String url = createAppDocPathFromListWithComas(appId, path);
		return model.createNonPublishableDocument(appId, data, url, location);
	}

	public ArrayList<String> getElementInAppInRadius(String appId, List<PathSegment> path, double latitude,
			double longitude, double radius) {
		String url = createAppDocPathFromListWithComas(appId, path);
		return model.getElementInDocumentInRadius(appId, url, latitude, longitude, radius);
	}

	public ArrayList<String> getAllDocsInRadius(String appId, double latitude,
			double longitude, double radius) {
		return model.getAllDocsInRadius(appId, latitude, longitude, radius);
	}

	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId, double latitude,
			double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return model.getAllUserDocsInRadius(appId, userId, latitude, longitude, radius, pageNumber,pageSize,orderBy,orderType);
	}

	public String getAllUserDocs(String appId, String userId) {
		return model.getAllUserDocs(appId, userId);
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

}
