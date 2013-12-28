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
	protected static final String _PARENT_PATH = "_parentPath"; 
	protected static final String _ID = "_id"; 
	protected static final String _KEY = "_key";
	protected static final String _USER_ID = "_userId";
	protected static final String DATA = "data";
	
	private static final String PARENT_PATH_QUERY_FORMAT = "{\"" + _PARENT_PATH + "\": \"%s\"}";
	private static final String AND_QUERY_FORMAT = "{%s, %s}";
	private static final String OR_QUERY_FORMAT = "{ $or: [%s, %s]}}";
	private static final String NOT_QUERY_FORMAT = "{$not: %s}";
	private static final String CONTAINS_QUERY_FORMAT = "{\"%s\": \"*%s*\"}";
	private static final String EQUALS_QUERY_FORMAT = "{\"%s\": %s}";
	private static final String GREATER_QUERY_FORMAT = "{\"%s\": {$gt: %s} }";
	private static final String LESSER_QUERY_FORMAT = "{\"%s\": {$lt: %s} }";
	private static final String KEY_EXISTS_QUERY_FORMAT = "{\"" + _ID +"\": \"%s\", \"%s\": {$exists: true}}";

	// *** VARIABLES *** //
	
	protected MongoClient mongoClient;
	protected DB db;
	public static final String APP_DATA_COLL_FORMAT = "app%sdata";
	Geolocation geo;

	private BasicDBObject dataProjection = null; 	
	
	public ModelAbstract() {
		try {
			mongoClient = new MongoClient(Const.getMongoServer(), Const.getMongoPort());
			db = mongoClient.getDB(Const.getMongoDb());
		} catch (UnknownHostException e) {
			Log.error("", this, "DocumentModel", "Unknown Host.", e); 
		}
	}

	
	// *** PROTECTED *** //

	protected DBCollection getAppCollection(String appId) {
		return db.getCollection(String.format(APP_DATA_COLL_FORMAT, appId));
	}
	
	public String getDocumentPath(List<String> path) {
		if (path == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size(); i++)
			sb.append(path.get(i)).append('.');
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	protected String getDocumentParentPath(List<String> path) {
		if (path == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size() - 1; i++)
			sb.append(path.get(i)).append('.');
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
				sb.append(path.get(i)).append('.');
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
	
	protected BasicDBObject getDataProjection() {
		if (dataProjection == null) {
			dataProjection = new BasicDBObject();
			dataProjection.append(_ID, ZERO);
			dataProjection.append(_USER_ID, ZERO);
			dataProjection.append(_PARENT_PATH, ZERO);
			dataProjection.append(_KEY, ZERO);
		}
		return dataProjection;
	}

	
	// *** CREATE *** //

	public Boolean insertDocumentInPath(String appId, String userId, List<String> path, JSONObject data) throws JSONException {
		deleteDocumentInPath(appId, userId, path);
		return insertDocument(appId, userId, path, data) && updateAscendents(appId, userId, path, data);
	}
	
	private Boolean insertDocument(String appId, String userId, List<String> path, JSONObject data) throws JSONException{
		try{ 
			Iterator<?> it = data.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = data.get(key);
				if (value instanceof JSONObject) {
					List<String> newPath = new ArrayList<String>();
					newPath.addAll(path);
					newPath.add(key);
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
		DBCollection coll = getAppCollection(appId);
		String id = getDocumentId(userId, path);
		JSONObject  data = new JSONObject(value.toString());
		data.put(_KEY, getDocumentKey(path));
		data.put(_ID, id);
		if (userId == null)  userId = DATA;
		data.put(_USER_ID, userId);
		data.put(_PARENT_PATH, getDocumentParentPath(path));
		DBObject dbData = (DBObject)JSON.parse(data.toString());
		if(!getDocumentId(userId, path).equals(userId) || !existsDocument(appId, userId, path))
			coll.insert(dbData);
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
					List<String> newPath = new ArrayList<String>();
					newPath.addAll(path);
					newPath.add(key);
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
	
	private Boolean updateDocumentValue(String appId, String id, String key, Object value) throws JSONException{
		DBCollection coll = getAppCollection(appId);
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
		if (path!= null && path.size() > 0) path.remove(path.size() -1);
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
				List<String> newPath = new ArrayList<String>();
				newPath.addAll(path);
				newPath.remove(newPath.size() - 1);
				id = getDocumentId(userId, newPath);
				DBCollection coll = getAppCollection(appId);
				BasicDBObject ob = new BasicDBObject();
				BasicDBObject dbRemove = new BasicDBObject();
				ob.append(key, "");
				dbRemove.append("$unset", ob);
				BasicDBObject dbQuery = new BasicDBObject();
				dbQuery.append(_ID, id); 		
				coll.update(dbQuery, dbRemove);
				removeKeyFromAscendents(appId, userId, "", newPath);
			}
			catch (Exception e) {
				Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
				return false;
			}
			
		}
		return true;
	}
	
	private Boolean deleteDocument(String appId, String id) {
		DBCollection coll = getAppCollection(appId);
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
	
	public Boolean removeKeyFromAscendents(String appId, String userId, String key, List<String> path) throws JSONException {
		if (path == null || path.size() <= 0) return true;
		String auxKey = getDocumentKey(path);
		key = auxKey + ((key == null || "".equals(key)) ? "" : "." + key);
		path.remove(path.size() - 1);
		String id = getDocumentId(userId, path); 
		
		DBCollection coll = getAppCollection(appId);
		BasicDBObject ob = new BasicDBObject();
		BasicDBObject dbRemove = new BasicDBObject();
		ob.append(key, "");
		dbRemove.append("$unset", ob);
		BasicDBObject dbQuery = new BasicDBObject();
		dbQuery.append(_ID, id); 		
		coll.update(dbQuery, dbRemove);
		
		return removeKeyFromAscendents(appId, userId, key, path);
	}
	
	// *** GET LIST *** //

	public List<String> getDocuments(String appId, String path, JSONObject query, String orderType) throws Exception {
		String strParentPathQuery = getParentPathQueryString(path);
		String strQuery = getQueryString(appId, path, query, orderType);
		String searchQuery = getAndQueryString(strParentPathQuery, strQuery); 
		DBCollection coll = getAppCollection(appId);     //searchQuery="{\"_parentPath\": \"a\", x: 1}"
		DBObject queryObj = (DBObject)JSON.parse(searchQuery);
		BasicDBObject projection = new BasicDBObject();
		//XPTO: na projection só deve vir o ID jm
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
			if(query.has(OperatorEnum.oper.toString())){
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
			}else
				return query.toString();
		} else {
			return null;
		}
	}
	
	private static String getParentPathQueryString(String path) {
		return String.format(PARENT_PATH_QUERY_FORMAT, path);
	}
	
	private static String getAndQueryString(String oper1, String oper2) {
		if(oper1!=null){
			if (oper1.startsWith("{")) oper1 = oper1.substring(1);
			if (oper1.endsWith("}")) oper1 = oper1.substring(0, oper1.length() - 1);
			if(oper1.contains("null"))
				oper1 = oper1.replace("null", "");
		}
		if(oper2!=null){
			if (oper2.startsWith("{")) oper2 = oper2.substring(1);
			if (oper2.endsWith("}")) oper2 = oper2.substring(0, oper2.length() - 1);
		}else
			oper2="";
		return String.format(AND_QUERY_FORMAT, oper1, oper2);
	}
	
	private static String getOrQueryString(String oper1, String oper2) {
		return String.format(OR_QUERY_FORMAT, oper1, oper2);
	}

	private static String getNotQueryString(String oper1) {
		return String.format(NOT_QUERY_FORMAT, oper1);
	}
	
	private static String getOperationQueryString(OperatorEnum oper, String attribute, String value) {
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
	
	private static String getKeyExists(String id, String key) {
		return String.format(KEY_EXISTS_QUERY_FORMAT, id, key);
	}


	// *** GET *** //

	public Object getDocumentInPath(String appId, String userId, List<String> path) throws JSONException {
		String id = getDocumentId(userId, path);
		DBCollection coll = getAppCollection(appId);
		//XPTO alterar a query para pesquisa _id = id
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(_ID, id);
		BasicDBObject projection = getDataProjection();
		DBCursor cursor = coll.find(searchQuery, projection);
		if (cursor.hasNext()) {
			return cursor.next();
		} else if (path != null && path.size() > 0) {
			String key = getDocumentKey(path);
			List<String> newPath = new ArrayList<String>();
			newPath.addAll(path);
			newPath.remove(path.size() - 1);
			return ((DBObject)getDocumentInPath(appId, userId, newPath)).get(key);
			//XPTO: pesquisar se existe um document com _id=id e que key exists
		}
		return null;
	}
	
	private List<DBObject> getDocumentAndChilds(String appId, String path) {		
		List<DBObject> res = new ArrayList<DBObject>();
		try {
			DBCollection coll = getAppCollection(appId);
			BasicDBObject regexQuery = new BasicDBObject();
			regexQuery.append("$regex", path + "*");
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append(_ID, regexQuery);
			BasicDBObject projection = new BasicDBObject();
			projection.append(_ID, 1);
			DBCursor cursor = coll.find(dbQuery, projection);
			res = cursor.toArray();
		}catch (Exception e) {
			Log.error("", this, "getDocumentAndChilds", "Error quering mongoDB.", e);
		}
		return res;
	}

	
	// *** EXISTS *** //

	public Boolean existsDocument(String appId, String userId, List<String> path) {
		return existsNode(appId, getDocumentId(userId, path)) ||
				existsKey(appId, userId, path);
	}

	private Boolean existsNode(String appId, String id) {
		DBCollection coll = getAppCollection(appId);
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

	private Boolean existsKey(String appId, String userId, List<String> path) {
		if (path == null || path.size() <= 0) return false;
		DBCollection coll = getAppCollection(appId);
		String key = getDocumentKey(path);
		List<String> newPath = new ArrayList<String>();
		newPath.addAll(path);
		newPath.remove(newPath.size() - 1);
		String id = getDocumentId(userId, newPath);
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
