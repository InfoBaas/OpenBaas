package infosistema.openbaas.dataaccess.models;

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
import com.mongodb.QueryBuilder;
import com.mongodb.util.JSON;

/*MongoDB java driver has quite a few important things that are not easilly found. 
 * Whenever you want to get the descendants or do the "like" shell option you need to turn it into a pattern
 * e.g
 * BasicDBObject query = new BasicDBObject();
 query.put("path", Pattern.compile("b"));
 DBCursor cursor = coll.find(query);

 cursor now has all the descendants of b.

 * Notes:
 * This is the same database as the one found in MongoDBDataModel, we are splitting data in a new class due to
 * its unique properties.
 */
public abstract class ModelAbstract {

	// *** CONSTANTS *** //

	private static final String _PATH = "_path"; 
	private static final String _ID = "_id"; 
	private static final String _KEY = "_key"; 
	
	
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
	
	protected String getDocumentPath(List<String> path) {
		if (path == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size() - 1; i++)
			sb.append(path.get(i)).append('.');
		sb.deleteCharAt(sb.length()-1);
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
		return insertDocument(appId, userId, path, data);
	}
	
	private Boolean insertDocument(String appId, String userId, List<String> path, JSONObject data) throws JSONException{
		DBCollection coll = getAppCollection(appId);
		try{
			Iterator<?> it = data.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = data.get(key);
				if (value instanceof JSONObject) {
					data.remove(key);
					List<String> newPath = new ArrayList<String>();
					newPath.addAll(path);
					newPath.add(key);
					insertDocument(appId, userId, newPath, (JSONObject)value);
				}
			}
			data.append(_KEY, getDocumentKey(path));
			data.append(_ID, getDocumentId(userId, path));
			data.append(_PATH, getDocumentPath(path));
			DBObject dbData = (DBObject) JSON.parse(data.toString());
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
					insertDocument(appId, userId, newPath, (JSONObject)value);
				} else { 
					if (!existsDocument(appId, id)) {
						//XPTO Inserir um documento vazio para actualizar a key
					}
					updateDocumentValue(appId, id, key, value);
				}
			}
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
	
	
	// *** DELETE *** //
	
	public Boolean deleteDocumentInPath(String appId, String userId, List<String> path) throws JSONException{
		
		try{
			String sPath = getDocumentId(userId, path);
			List<DBObject> listToDel = getDocumentAndChilds(appId,sPath);		
			Iterator<DBObject> itDoc = listToDel.iterator();
			while(itDoc.hasNext()){
				DBObject doc = itDoc.next();
				String id= doc.get(_ID).toString();
				deleteDocument(appId, id);
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
			dbRemove.append(_ID, id); //XPTO não era possível fazer dbRemove.append(_ID, "{ $match :" +id + "* }") 
			coll.remove(dbRemove); 
		} catch (Exception e) {
			Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return true;		
	}
	
	
	// *** GET LIST *** //
	
	private List<DBObject> getDocumentAndChilds(String appId, String path) {		
		List<DBObject> res = new ArrayList<DBObject>();
		try {
			DBCollection coll = getAppCollection(appId);
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append(_ID, "{$regex:\""+path+".*}}");
			DBCursor cursor = coll.find(dbQuery);
			res = cursor.toArray();	
		}catch (Exception e) {
			Log.error("", this, "getDocumentAndChilds", "Error quering mongoDB.", e);
		}
		return res;
	}

	public List<String> getOperation(String appId, OperatorEnum oper, String url, String path, String attribute, String value) {
		List<String> listRes = new ArrayList<String>();
		try {
			
			DBCollection coll = getAppCollection(appId);
			BasicDBObject projection = new BasicDBObject();
			projection.append(_ID, 0);
			DBCursor cursor1 = coll.find(new BasicDBObject(),projection);
			while(cursor1.hasNext()){
				DBObject obj = cursor1.next();
				JSONObject json = new JSONObject(obj.toString());
				Iterator<Object> jsonIter = json.keys();
				String mainKey = null;
				if(jsonIter.hasNext())
					mainKey = (String) jsonIter.next();
				if(mainKey!=null){
					DBObject query = getOperDBQuery(oper, value, mainKey+"."+path);
					BasicDBObject findQuery = new BasicDBObject();
					DBCursor cursor2 = coll.find(query);
					if(cursor2.hasNext())
						listRes.add(mainKey+"."+path);
				}
			}
		} catch (JSONException e) {
			Log.error("", this, "getOperation", "Error quering mongoDB.", e);
		}
		return listRes;
	}
	
	private DBObject getOperDBQuery(OperatorEnum oper, String value, String path ) {
		DBObject res = new BasicDBObject();
		try {
			if(oper.equals(OperatorEnum.contains))
				res.put(path, java.util.regex.Pattern.compile(value));
				//res = java.util.regex.Pattern.compile(value);
			if(oper.equals(OperatorEnum.notContains)){
				//TODO
				/*BasicDBList docIds = new BasicDBList();
            	docIds.add(java.util.regex.Pattern.compile(value));
				res = QueryBuilder.start(path).notIn(docIds).get();//"{$nin:/"+value+"/}";*/
			}
			if(oper.equals(OperatorEnum.equals))
				res = QueryBuilder.start(path).is(value).get();
			if(oper.equals(OperatorEnum.diferent))
				res = QueryBuilder.start(path).notEquals(value).get();
			if(oper.equals(OperatorEnum.greater))
				res = QueryBuilder.start(path).greaterThan(value).get();
			if(oper.equals(OperatorEnum.greaterOrEqual))
				res = QueryBuilder.start(path).greaterThanEquals(value).get();
			if(oper.equals(OperatorEnum.lesser))
				res = QueryBuilder.start(path).lessThan(value).get();
			if(oper.equals(OperatorEnum.lesserOrEqual))
				res = QueryBuilder.start(path).lessThanEquals(value).get();
		} catch (Exception e) {
			Log.error("", this, "getOperDBQuery", "Error creating query mongoDB.", e);
		}
		return res;
	}


	// *** GET *** //

	public Object getDocumentInPath(String appId, String userId, List<String> path) {
		//1 - Obter todos os document com _id = id + *
		//Excluir a key _path que não vai ser usadas
		//Sugestão: ordenar pelo id
		String id = getDocumentId(userId, path);
		DBCollection coll = getAppCollection(appId);
		//XPTO alterar a query
		BasicDBObject existsQuery = new BasicDBObject();
		existsQuery.append("$exists", true);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(id, existsQuery);
		BasicDBObject projection = new BasicDBObject();
		projection.append(id, "\"_id\":0");
		//2 - Montar o JSon ou a String para returnar 
		DBCursor cursor = coll.find(searchQuery,projection);
		if (cursor.hasNext()) {
			return mountJson(cursor, id);
		} else {
			if (path.size() <= 0) return null;
			//3 - ver se existe um document que tenha _id = path - last
			List<String> newPath = new ArrayList<String>();
			String key = newPath.get(path.size() -1);
			newPath.addAll(path);
			newPath.remove(path.size() -1);
			id = getDocumentId(userId, newPath);
			//XPTO: procurar o doc com _id = id e com um field com a key
		}
		return null;
	}
	
	private JSONObject mountJson(DBCursor cursor, String id) {
		JSONObject retObj = new JSONObject();
		//XPTO Montar o Json
		//
		return retObj;
	}

	// *** EXISTS *** //

	public Boolean existsDocument(String appId, String userId, List<String> path) {
		try {
			String id = getDocumentId(userId, path);
			DBCollection coll = getAppCollection(appId);
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append(_ID, "{$regex:\""+id+".*}}");
			DBCursor cursor = coll.find(dbQuery);
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
