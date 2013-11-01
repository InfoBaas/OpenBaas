package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.PathSegment;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class DocumentMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	// *** INSTANCE *** //
	
	private static DocumentMiddleLayer instance = null;

	protected static DocumentMiddleLayer getInstance() {
		if (instance == null) instance = new DocumentMiddleLayer();
		return instance;
	}
	
	private DocumentMiddleLayer() {
		super();
	}

	
	// *** CREATE *** //
	
	public String createAppDocPathFromListWithSlashes(String appId, List<PathSegment> path) {
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

	//XPTO
	public boolean insertUserDocumentRoot(String appId, String userId, JSONObject data, String location) {
		try {
			//return docModel.insertUserDocumentRoot(appId, userId, data, location);
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	//XPTO: PARA QUE SERVE ISTO????
	/*
	public boolean createNonPublishableUserDocument(String appId, String userId, JSONObject data, String url, String location) {
		try {
			return docModel.createNonPublishableUserDocument(appId, userId, data, url, location);
		} catch (Exception e) {
			return false;
		}
	}
	*/

	//XPTO
	public boolean insertIntoUserDocument(String appId, String userId, String url, JSONObject data, String location) {
		try {
			return false;
			//return docModel.insertIntoUserDocument(appId, userId, data, url, location);
		} catch (Exception e) {
			return false;
		}
	}

	//XPTO
	/*
	public boolean createNonPublishableAppDocument(String appId, JSONObject data, List<PathSegment> path, String location) {
		String url = getAppDocPathFromListWithComas(appId, path);
		try {
			return docModel.createNonPublishableDocument(appId, data, url, location);
		} catch (Exception e) {
			return false;
		}
	}
	*/

	//XPTO
	public boolean insertAppDocumentRoot(String appId, JSONObject data, String location) {
		try {
			//return docModel.insertDocumentRoot(appId, data, location);
		//} catch (JSONException e) {
			//e.printStackTrace();
		} catch (Exception e) {
			return false;
		}
		return false;
	}


	// *** UPDATE *** //
	
	//XPTO
	public boolean insertIntoAppDocument(String appId, String url, JSONObject data, String location) {
		try {
			//docModel.insertIntoDocument(appId, url, data, location);
		//} catch (JSONException e) {
			//e.printStackTrace();
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	//XPTO
	public boolean updateDataInDocument(String appId, String url, String data) {
		String path = convertDocPathWithSlashestoComas(appId, url);
		try {
			//return docModel.updateDataInDocument(appId, path, data);
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	//XPTO
	public String patchDataInElement(String appId, List<PathSegment> path, JSONObject inputJson, String location) {
		String url = getAppDocPathFromListWithComas(appId, path);
		/*
		try {
			return docModel.patchDataInElement(url, inputJson, appId, location);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		*/
		return null;
	}

	
	// *** DELETE *** //

	public boolean deleteDataInElement(String appId, List<PathSegment> path) {
		String url = getAppDocPathFromListWithComas(appId, path);
		return deleteDataInElement(appId, url);
	}
	public boolean deleteUserDataInElement(String appId, String userId, List<PathSegment> path) {
		String url = createUserDocPathFromListWithComas(appId, userId, path);
		return deleteDataInElement(appId, url);
	}
	
	//XPTO
	private boolean deleteDataInElement(String appId, String url) {
		try {
			//return docModel.deleteDataInDocument(appId, url);
			return false;
		} catch (Exception e) {
			return false;
		}
	}
	
	
	// *** GET LIST *** //

	public String getAllUserDocs(String appId, String userId) {
		try {
			return docModel.getAllUserDocs(appId, userId);
		} catch (Exception e) {
			return null;
		}
	}

	public ArrayList<String> getAllDocsInRadius(String appId, double latitude, double longitude, double radius) {
		try {
			return docModel.getAllDocsInRadius(appId, latitude, longitude, radius);
		} catch (Exception e) {
			return null;
		}
	}

	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId, double latitude, double longitude, 
			double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		try {
			return docModel.getAllUserDocsInRadius(appId, userId, latitude, longitude, radius, pageNumber,pageSize,orderBy,orderType);
		} catch (Exception e) {
			return null;
		}
	}

	//XPTO
	public ArrayList<String> getElementInAppInRadius(String appId, List<PathSegment> path, double latitude,
			double longitude, double radius) {
		String url = getAppDocPathFromListWithComas(appId, path);
		try {
			//return docModel.getDataInDocumentInRadius(appId, url, latitude, longitude,radius);
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public String getAllDocInApp(String appId) {
		try {
			return docModel.getAllDocInApp(appId);
		} catch (Exception e) {
			return null;
		}
	}

	
	// *** GET *** //
	
	//XPTO
	public String getElementInAppDocument(String appId, List<PathSegment> path) {
		String url = getAppDocPathFromListWithComas(appId, path);
		try {
			//return docModel.getDataInDocument(appId, url);
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	//XPTO
	public String getElementInUserDocument(String appId, String userId, List<PathSegment> path){
		String url = createUserDocPathFromListWithComas(appId, userId, path);
		try {
			//return docModel.getElementInUserDocument(appId, userId, url);
			return null;
		} catch (Exception e) {
			return null;
		}
	}
	
	// *** EXISTS *** //

	//XPTO
	public boolean elementExistsInDocument(String appId, String url) {
		String [] path = url.split("/");
		StringBuilder tempPath = new StringBuilder();
		for(int i = 0; i < path.length; i++)
			tempPath.append(path[i] + ",");
		tempPath.deleteCharAt(tempPath.length()-1); //delete last comma
		try {
			//return docModel.elementExistsInDocument(appId, url);
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean dataExistsForElement(String appId, List<PathSegment> path) {
		String url = getAppDocPathFromListWithComas(appId, path);
		return dataExistsForElement(appId, url);
	}
	
	public boolean dataExistsForUserElement(String appId, String userId, List<PathSegment> path) {
		String url = createUserDocPathFromListWithComas(appId, userId, path);
		return dataExistsForElement(appId, url);
	}

	//private
	
	//XPTO
	private boolean dataExistsForElement(String appId, String path) {
		try {
			//return docModel.dataExistsForElement(appId, path);
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	
	// *** OTHERS *** //
	
	private String getAppDocPathFromListWithComas(String appId, List<PathSegment> path){
		StringBuilder sb = new StringBuilder();
		sb.append(appId + ",");
		for(int i = 0; i < path.size(); i++)
			sb.append(path.get(i).getPath()).append(',');
		sb.deleteCharAt(sb.length()-1); //delete last comma
		return sb.toString();
	}
	
	private String createUserDocPathFromListWithComas(String appId, String userId, List<PathSegment> path) {
		StringBuilder sb = new StringBuilder();
		sb.append(appId + "," + "userId" + ",");
		for(int i = 0; i < path.size(); i++)
			sb.append(path.get(i).getPath()).append(',');
		sb.deleteCharAt(sb.length()-1); //delete last comma
		return sb.toString();
	}

	private String convertDocPathWithSlashestoComas(String appId, String url){
		StringBuilder sb = new StringBuilder();
		String [] array = url.split("/");
		for(int i = 0; i < array.length; i++){
			sb.append(array[i]).append(",");
		}
		sb.deleteCharAt(sb.length()-1); //delete last comma
		return sb.toString();
	}
	
}
