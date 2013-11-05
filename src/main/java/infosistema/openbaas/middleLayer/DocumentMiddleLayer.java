package infosistema.openbaas.middleLayer;


import infosistema.openbaas.data.ModelEnum;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;

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

	
	// *** PRIVATE *** //

	private String getDocumentPath(String userId, List<PathSegment> path) {
		StringBuilder sb = new StringBuilder();
		if (userId == null)
			sb.append("data.");
		else
			sb.append(userId).append(".");
		if (path != null) {
			for(int i = 0; i < path.size(); i++)
				sb.append(path.get(i).getPath()).append('.');
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	// *** CREATE *** //
	
	public boolean insertDocumentInPath(String appId, String userId, List<PathSegment> path, JSONObject data, String location) {
		try {
			String pathRes = getDocumentPath(userId, path);
			Boolean res =  docModel.insertDocumentInPath(appId, userId, pathRes, data);
			if (location != null){
				String[] splitted = location.split(":");
				Geolocation geo = Geolocation.getInstance();
				geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), ModelEnum.data, appId, pathRes);
			}
			return res;
		} catch (JSONException e) {
			return false;
		} catch (Exception e) {
			return false;
		}
	}


	// *** UPDATE *** //
	
	public boolean updateDocumentInPath(String appId, String userId, List<PathSegment> path, JSONObject data, String location) {
		try {
			String pathRes = getDocumentPath(userId, path);
			Boolean res =  docModel.updateDocumentInPath(appId, userId, pathRes, data);
			if (location != null){
				String[] splitted = location.split(":");
				Geolocation geo = Geolocation.getInstance();
				geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), ModelEnum.data, appId, pathRes);
			}
			return res;
		} catch (JSONException e) {
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	
	// *** DELETE *** //

	public boolean deleteDocumentInPath(String appId, String userId, List<PathSegment> path) {
		try {
			return docModel.deleteDocumentInPath(appId, getDocumentPath(userId, path));
		} catch (Exception e) {
			return false;
		}
	}
	
	
	// *** GET LIST *** //

	//XPTO: Refazer isto tudo 
	/*
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
			return docModel.getAllUserDocsInRadius(appId, latitude, longitude, radius, pageNumber,pageSize,orderBy,orderType);
		} catch (Exception e) {
			return null;
		}
	}

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
	*/

	
	// *** GET *** //
	
	public String getDocumentInPath(String appId, String userId, List<PathSegment> path) {
		try {
			return docModel.getDocumentInPath(appId, userId, getDocumentPath(userId, path));
		} catch (Exception e) {
			return null;
		}
	}
	
	// *** EXISTS *** //

	//XPTO
	public boolean existsDocumentInPath(String appId, String userId, List<PathSegment> path) {
		try {
			return docModel.existsDocumentInPath(appId, getDocumentPath(userId, path));
		} catch (Exception e) {
			return false;
		}
	}

	// *** OTHERS *** //
	
}
