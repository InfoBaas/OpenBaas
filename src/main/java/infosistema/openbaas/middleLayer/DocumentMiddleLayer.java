package infosistema.openbaas.middleLayer;


import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.dataaccess.models.ModelAbstract;
import infosistema.openbaas.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.PathSegment;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class DocumentMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	// *** INSTANCE *** //
	
	private static DocumentMiddleLayer instance = null;

	public static DocumentMiddleLayer getInstance() {
		if (instance == null) instance = new DocumentMiddleLayer();
		return instance;
	}
	
	private DocumentMiddleLayer() {
		super();
	}

	
	// *** PRIVATE *** //

	public String convertPathToString(List<PathSegment> path) {
		StringBuilder sb = new StringBuilder();
		if (path != null && !path.isEmpty()) {
			for(int i = 0; i < path.size(); i++)
				sb.append(path.get(i).getPath()).append('.');
			sb.deleteCharAt(sb.length()-1);
		}
		return sb.toString();
	}

	public String getDocumentPath(String userId, List<PathSegment> path) {
		return docModel.getDocumentPath(convertPath(path));
	}


	// *** CREATE *** //
	
	public Result insertDocumentInPath(String appId, String userId, List<PathSegment> path, JSONObject document, Map<String, String> extraMetadata) {
		try {
			Metadata metadata = null;
			Object data = null;
			List<String> lPath = convertPath(path);
			data = docModel.insertDocumentInPath(appId, userId, lPath, document, extraMetadata);
			if(((JSONObject) data).has(ModelAbstract._METADATA)){
				metadata = Metadata.getMetadata(new JSONObject(((JSONObject) data).getString(ModelAbstract._METADATA)));
				((JSONObject) data).remove(ModelAbstract._METADATA);
			}
			data = (DBObject)JSON.parse(data.toString());
			return new Result(data, metadata);
		} catch (JSONException e) {
			Log.error("", this, "insertDocumentInPath", "Error parsing the JSON.", e); 
		} catch (Exception e) {
			Log.error("", this, "insertDocumentInPath", "An error ocorred.", e); 
		}
		return null;
	}


	// *** UPDATE *** //
	
	public Result updateDocumentInPath(String appId, String userId, List<PathSegment> path, JSONObject document, Map<String, String> extraMetadata) {
		try {
			Metadata metadata = null;
			Object data = null;
			data = docModel.updateDocumentInPath(appId, userId, convertPath(path), document, extraMetadata);
			if(((JSONObject) data).has(ModelAbstract._METADATA)){
				metadata = Metadata.getMetadata(new JSONObject(((JSONObject) data).getString(ModelAbstract._METADATA)));
				((JSONObject) data).remove(ModelAbstract._METADATA);
			}
			data = (DBObject)JSON.parse(data.toString());
			return new Result(data, metadata);
		} catch (JSONException e) {
			Log.error("", this, "updateDocumentInPath", "Error parsing the JSON.", e); 
		} catch (Exception e) {
			Log.error("", this, "updateDocumentInPath", "An error ocorred.", e); 
		}
		return null;
	}

	
	// *** DELETE *** //

	public boolean deleteDocumentInPath(String appId, String userId, List<PathSegment> path) {
		Boolean res = false;
		try {
			res = docModel.deleteDocumentInPath(appId, userId, convertPath(path));
		} catch (Exception e) {
			Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return res;
	}
	
	
	// *** GET LIST *** //

	@Override
	protected List<DBObject> getAllSearchResults(String appId, String userId, String url, Double latitude, Double longitude, Double radius, JSONObject query, String orderType, String orderBy, ModelEnum type, List<String> toShow) throws Exception {
		return docModel.getDocuments(appId, userId, url, latitude, longitude, radius, query, orderType, orderBy,toShow);
	}

	
	// *** GET *** //
	
	public Result getDocumentInPath(String appId, String userId, List<PathSegment> path, boolean getMetadata, JSONArray arrayToShow, JSONArray arrayToHide) {
		Metadata metadata = null;
		Object data = null;
		List<String> toShow = new ArrayList<String>();
		List<String> toHide = new ArrayList<String>();
		try {
			toShow = convertJsonArray2ListString(arrayToShow);
			toHide = convertJsonArray2ListString(arrayToHide);
			data = docModel.getDocumentInPath(appId, userId, convertPath(path), getMetadata, toShow, toHide);
			if (data instanceof JSONObject) {
				if (getMetadata) {
					if(((JSONObject) data).has(ModelAbstract._METADATA)){
						metadata = Metadata.getMetadata(new JSONObject(((JSONObject) data).getString(ModelAbstract._METADATA)));
						((JSONObject) data).remove(ModelAbstract._METADATA);
					}
					data = (DBObject)JSON.parse(data.toString());
				}
			}
		} catch (Exception e) {
			Log.error("", this, "getDocumentInPath", "An error ocorred.", e); 
			return null;
		}
		return new Result(data, metadata);
	}
	
	// *** EXISTS *** //

	

	public boolean existsDocumentInPath(String appId, String userId, List<PathSegment> path) {
		try {
			return docModel.existsDocument(appId, userId, convertPath(path));
		} catch (Exception e) {
			Log.error("", this, "existsDocumentInPath", "An error ocorred.", e); 
			return false;
		}
	}

	
	// *** OTHERS *** //
	
}
