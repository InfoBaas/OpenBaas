package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

	private static final String _PARENT_PATH = "_parentPath"; 
	private static final String _ID = "_id"; 
	private static final String _KEY = "_key";
	private static final String _USER_ID = "_userId";
	private static final String DATA = "data";

	private static final String PARENT_PATH_QUERY_FORMAT = "{parentPath: %s}";
	private static final String AND_QUERY_FORMAT = "{%s, $s}";
	private static final String OR_QUERY_FORMAT = "{ $or: [%s, %s]}}";
	private static final String NOT_QUERY_FORMAT = "{$not: %s}";
	private static final String CONTAINS_QUERY_FORMAT = "{%s: *%s*}";
	private static final String EQUALS_QUERY_FORMAT = "{%s: %s}";
	private static final String GREATER_QUERY_FORMAT = "{%s: {$gt: %s} }";
	private static final String LESSER_QUERY_FORMAT = "{%s: {$lt: %s} }";;

	// *** VARIABLES *** //
	
	private MongoClient mongoClient;
	private DB db;
	public static final String APP_COLL_FORMAT = "app%sData";
	public static final String UserDataColl = "users:data";
	Geolocation geo;
	
	public ModelAbstract() {
		try {
			mongoClient = new MongoClient(Const.getMongoServer(), Const.getMongoPort());
		} catch (UnknownHostException e) {
			Log.error("", this, "DocumentModel", "Unknown Host.", e); 
		}
		db = mongoClient.getDB(Const.getMongoDb());
	}

	
	// *** PROTECTED *** //

	protected DBCollection getAppCollection(String appId) {
		return db.getCollection(String.format(APP_COLL_FORMAT, appId));
	}
	
	protected String getDocumentParentPath(List<String> path) {
		if (path == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size() - 1; i++)
			sb.append(path.get(i)).append('.');
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	protected String getDocumentId(String userId, List<String> path) {
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


	// *** CREATE *** //

	public Boolean insertDocumentInPath(String appId, String userId, List<String> path, JSONObject data) throws JSONException {
		deleteDocumentInPath(appId, userId, path);
		return insertDocument(appId, userId, path, data) && updateAscendents(appId, userId, path, data);
	}
	
	private Boolean insertDocument(String appId, String userId, List<String> path, JSONObject data) throws JSONException{
		DBCollection coll = getAppCollection(appId);
		//List<String> keysToRemove = new ArrayList<String>();
		try{ 
			Iterator<?> it = data.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = data.get(key);
				if (value instanceof JSONObject) {
					//keysToRemove.add(key);
					List<String> newPath = new ArrayList<String>();
					newPath.addAll(path);
					newPath.add(key);
					insertDocument(appId, userId, newPath, (JSONObject)value);
				}
			}
			//for(String key: keysToRemove) {
				//data.remove(key);
			//}
			data.put(_KEY, getDocumentKey(path));
			data.put(_ID, getDocumentId(userId, path));
			if (userId == null)  userId = DATA;
			data.put(_USER_ID, userId);
			data.put(_PARENT_PATH, getDocumentParentPath(path));
			DBObject dbData = (DBObject) JSON.parse(data.toString());
			if(!getDocumentId(userId, path).equals(userId))
				coll.insert(dbData);
		} catch (Exception e) {
			Log.error("", this, "insertDocumentInPath", "An error ocorred.", e);
			return false;
		}
		return true;
	}

	// *** UPDATE *** //
	
	public Boolean updateDocumentInPath(String appId, String userId, List<String> path, JSONObject data) throws JSONException{
		try{
			String id = getDocumentId(userId, path);
			if (!existsDocument(appId, userId, path)) 
				return insertDocument(appId, userId, path, data);
			Iterator<?> it = data.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = data.get(key);
				if (value instanceof JSONObject) {
					List<String> newPath = new ArrayList<String>();
					newPath.addAll(path);
					newPath.add(key);
					if(existsDocument(appId, id+"."+key)){
						deleteDocument(appId, id+"."+key);
					}
					insertDocument(appId, userId, newPath, (JSONObject)value);
				} else {
					/*if (!existsDocument(appId, id)) {
						//Caso nao se insiram os documentos vazios inserir um documento vazio para actualizar a key
					}*/
					updateDocumentValue(appId, id, key, value);
				}
			}
			updateAscendents(appId, userId, path, data);
		} catch (Exception e) {
			Log.error("", this, "updateDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return true;
	}
	
	private Boolean updateDocumentValue(String appId, String id, String key, Object value) throws JSONException{
		DBCollection coll = getAppCollection(appId);
		try{
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
	
	private Boolean updateAscendents(String appId, String userId, List<String> path, JSONObject data) throws JSONException{
		if (path == null || path.size() <= 0) return true;
		String key = getDocumentKey(path);
		path.remove(path.size() -1);
		String id = getDocumentId(userId, path);
		updateDocumentValue(appId, id, key, data);
		return updateAscendents(appId, userId, path, (JSONObject)getDocumentInPath(appId, userId, path));
	}
	
	
	// *** DELETE *** //
	
	public Boolean deleteDocumentInPath(String appId, String userId, List<String> path) throws JSONException{
		
		try{
			String id = getDocumentId(userId, path);
			List<DBObject> listToDel = getDocumentAndChilds(appId, id);		
			Iterator<DBObject> itDoc = listToDel.iterator();
			while(itDoc.hasNext()){
				DBObject doc = itDoc.next();
				deleteDocument(appId, doc.get(_ID).toString());
				removeKeyFromAscendents(appId, userId, "", path);
			}
		}
		catch (Exception e) {
			Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
			return false;
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
			Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return true;		
	}
	
	public Boolean removeKeyFromAscendents(String appId, String userId, String key, List<String> path) throws JSONException {
		if (path == null || path.size() <= 0) return true;
		String auxKey = getDocumentKey(path);
		key = auxKey + ((key == null || "".equals(key)) ? "" : "." + key);
		path.remove(path.size() -1);
		String id = getDocumentId(userId, path);
		
		Boolean aux = deleteDocument(appId, id);
		if(aux)
			return removeKeyFromAscendents(appId, userId, key, path);
		else 
			return false;
	}
	
	// *** GET LIST *** //

	public List<String> getDocuments(String appId, String path, JSONObject query, String orderType) throws Exception {
		String strParentPathQuery = getParentPathQueryString(path);
		String strQuery = getQueryString(appId, path, query, orderType);
		String searchQuery = getAndQueryString(strParentPathQuery, strQuery); 
		DBCollection coll = getAppCollection(appId);
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
	
	private static String getParentPathQueryString(String path) {
		return String.format(PARENT_PATH_QUERY_FORMAT, path);
	}
	
	private static String getAndQueryString(String oper1, String oper2) {
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


	// *** GET *** //

	public Object getDocumentInPath(String appId, String userId, List<String> path) {
		String id = getDocumentId(userId, path);
		DBCollection coll = getAppCollection(appId);
		//XPTO alterar a query para pesquisa _id = id
		BasicDBObject existsQuery = new BasicDBObject();
		existsQuery.append("$exists", true);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(id, existsQuery);
		BasicDBObject projection = new BasicDBObject();
		projection.append(id, "\"_id\":0");
		DBCursor cursor = coll.find(searchQuery, projection);
		if (cursor.hasNext()) {
			return cursor.next();
		} else {
			String key = getDocumentKey(path);
			List<String> newPath = new ArrayList<String>();
			newPath.addAll(path);
			newPath.remove(path.size() -1);
			id = getDocumentId(userId, newPath);
			//XPTO: pesquisar se existe um document com _id=id e que key exists
		}
		return null;
	}
	
	private List<DBObject> getDocumentAndChilds(String appId, String path) {		
		List<DBObject> res = new ArrayList<DBObject>();
		try {
			DBCollection coll = getAppCollection(appId);
			BasicDBObject regexQuery = new BasicDBObject();
			regexQuery.append("$regex", path+"\\.");
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append(_ID, regexQuery);
			DBCursor cursor = coll.find(dbQuery);
			res = cursor.toArray();
		}catch (Exception e) {
			Log.error("", this, "getDocumentAndChilds", "Error quering mongoDB.", e);
		}
		return res;
	}

	
	// *** EXISTS *** //

	public Boolean existsDocument(String appId, String userId, List<String> path) {
		DBCollection coll = getAppCollection(appId);
		try {
			String id = getDocumentId(userId, path);
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append("_id", id); 		
			BasicDBObject dbProjection = new BasicDBObject();
			dbProjection.append("_id", 1);
			DBCursor cursor = coll.find(dbQuery, dbProjection).limit(1);
			if(cursor.hasNext())
				return true;
			else 
				return false;
		}catch (Exception e) {
			Log.error("", this, "existsDocument", "Error quering mongoDB.", e);
			return false;
		}
	}

	private Boolean existsDocument(String appId, String id) throws JSONException{
		DBCollection coll = getAppCollection(appId);
		try{
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append("_id", id); 		
			BasicDBObject dbProjection = new BasicDBObject();
			dbProjection.append("_id", 1);
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
	
}
