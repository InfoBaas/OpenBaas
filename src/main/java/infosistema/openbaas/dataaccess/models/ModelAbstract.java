package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public abstract class ModelAbstract {

	// *** CONSTANTS *** //

	protected static final int ZERO = 0; 
	protected static final String _ID = "_id"; 
	protected static final String _PARENT_PATH = "_parentPath"; 
	protected static final String _KEY = "_key";
	protected static final String _USER_ID = "_userId";
	
	protected static final String _SN_SOCIALNETWORK_ID = "SN_SocialNetwork_ID";
	protected static final String _BASE_LOCATION_OPTION = "baseLocationOption";
	protected static final String _HASH = "hash";
	protected static final String _EMAIL = "email";
	protected static final String _ALIVE = "alive";
	protected static final String _SALT = "salt";
		
		
	private static final String AND_QUERY_FORMAT = "{%s, %s}";
	private static final String OR_QUERY_FORMAT = "{$or: [%s, %s]}";
	private static final String NOT_QUERY_FORMAT = "{$not: %s}";
	private static final String CONTAINS_QUERY_FORMAT = "{\"%s\": \"*%s*\"}";
	private static final String EQUALS_QUERY_FORMAT = "{\"%s\": %s}";
	private static final String GREATER_QUERY_FORMAT = "{\"%s\": {$gt: %s} }";
	private static final String LESSER_QUERY_FORMAT = "{\"%s\": {$lt: %s} }";
	private static final String KEY_EXISTS_QUERY_FORMAT = "{\"" + _ID +"\": \"%s\", \"%s\": {$exists: true}}";
	private static final String ID_QUERY_FORMAT = "{\"" + _ID +"\": \"%s\"}";
	private static final String USER_ID_QUERY_FORMAT = "{\"" + _USER_ID +"\": \"%s\"}";
	private static final String CHILD_IDS_TO_REMOVE_QUERY_FORMAT = "{\"" + _ID +"\":  {$regex: \"%s.\"}}";

	protected BasicDBObject dataProjection = null; 	

	// *** VARIABLES *** //
	
	private MongoClient mongoClient;
	private DB db;

	public ModelAbstract() {
		try {
			mongoClient = new MongoClient(Const.getMongoServer(), Const.getMongoPort());
			db = mongoClient.getDB(Const.getMongoDb());
		} catch (UnknownHostException e) {
			Log.error("", this, "DocumentModel", "Unknown Host.", e); 
		}
	}

	
	// *** PROTECTED *** //

	protected BasicDBObject getDataProjection() {
		if (dataProjection == null) {
			dataProjection = new BasicDBObject();
			dataProjection.append(_ID, ZERO);
			dataProjection.append(_KEY, ZERO);
			dataProjection.append(_USER_ID, ZERO);
			dataProjection.append(_PARENT_PATH, ZERO);
			
			dataProjection.append(_SN_SOCIALNETWORK_ID, ZERO);
			dataProjection.append(_BASE_LOCATION_OPTION, ZERO);
			dataProjection.append(_HASH, ZERO);
			dataProjection.append(_EMAIL, ZERO);
			dataProjection.append(_ALIVE, ZERO);
			dataProjection.append(_SALT, ZERO);
		}
		return dataProjection;
	}

	protected DBCollection getCollection(String collStr) {
		return db.getCollection(collStr);
	}

	protected JSONObject getJSonObject(Map<String, String> fields) throws JSONException  {
		JSONObject obj = new JSONObject();
		for (String key : fields.keySet()) {
			if (fields.get(key) != null) {
				String value = fields.get(key);
				obj.put(key, value);
			}
		}
		return obj;
	}
	
	protected Map<String, String> getObjectFields(JSONObject obj) throws JSONException  {
		Map<String, String> fields = new HashMap<String, String>();
		Iterator it = obj.keys();
		while (it.hasNext()) {
			String key = it.next().toString();
			fields.put(key, obj.getString(key));
		}
		return fields;
	}
	
	protected List<String> removeLast(List<String> path) {
		List<String> newPath = null;
		if (path != null && path.size() > 0) {
			newPath = new ArrayList<String>();
			newPath.addAll(path);
			newPath.remove(path.size() -1);
		}
		return newPath;
	}
	
	protected List<String> addPath(List<String> path, String key) {
		List<String> newPath = new ArrayList<String>();
		newPath.addAll(path);
		newPath.add(key);
		return newPath;
	}
	
	// *** CREATE *** //

 	protected Boolean insert(String appId, JSONObject value) throws JSONException{
		DBCollection coll = getCollection(appId);
		JSONObject  data = new JSONObject(value.toString());
		DBObject dbData = (DBObject)JSON.parse(data.toString());
		coll.insert(dbData);
		return true;
	}
	
	// *** UPDATE *** //
	
	protected Boolean updateDocumentValue(String appId, String id, String key, Object value) throws JSONException{
		DBCollection coll = getCollection(appId);
		try{
			if (value instanceof JSONObject)
				value = (DBObject)JSON.parse(value.toString());
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append(_ID, id); 		
			BasicDBObject dbBase = new BasicDBObject();
			dbBase.append(key, value);
			BasicDBObject dbUpdate = new BasicDBObject();
			dbUpdate.append("$set", dbBase);
			coll.update(dbQuery, dbUpdate);
		} catch (Exception e) {
			Log.error("", this, "updateDocument", "An error ocorred.", e); 
			return false;
		}
		return true;
	}
	
	// *** DELETE *** //
	
	protected Boolean deleteDocument(String appId, String id) {
		DBCollection coll = getCollection(appId);
		try{
			BasicDBObject dbRemove = new BasicDBObject();
			dbRemove.append(_ID, id);  
			coll.remove(dbRemove); 
		} catch (Exception e) {
			Log.error("", this, "deleteDocument", "An error ocorred.", e); 
			return false;
		}
		return true;		
	}
	
	public Boolean removeKeyFromDocument(String appId, String id, String key) throws JSONException {
		DBCollection coll = getCollection(appId);
		BasicDBObject ob = new BasicDBObject();
		BasicDBObject dbRemove = new BasicDBObject();
		ob.append(key, "");
		dbRemove.append("$unset", ob);
		BasicDBObject dbQuery = new BasicDBObject();
		dbQuery.append(_ID, id); 		
		coll.update(dbQuery, dbRemove);
		return true;
	}
	
	// *** GET LIST *** //

	public List<String> getDocuments(String appId, String userId, String path, JSONObject query, String orderType) throws Exception {
		String strParentPathQuery = getParentPathQueryString(path);
		String strQuery = getQueryString(appId, path, query, orderType);
		String searchQuery = getAndQueryString(strParentPathQuery, strQuery);
		if (userId != null && !"".equals(userId))
			searchQuery = getAndQueryString(searchQuery, getUserIdQueryString(userId));
		DBCollection coll = getCollection(appId);
		DBObject queryObj = (DBObject)JSON.parse(searchQuery);
		BasicDBObject projection = new BasicDBObject();
		projection.append(_ID, 1);
		DBCursor cursor = coll.find(queryObj, projection);
		List<String> retObj = new ArrayList<String>();
		while (cursor.hasNext()) {
			retObj.add(cursor.next().get(_ID).toString());
		}
		return retObj;
	}
	
	protected String getQueryString(String appId, String path, JSONObject query, String orderType) throws Exception {
		if (query!=null) {
			OperatorEnum oper = OperatorEnum.valueOf(query.getString(OperatorEnum.oper.toString())); 
			if (oper == null)
				throw new Exception("Error in query."); 
			else if (oper.equals(OperatorEnum.and)) {
				String oper1 = getQueryString(appId, path, (JSONObject)(query.get(OperatorEnum.op1.toString())), orderType);
				String oper2 = getQueryString(appId, path, (JSONObject)(query.get(OperatorEnum.op2.toString())), orderType);
				return getAndQueryString(oper1, oper2);
			} else if (oper.equals(OperatorEnum.or)) {
				String oper1 = getQueryString(appId, path, (JSONObject)(query.get(OperatorEnum.op1.toString())), orderType);
				String oper2 = getQueryString(appId, path, (JSONObject)(query.get(OperatorEnum.op2.toString())), orderType);
				return getOrQueryString(oper1, oper2);
			} else if (oper.equals(OperatorEnum.not)) {
				String oper1 = getQueryString(appId, path, (JSONObject)(query.get(OperatorEnum.op1.toString())), orderType);
				return getNotQueryString(oper1);
			} else {
				String value = null; 
				try { value = query.getString(QueryParameters.ATTR_VALUE); } catch (Exception e) {}
				String attribute = null;
				try { attribute = query.getString(QueryParameters.ATTR_ATTRIBUTE); } catch (Exception e) {}
				if (attribute == null) {
					try { attribute = query.getString(QueryParameters.ATTR_PATH); } catch (Exception e) {}
				}
				return getOperationQueryString(oper, attribute, value);
			}
		} else {
			return null;
		}
	}
	
	protected String getParentPathQueryString(String path) {
		return "";
	}
	
	private String getAndQueryString(String oper1, String oper2) {
		if (oper1 == null || "".equals(oper1) || oper1.contains("null")) return (oper2 == null || "".equals(oper2)) ? "" : oper2;
		if (oper2 == null || "".equals(oper2) || oper2.contains("null")) return oper1;
		if (oper1.startsWith("{")) oper1 = oper1.substring(1);
		if (oper1.endsWith("}")) oper1 = oper1.substring(0, oper1.length() - 1);
		if (oper2.startsWith("{")) oper2 = oper2.substring(1);
		if (oper2.endsWith("}")) oper2 = oper2.substring(0, oper2.length() - 1);
		return String.format(AND_QUERY_FORMAT, oper1, oper2);
	}
	
	private String getOrQueryString(String oper1, String oper2) {
		return String.format(OR_QUERY_FORMAT, oper1, oper2);
	}

	private String getNotQueryString(String oper1) {
		return String.format(NOT_QUERY_FORMAT, oper1);
	}
	
	private String getOperationQueryString(OperatorEnum oper, String attribute, String value) {
		if (oper.equals(OperatorEnum.contains))
			return String.format(CONTAINS_QUERY_FORMAT, attribute, value);
		else if (oper.equals(OperatorEnum.equals))
			return String.format(EQUALS_QUERY_FORMAT, attribute, value);
		else if (oper.equals(OperatorEnum.greater))
			return String.format(GREATER_QUERY_FORMAT, attribute, value);
		else if (oper.equals(OperatorEnum.lesser))
			return String.format(LESSER_QUERY_FORMAT, attribute, value);
		else
			return "";
	}

	private String getKeyExists(String id, String key) {
		return String.format(KEY_EXISTS_QUERY_FORMAT, id, key);
	}

	protected String getIdsToRemoveQueryString(String id) {
		String oper1 = String.format(ID_QUERY_FORMAT, id);
		String oper2 = String.format(CHILD_IDS_TO_REMOVE_QUERY_FORMAT, id);
		return getOrQueryString(oper1, oper2);
	}
	
	protected String getUserIdQueryString(String id) {
		return String.format(USER_ID_QUERY_FORMAT, id);
	}
	

	// *** GET *** //

	protected JSONObject getDocument(String appId, String id) throws JSONException {
		DBCollection coll = getCollection(appId);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(_ID, id);
		BasicDBObject projection = getDataProjection();
		DBCursor cursor = coll.find(searchQuery, projection);
		if (cursor.hasNext()) {
			return new JSONObject(JSON.serialize(cursor.next()));
		}
		return null;
	}
	
	protected List<DBObject> getDocumentAndChilds(String appId, String id) {		
		List<DBObject> res = new ArrayList<DBObject>();
		try {
			DBCollection coll = getCollection(appId);
			String sQuery = getIdsToRemoveQueryString(id);
			DBObject regexQuery = (DBObject)JSON.parse(sQuery);
			BasicDBObject projection = new BasicDBObject();
			projection.append(_ID, 1);
			DBCursor cursor = coll.find(regexQuery, projection);
			res = cursor.toArray();
		}catch (Exception e) {
			Log.error("", this, "getDocumentAndChilds", "Error quering mongoDB.", e);
		}
		return res;
	}

	
	// *** EXISTS *** //

	protected Boolean existsNode(String appId, String id) {
		DBCollection coll = getCollection(appId);
		try{
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append(_ID, id); 		
			BasicDBObject dbProjection = new BasicDBObject();
			dbProjection.append(_ID, 1);
			DBCursor cursor = coll.find(dbQuery, dbProjection).limit(1);
			if(cursor.hasNext())
				return true;
			else 
				return false;
		} catch (Exception e) {
			Log.error("", this, "existsDocument", "An error ocorred.", e); 
		}
		return false;
	}

	protected Boolean existsKey(String appId, String id, String key) {
		DBCollection coll = getCollection(appId);
		String sQuery = getKeyExists(id, key);
		try{
			DBObject queryObj = (DBObject)JSON.parse(sQuery);
			BasicDBObject projection = new BasicDBObject();
			projection.append(_ID, 1);
			DBCursor cursor = coll.find(queryObj, projection);
			return cursor.hasNext();
		} catch (Exception e) {
			Log.error("", this, "existsDocument", "An error ocorred.", e); 
		}
		return false;
	}
	
}
