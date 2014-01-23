package infosistema.openbaas.middleLayer;


import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.utils.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.PathSegment;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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
	
	public boolean insertDocumentInPath(String appId, String userId, List<PathSegment> path, JSONObject data, String location) {
		try {
			List<String> lPath = convertPath(path);
			String id = docModel.getDocumentId(userId, lPath);
			Boolean res =  docModel.insertDocumentInPath(appId, userId, lPath, data);
			if (location != null && res){
				String[] splitted = location.split(":");
				geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), ModelEnum.data, appId, id);
			}
			return res;
		} catch (JSONException e) {
			Log.error("", this, "insertDocumentInPath", "Error parsing the JSON.", e); 
			return false;
		} catch (Exception e) {
			Log.error("", this, "insertDocumentInPath", "An error ocorred.", e); 
			return false;
		}
	}


	// *** UPDATE *** //
	
	public boolean updateDocumentInPath(String appId, String userId, List<PathSegment> path, JSONObject data, String location) {
		try {
			List<String> lPath = convertPath(path);
			String id = docModel.getDocumentId(userId, lPath);
			Boolean res =  docModel.updateDocumentInPath(appId, userId, lPath, data);
			if (location != null){
				String[] splitted = location.split(":");
				geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), ModelEnum.data, appId, id);
			}
			return res;
		} catch (JSONException e) {
			Log.error("", this, "updateDocumentInPath", "Error parsing the JSON.", e); 
		} catch (Exception e) {
			Log.error("", this, "updateDocumentInPath", "An error ocorred.", e); 
		}
		return false;
	}

	
	// *** DELETE *** //

	public boolean deleteDocumentInPath(String appId, String userId, List<PathSegment> path) {
		Boolean res = false;
		
		try {
			Metadata meta = getMetadata(appId, userId, convertPathToString(path), ModelEnum.data);
			String location = meta.getLocation();
			if (location != null){
				String[] splitted = location.split(":");
				geo.deleteObjectFromGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), ModelEnum.data, appId, docModel.getDocumentId(userId, convertPath(path)));
			}
			res = docModel.deleteDocumentInPath(appId, userId, convertPath(path));
		} catch (Exception e) {
			Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return res;
	}
	
	
	// *** GET LIST *** //

	@Override
	protected List<String> getAllSearchResults(String appId, String url, JSONObject query, String orderType, ModelEnum type) throws Exception {
		return docModel.getDocuments(appId, url, query, orderType);
	}

	
	// *** GET *** //
	
	public Object getDocumentInPath(String appId, String userId, List<PathSegment> path) {
		try {
			return docModel.getDocumentInPath(appId, userId, convertPath(path));
		} catch (Exception e) {
			Log.error("", this, "getDocumentInPath", "An error ocorred.", e); 
			return null;
		}
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

	
	// *** METADATA *** //
	
	public Metadata createMetadata(String appId, String userId, String path, String creatorId, String location, JSONObject input) {
		String key = getMetaKey(appId, userId, path, ModelEnum.data);
		Map<String, String> fields = new HashMap<String, String>();
		fields.put(Metadata.CREATE_DATE, (new Date()).toString());
		fields.put(Metadata.CREATE_USER, creatorId);
		fields.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		fields.put(Metadata.LAST_UPDATE_USER, creatorId);
		fields.put(Metadata.LOCATION, location);
		deleteMetadata(appId, userId, path, ModelEnum.data);
		if (propagateMetadata(key, input, fields))
			return getMetadata(appId, userId, path, ModelEnum.data);
		else
			return null;
	}
	
	public Metadata updateMetadata(String appId, String userId, String path, String creatorId, String location, JSONObject input) {
		String key = getMetaKey(appId, userId, path, ModelEnum.data);
		Map<String, String> fields = new HashMap<String, String>();
		fields.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		fields.put(Metadata.LAST_UPDATE_USER, creatorId);
		if (location != null && !"".equals(location))
			fields.put(Metadata.LOCATION, location);
		if (propagateMetadata(key, input, fields))
			return getMetadata(appId, userId, path, ModelEnum.data);
		else
			return null;
	}

	public boolean propagateMetadata(String key, JSONObject input, Map<String, String> fields) {
		if (metadataModel.existsMetadata(key)) {
			fields.remove(Metadata.CREATE_DATE);
			fields.remove(Metadata.CREATE_USER);
		} else {
			fields.put(Metadata.CREATE_DATE, fields.get(Metadata.LAST_UPDATE_DATE));
			fields.put(Metadata.CREATE_USER, fields.get(Metadata.LAST_UPDATE_USER));
		}
		metadataModel.createUpdateMetadata(key, fields);
		fields.remove(Metadata.LOCATION);
		Iterator<?> it = input.keys();
		while (it.hasNext()) { 
			String k = (String)it.next();
			try {
				Object obj = input.get(k);
				if (obj instanceof JSONObject) {
					propagateMetadata(key + "." + k, (JSONObject)obj, fields);
				}
			} catch (Exception e) { }
		}
		return true;
	}
	
	@Override
	public Boolean deleteMetadata(String appId, String userId, String path, ModelEnum type) {
		String key = getMetaKey(appId, userId, path, type);
		return metadataModel.deleteMetadata(key, true);
	}

	
	// *** OTHERS *** //
	
}
