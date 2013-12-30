package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.utils.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class DocumentModel extends ModelAbstract {

	// *** CONSTANTS *** //

	private static final String DATA = "data";
	private static final String _PARENT_PATH = "_parentPath"; 
	private static final String _KEY = "_key";
	private static final String PARENT_PATH_QUERY_FORMAT = "{\"" + _PARENT_PATH + "\": \"%s\"}";
	private static final String APP_DATA_COLL_FORMAT = "app%sdata";

	
	// *** VARIABLES *** //
	
	Geolocation geo;

	public DocumentModel() {
		super();
	}

	
	// *** PROTECTED *** //

	@Override
	protected DBCollection getCollection(String appId) {
		return super.getCollection(String.format(APP_DATA_COLL_FORMAT, appId));
	}
	
	public String getDocumentPath(List<String> path) {
		if (path == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size(); i++)
			if (!"".equals(path.get(i))) sb.append(path.get(i)).append('.');
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	protected String getDocumentParentPath(List<String> path) {
		if (path == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size() - 1; i++)
			if (!"".equals(path.get(i))) sb.append(path.get(i)).append('.');
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	public String getDocumentId(String userId, List<String> path) {
		StringBuilder sb = new StringBuilder();
		if (userId == null)
			sb.append("data.");
		else
			sb.append(userId).append(".");
		if (path != null) {
			for(int i = 0; i < path.size(); i++)
				if (!"".equals(path.get(i))) sb.append(path.get(i)).append('.');
		}
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	protected String getDocumentKey(List<String> path) {
		String retObj = "";
		if (path != null && path.size() > 0) {
			retObj = path.get(path.size() - 1);
		}
		return retObj;
	}

	private static BasicDBObject dataProjection = null; 	
	protected BasicDBObject getDataProjection() {
		if (dataProjection == null) {
			dataProjection = super.getDataProjection(new BasicDBObject());
			dataProjection.append(_KEY, ZERO);
			dataProjection.append(_USER_ID, ZERO);
			dataProjection.append(_PARENT_PATH, ZERO);
		}
		return dataProjection;
	}

	
	// *** CREATE *** //

	public Boolean insertDocumentInPath(String appId, String userId, List<String> path, JSONObject data) throws JSONException {
		deleteDocumentInPath(appId, userId, path);
		return insertDocument(appId, userId, path, data) && 
				updateAscendents(appId, userId, path, data);
	}
	
	private Boolean insertDocument(String appId, String userId, List<String> path, JSONObject data) throws JSONException{
		try{ 
			Iterator<?> it = data.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = data.get(key);
				if (value instanceof JSONObject) {
					List<String> newPath = addPath(path, key);
					insertDocument(appId, userId, newPath, (JSONObject)value);
				}
			}
			return insert(appId, userId, path, data);
		} catch (Exception e) {
			Log.error("", this, "insertDocumentInPath", "An error ocorred.", e);
			return false;
		}
	}

	private Boolean insert(String appId, String userId, List<String> path, JSONObject value) throws JSONException{
		String id = getDocumentId(userId, path);
		JSONObject  data = new JSONObject(value.toString());
		data.put(_KEY, getDocumentKey(path));
		data.put(_ID, id);
		if (userId == null)  userId = DATA;
		data.put(_USER_ID, userId);
		data.put(_PARENT_PATH, getDocumentParentPath(path));
		if(!existsDocument(appId, userId, path) || !getDocumentId(userId, path).equals(userId))
			return super.insert(appId, data);
		return true;
	}
	
	// *** UPDATE *** //
	
	public Boolean updateDocumentInPath(String appId, String userId, List<String> path, JSONObject data) throws JSONException{
		try{
			String id = getDocumentId(userId, path);
			if (!existsNode(appId, id)) 
				return insertDocument(appId, userId, path, data) && updateAscendents(appId, userId, path, data);
			Iterator<?> it = data.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = data.get(key);
				if (value instanceof JSONObject) {
					List<String> newPath = addPath(path, key);
					if(existsNode(appId, id+"."+key)){
						deleteDocument(appId, id+"."+key);
					}
					insertDocument(appId, userId, newPath, (JSONObject)value);
				} 
				updateDocumentValue(appId, id, key, value);
			}
			JSONObject newData = (JSONObject)getDocumentInPath(appId, userId, path);
			updateAscendents(appId, userId, path, newData);
		} catch (Exception e) {
			Log.error("", this, "updateDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return true;
	}
	
	private Boolean updateDocumentValues(String appId, String id, String key, JSONObject value) throws JSONException{
		Iterator it = value.keys();
		while (it.hasNext()) {
			String k = it.next().toString();
			Object v = value.get(k);
			updateDocumentValue(appId, id, k, v);
		}
		return true;
	}
	
	private Boolean updateAscendents(String appId, String userId, List<String> path, JSONObject data) throws JSONException{
		if (userId == null || "".equals(userId)) userId = DATA; 
		String key = getDocumentKey(path);
		path = removeLast(path);
		String id = getDocumentId(userId, path);
		if (!existsNode(appId, id))
			insert(appId, userId, path, new JSONObject());
		if ("".equals(key))
			return updateDocumentValues(appId, id, key, data);
		else {
			updateDocumentValue(appId, id, key, data);
			return updateAscendents(appId, userId, path, (JSONObject)getDocumentInPath(appId, userId, path));
		}
	}
	
	
	// *** DELETE *** //
	
	public Boolean deleteDocumentInPath(String appId, String userId, List<String> path) throws JSONException{
		String id = getDocumentId(userId, path);
		if (existsNode(appId, id)) {
			try{
				List<DBObject> listToDel = getDocumentAndChilds(appId, id);		
				Iterator<DBObject> itDoc = listToDel.iterator();
				while(itDoc.hasNext()){
					DBObject doc = itDoc.next();
					deleteDocument(appId, doc.get(_ID).toString());
				}
				removeKeyFromAscendents(appId, userId, "", path);
			}
			catch (Exception e) {
				Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
				return false;
			}
		} else {
			try{
				String key = getDocumentKey(path);
				path = removeLast(path);
				id = getDocumentId(userId, path);
				DBCollection coll = getCollection(appId);
				BasicDBObject ob = new BasicDBObject();
				BasicDBObject dbRemove = new BasicDBObject();
				ob.append(key, "");
				dbRemove.append("$unset", ob);
				BasicDBObject dbQuery = new BasicDBObject();
				dbQuery.append(_ID, id); 		
				coll.update(dbQuery, dbRemove);
				removeKeyFromAscendents(appId, userId, key, path);
			}
			catch (Exception e) {
				Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
				return false;
			}
		}
		return true;
	}
	
	public Boolean removeKeyFromAscendents(String appId, String userId, String key, List<String> path) throws JSONException {
		if (path == null || path.size() <= 0) return true;
		String auxKey = getDocumentKey(path);
		key = auxKey + ((key == null || "".equals(key)) ? "" : "." + key);
		path = removeLast(path);
		String id = getDocumentId(userId, path); 
		super.removeKeyFromDocument(appId, id, key);
		return removeKeyFromAscendents(appId, userId, key, path);
	}
	
	// *** GET LIST *** //

	@Override
	protected String getParentPathQueryString(String path) {
		return String.format(PARENT_PATH_QUERY_FORMAT, path);
	}
	

	// *** GET *** //

	public Object getDocumentInPath(String appId, String userId, List<String> path) throws JSONException {
		String id = getDocumentId(userId, path);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(_ID, id);
		if (existsNode(appId, id)) {
			return super.getDocument(appId, id);
		} else if (path != null && path.size() > 0) {
			String key = getDocumentKey(path);
			path = removeLast(path);
			id = getDocumentId(userId, path);
			if (existsNode(appId, id))
				return (super.getDocument(appId, id).get(key));
		}
		return null;
	}
	
	
	// *** EXISTS *** //

	public Boolean existsDocument(String appId, String userId, List<String> path) {
		return existsNode(appId, getDocumentId(userId, path)) ||
				existsKey(appId, userId, path);
	}

	private Boolean existsKey(String appId, String userId, List<String> path) {
		if (path == null || path.size() <= 0) return false;
		String key = getDocumentKey(path);
		path = removeLast(path);
		String id = getDocumentId(userId, path);
		return super.existsKey(appId, id, key);
	}
	
}
