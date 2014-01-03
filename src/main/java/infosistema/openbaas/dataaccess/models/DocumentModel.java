package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBList;
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
public class DocumentModel {

	private MongoClient mongoClient;
	private DB db;
	public static final String APP_COLL_FORMAT = "app%sData";
	public static final String UserDataColl = "users:data";
	Geolocation geo;
	
	public DocumentModel() {
		try {
			mongoClient = new MongoClient(Const.getMongoServer(), Const.getMongoPort());
		} catch (UnknownHostException e) {
			Log.error("", this, "DocumentModel", "Unknown Host.", e); 
		}
		db = mongoClient.getDB(Const.getMongoDb());
	}

	// *** PRIVATE *** //
	
	private DBCollection getAppCollection(String appId) {
		return db.getCollection(String.format(APP_COLL_FORMAT, appId));
	}
	
	// criar nó da arvore se nao existe raiz (data ou userId)
	//XPTO: Isto não me parece estar bem 
	private boolean checkPath(DBCollection coll, String appId, String path, String userId) throws JSONException {
		//se nao existir raiz faz um insert
		try{
			String[] splitted = path.split("\\.");
			path =  splitted[0];
			if (!existsDocumentInPath(appId, path)){
				DBObject dbObject = null; 
				if(userId == null)
					dbObject =  (DBObject) JSON.parse("{'data':{}}");
				else 
					dbObject =  (DBObject) JSON.parse("{'"+userId+"':{}}");
				coll.insert(dbObject);
			}
		}catch (Exception e){
			Log.error("", this, "checkPath", "An error ocorred.", e); 
			return false;
		}
		return true;
	}
	

	// *** CREATE *** //
	
	public Boolean insertDocumentInPath(String appId, String userId, String path, JSONObject data) throws JSONException{
		DBCollection coll = getAppCollection(appId);
		String[] splitted = path.split("\\.");
		String baseKey =  splitted[0];
		try{
			if(checkPath(coll,appId,path, userId)){
				DBObject dbData = (DBObject) JSON.parse(data.toString());
				BasicDBObject dbQuery = new BasicDBObject();
				dbQuery.append(baseKey, new  BasicDBObject("$exists", true));
				BasicDBObject dbBaseData = new BasicDBObject();
				dbBaseData.append(path, dbData);
				BasicDBObject dbInsert = new BasicDBObject();
				dbInsert.append("$set", dbBaseData);
				coll.update(dbQuery, dbInsert);
			}else 
				return false;
		} catch (Exception e) {
			Log.error("", this, "insertDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return true;
	}
	
	
	// *** UPDATE *** //
	
	public Boolean updateDocumentInPath(String appId, String userId, String path, JSONObject data) throws JSONException{
		DBCollection coll = getAppCollection(appId);
		String[] splitted = path.split("\\.");
		String baseKey =  splitted[0];
		try{
			if(checkPath(coll,appId,path,userId)){
				Iterator<?> it = data.keys();
				while (it.hasNext()) {
					String key = (String) it.next();
					String value = (String) data.get(key);
					//DBObject dbData = (DBObject) JSON.parse(data.toString());
					BasicDBObject dbQuery = new BasicDBObject();
					dbQuery.append(baseKey, new  BasicDBObject("$exists", true));
					BasicDBObject dbBaseData = new BasicDBObject();
					dbBaseData.append(path+"."+key, value);
					BasicDBObject dbInsert = new BasicDBObject();
					dbInsert.append("$set", dbBaseData);
					coll.update(dbQuery, dbInsert);
				}
			}
		} catch (Exception e) {
			Log.error("", this, "updateDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return true;
	}
	
	
	// *** DELETE *** //
	
	public Boolean deleteDocumentInPath(String appId, String path) throws JSONException{
		DBCollection coll = getAppCollection(appId);
		String[] splitted = path.split("\\.");
		String baseKey =  splitted[0];
		try{
			BasicDBObject existsQuery = new BasicDBObject();
			existsQuery.append("$exists", 1);
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append(baseKey, existsQuery);
			BasicDBObject dbBaseData = new BasicDBObject();
			dbBaseData.append(path, 1);
			BasicDBObject dbProjection = new BasicDBObject();
			dbProjection.append("$unset", dbBaseData);
			coll.update(dbQuery, dbProjection);
		} catch (Exception e) {
			Log.error("", this, "deleteDocumentInPath", "An error ocorred.", e); 
			return false;
		}
		return true;
	}
	
	
	// *** GET LIST *** //
	
	public List<String> getOperation(String appId, OperatorEnum oper, String url, String path, String attribute, String value) {
		List<String> listRes = new ArrayList<String>();
		try {
			
			DBCollection coll = getAppCollection(appId);
			BasicDBObject projection = new BasicDBObject();
			projection.append("_id",0);
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
	

	// *** GET *** //

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

	//XPTO: eu acho que isto devia devolver um jason, mas é preciso ver o que fazem as funções que chamam isto
	public String getDocumentInPath(String appId, String userId, String path) {
		String[] splitted = path.split("\\.");
		String keyValue = splitted[splitted.length-1];
		DBCollection coll = getAppCollection(appId);
		BasicDBObject existsQuery = new BasicDBObject();
		existsQuery.append("$exists", true);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(path, existsQuery);
		BasicDBObject projection = new BasicDBObject();
		projection.append(path, "\"_id\":0");
		DBCursor cursor = coll.find(searchQuery,projection);
		if(cursor.hasNext()){
			DBObject obj = cursor.next();
			return (String)obj.get(keyValue);
		}
		return "{}";
	}

	// *** EXISTS *** //

	public boolean existsDocumentInPath(String appId, String path) {
		DBCollection coll = getAppCollection(appId);
		String[] splitted = path.split("\\.");
		String keyValue = splitted[0];
		BasicDBObject existsQuery = new BasicDBObject();
		existsQuery.append("$exists", true);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put(keyValue, existsQuery);
		DBCursor cursor = coll.find(searchQuery);
		boolean exists = false;
		if (cursor.hasNext())
			exists = true;
		return exists;
	}


	// *** OTHERS *** //
	
	//XPTO: isto é para apagar daqui. isto terá que ir para o geolocation
	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude, double longitude, double radius) {
		DBCollection coll = db.getCollection(UserDataColl);
		ArrayList<String> all = geo.getObjectsInDistance(latitude, longitude, radius, appId, ModelEnum.audio);
		Iterator<String> allIt = all.iterator();
		ArrayList<String> allElements = new ArrayList<String>();
		while(allIt.hasNext()){
			String next = allIt.next();
			BasicDBObject query = new BasicDBObject();
			query.put("path", Pattern.compile(next));
			DBCursor cursor = coll.find(query);
			while(cursor.hasNext()){
				DBObject element = cursor.next();
				allElements.add("path: " + element.get("path") + " data: " + element.get("data"));
			}
		}
		return allElements;
	}

	//XPTO: isto é para apagar daqui. isto terá que ir para o geolocation
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,double longitude, 
			double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		DBCollection coll = db.getCollection(UserDataColl);
		ArrayList<String> all = geo.getObjectsInDistance(latitude, longitude, radius, appId, ModelEnum.image);
		Iterator<String> allIt = all.iterator();
		Set<String> allElements = new HashSet<String>();
		while(allIt.hasNext()){
			String next = allIt.next();
			BasicDBObject query = new BasicDBObject();
			query.put("path", Pattern.compile(next));
			DBCursor cursor = coll.find(query);
			while(cursor.hasNext()){
				DBObject element = cursor.next();
				allElements.add("path: " + element.get("path") + " data: " + element.get("data"));
			}
		}
		//return allElements;
		return all;
	}

}
