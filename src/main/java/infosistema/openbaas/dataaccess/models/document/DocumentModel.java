package infosistema.openbaas.dataaccess.models.document;

import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.model.ModelEnum;
import infosistema.openbaas.utils.Const;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;


import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
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
public class DocumentModel implements DocumentInterface {

	private MongoClient mongoClient;
	private DB db;
	public static final String APP_COLL_FORMAT = "app%sData";
	public static final String UserDataColl = "users:data";
	Geolocation geo;
	
	public DocumentModel() {
		try {
			mongoClient = new MongoClient(Const.MONGO_SERVER, Const.MONGO_PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongoClient.getDB("openbaas");
	}

	// *** PRIVATE *** //
	
	private DBCollection getAppCollection(String appId) {
		return db.getCollection(String.format(APP_COLL_FORMAT, appId));
	}
	
	/*
	//returns the document id based on path
	private String getDocumentId(DBCollection coll, String path) {
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(path, "{$exists:true}");
		BasicDBObject projection = new BasicDBObject();
		projection.append(path, 1);
		DBCursor cursor = coll.find(searchQuery,projection);
		while(cursor.hasNext()){
			DBObject dbObj = cursor.next();
			if(!dbObj.get("_id").equals(""))
				return (String) dbObj.get("_id");
		}
		return null;
	}
	*/
	
	
	
	// *** CREATE *** //
	// criar nó da arvore se nao existe raiz (data ou userId)
	private boolean checkPath(DBCollection coll, String appId, String path, String userId) throws JSONException {
		//se nao existir raiz faz um insert
		//db.appTeste.insert({"userId || appId":{}})
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
			return false;
		}
		return true;
	}
	
	// *** PUT *** //
	public Boolean insertDocumentInPath(String appId, String userId, String path, JSONObject data) throws JSONException{
		
		//db.appTeste.update({"f":{$exists:1}},{$set:{"zzzz.f.e.r.t":{"h":4}}})
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
		}catch (Exception e){
			System.err.println(e.toString());
			return false;
		}
		return true;
	}
	
	// *** PATCH *** //
	public Boolean updateDocumentInPath(String appId, String userId, String path, JSONObject data) throws JSONException{
		//db.appTeste.update({"restaurante1":{$exists:1}},{$set:{"restaurante1.nome.nome3":"33311111to"}})
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
		}catch (Exception e){
			System.err.println(e.toString());
			return false;
		}
		return true;
	}
	
	
	// *** DELETE *** //
	public Boolean deleteDocumentInPath(String appId, String path) throws JSONException{
		//db.collection_name.update({ _id: 1234 }, { $unset : { description : 1} });
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
		}catch (Exception e){
			System.err.println(e.toString());
			return false;
		}
		return true;
	}
	
	
	// *** GET *** //
	//XPTO: eu acho que isto devia devolver um jason, mas é preciso ver o que fazem as funções que chamam isto
	@Override
	public String getDocumentInPath(String appId, String userId, String path) {
		//db.appTeste.find({"restaurante1.nome":{$exists:true}},{"restaurante1.nome":1,"_id":0})
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
	//XPTO: verificar se a path exist
	@Override
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

	
	
/*
	
	//XPTO antes de inserir os elementos que vêm lá apagar todos os que estão. O insert apaga tudo
	public boolean insertDocumentInPath(String appId, String path, String userId, JSONObject data, String location) throws JSONException {
		DBCollection coll = getAppCollection(appId);
		
		//obter o primeiro no
		//se não existir inser
		
		Iterator<?> it = data.keys();
		while (it.hasNext()) {
			String key = (String) it.next();
			BasicDBObject searchQuery = new BasicDBObject();
			searchQuery.append("_id", userId + ": " + key);
			DBCursor cursor = coll.find(searchQuery);

			BasicDBObject temp = new BasicDBObject();
			if (userId != null) 
				temp.append("_id", userId + ":" + key);
			temp.append("data", (String)data.get(key));
			if (location != null){
				String[] splitted = location.split(":");
				geo = Geolocation.getInstance();
				geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), ModelEnum.data, appId, path);
			}
			if (!cursor.hasNext())
				coll.insert(temp);
			else {
				BasicDBObject newDoc = new BasicDBObject();
				newDoc.append("_id", userId + ":" + key);
				newDoc.append("data", (String) data.get(key));
				//newDoc.put("path", tempURL);
				if (location != null)
					newDoc.append("location", location);
				coll.update(searchQuery, newDoc);
			}
			it.remove();
			//tempURL = appId;
		}

		return true;
	}

	/*
	public boolean insertUserDocumentRoot(String appId, String userId, JSONObject data, String location) throws JSONException {
		DBCollection coll = getAppCollection(appId);
		String tempURL = appId + ",users," + userId;
		Iterator<?> it = data.keys();// iterate the new content and
		// make it accessible
		while (it.hasNext()) {
			String key = (String) it.next();
			tempURL = appId + ",users," + userId;
			BasicDBObject searchQuery = new BasicDBObject();
			searchQuery.append("_id", userId + ":" + key);
			DBCursor cursor = coll.find(searchQuery);

			BasicDBObject temp = new BasicDBObject();
			temp.append("_id", userId + ":" + key);
			temp.append("data", (String) data.get(key));
			tempURL += "," + key;
			temp.append("path", tempURL);
			if (location != null)
				temp.append("location", location);
			if (!cursor.hasNext())
				coll.insert(temp);
			else {
				BasicDBObject newDoc = new BasicDBObject();
				newDoc.append("_id", userId + ":" + key);
				newDoc.append("data", (String) data.get(key));
				newDoc.put("path", tempURL);
				if (location != null)
					newDoc.append("location", location);
				coll.update(searchQuery, newDoc);
			}
			it.remove();
			tempURL = appId;
		}
		return true;
	}
	*/

	//XPTO: PARA QUE SERVE ISTO????
	/*
	@Override
	public boolean createNonPublishableUserDocument(String appId,
			String userId, JSONObject data, String path, String location) {
		DBCollection coll = db.getCollection(UserDataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", path);
		DBCursor cursor = coll.find(searchQuery);
		boolean sucess = false;
		if (cursor.hasNext()) {// update the data inside the doc
			BasicDBObject newDocument = new BasicDBObject();
			if (location != null)
				newDocument.append(
						"$set",
						new BasicDBObject().append("data",
								specialCharacter + data.toString()).append(
								"location", location)); // ~data
			else
				newDocument.append(
						"$set",
						new BasicDBObject().append("data", specialCharacter
								+ data.toString())); // ~data
			coll.update(searchQuery, newDocument);
			sucess = true;
		} else {// create the element and insert data
			String[] arrayUrl = path.split(",");
			BasicDBObject temp = new BasicDBObject();
			temp.put("_id", arrayUrl[arrayUrl.length - 1]);
			temp.put("data", specialCharacter + data.toString());
			if (location != null)
				temp.append("location", location);
			coll.insert(temp);
			sucess = true;
		}
		return sucess;
	}
	*/
	
	//XPTO: PARA QUE SERVE ISTO????
	/*
	@Override
	public boolean createNonPublishableDocument(String appId, JSONObject data, String path, String location) {
		DBCollection coll = getAppCollection(appId);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", path);
		DBCursor cursor = coll.find(searchQuery);
		boolean sucess = false;
		if (cursor.hasNext()) {// update the data inside the doc
			BasicDBObject newDocument = new BasicDBObject();
			if (location != null)
				// ~data
				newDocument.append("$set",new BasicDBObject().append("data",specialCharacter + data.toString()).append("location", location));
			else
				// ~data
				newDocument.append(	"$set",	new BasicDBObject().append("data", specialCharacter	+ data.toString())); 
			coll.update(searchQuery, newDocument);
			sucess = true;
		} else {// create the element and insert data
			String[] arrayUrl = path.split(",");
			BasicDBObject temp = new BasicDBObject();
			temp.append("_id", arrayUrl[arrayUrl.length - 1]);
			temp.append("data", specialCharacter + data.toString());
			if (location != null)
				temp.append("location", location);
			coll.insert(temp);
			sucess = true;
		}
		return sucess;
	}
	*/
	

	// *** UPDATE *** //
/*
	//XPTO para cada elemento que vem no jason se existir substitui se não existir insere
	public boolean updateDocumentInPath(String appId, String userId, String inPath, JSONObject data, String location) throws JSONException {
        DBCollection coll = db.getCollection(UserDataColl);
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.put("path", inPath);
        Iterator<?> keys = data.keys();
        String tempURL = appId + ",users," + userId; // appId
        while (keys.hasNext()) {
                String element = (String) keys.next();
                if (element.equalsIgnoreCase("data")) {// update the content of the // url
                        BasicDBObject newDocument = new BasicDBObject().append("$set", new BasicDBObject().append("data", data.toString()));
                        coll.update(searchQuery, newDocument);
                        break;
                }
                tempURL += "," + element;
                insertIntoUserDocumentRec(coll, appId, userId, tempURL, data, location, element);
        }
        return true;
    }
	
	//XPTO Isto é para apagar deixei para se ver como estava 
	private void patchDataInElementRec(DBCollection coll, String appId, String tempURL,
			JSONObject inputJson, String location, String element) {
		JSONObject childs = null;
		//String value = null;
		String child = null;
		try {
			childs = (JSONObject) inputJson.get(element);
			Iterator<?> childsIt = childs.keys();
			if (childsIt.hasNext()) {
				child = (String) childsIt.next();
				tempURL += "," + child;
				patchDataInElementRec(coll, appId, tempURL, (JSONObject) childs, location, child);
			}
		} catch (JSONException e) {// Java 7 allows multiple exception catches
									// but
			// < 7 doesn't!
			try {
				inputJson.get(element);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		} catch (ClassCastException e1) {
			try {
				inputJson.get(element);
			} catch (JSONException e2) {
				e2.printStackTrace();
			}
		}
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", Pattern.compile(tempURL));
		DBCursor cursor = coll.find(searchQuery);
		if (cursor.hasNext()) {
			BasicDBObject newDocument = null;
			if (location != null){
				newDocument = new BasicDBObject().append("$set",new BasicDBObject().append("data", inputJson.toString()).append("location", location));
			String[] splitted = location.split(":");
			geo.insertObjectInGrid(Double.parseDouble(splitted[0]), Double.parseDouble(splitted[1]), ModelEnum.data, appId, tempURL);
		}
			else
				newDocument = new BasicDBObject()
						.append("$set",new BasicDBObject().append("data",inputJson.toString()));
			coll.update(searchQuery, newDocument);
		} else {
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.append("path", tempURL);
			newDocument.append("data", inputJson.toString());
			if (location != null)
				newDocument.append("location", location);
			coll.insert(newDocument);
		}

	}

	//private
	//XPTO Isto é para apagar deixei para se ver como estava 
	private void insertIntoUserDocumentRec(DBCollection coll, String appId, String userId, String tempURL,
			JSONObject data, String location, String element) {
		JSONObject childs = null;
		//String value = null;
		try {
			childs = (JSONObject) data.get(element);
			Iterator<?> childsIt = childs.keys();
			if (childsIt.hasNext()) {
				String child = (String) childsIt.next();
				tempURL += "," + child;
				patchDataInElementRec(coll, appId, tempURL, (JSONObject) childs, location, child);
			}
			//Java 7 allows multiple exception catches but version < 7 doesn't
		} catch (JSONException e) {
			try {
				data.get(element);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		} catch (ClassCastException e1) {
			try {
				data.get(element);
			} catch (JSONException e2) {
				e2.printStackTrace();
			}
		}
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("path", Pattern.compile(tempURL));
		DBCursor cursor = coll.find(searchQuery);
		if (cursor.hasNext()) {
			BasicDBObject newDocument = null;
			if (location != null)
				newDocument = new BasicDBObject().append(
						"$set",
						new BasicDBObject()
								.append("data", data.toString()).append(
										"location", location));
			else
				newDocument = new BasicDBObject()
						.append("$set",
								new BasicDBObject().append("data",
										data.toString()));
			coll.insert(searchQuery, newDocument);
		} else {
			BasicDBObject newDocument = new BasicDBObject();
			newDocument.put("path", tempURL);
			newDocument.put("data", data.toString());
			if (location != null)
				newDocument.append("location", location);
			coll.insert(newDocument);
		}
		
	}

*/
	// *** GET LIST *** //
/*
	//XPTO: Substituir isto tudo (as 4 funções) por um search. É para deixar para o fim.
	@Override
	public String getAllDocInApp(String appId) {
		DBCollection coll = getAppCollection(appId);
		String allDoc = "";
		BasicDBObject query = new BasicDBObject();
		query.put("path", Pattern.compile(appId));
		DBCursor cursor = coll.find(query);
		while (cursor.hasNext()) {
			DBObject obj = cursor.next();
			allDoc += obj.toString();
		}
		return allDoc;
	}

	@Override
	public ArrayList<String> getAllDocsInRadius(String appId, double latitude, double longitude, double radius) {
		DBCollection coll = getAppCollection(appId);
		ArrayList<String> all = geo.getObjectsInDistance(latitude, longitude, radius, appId, ModelEnum.data);
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

	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId, double latitude, double longitude,
			double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType){
		//TODO JM Tem GeoLocalizacao. Ver como se faz a paginacao
		DBCollection coll = db.getCollection(UserDataColl);
		ArrayList<String> all = geo.getObjectsInDistance(latitude, longitude, radius, appId, ModelEnum.data);
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

	@Override
	public String getAllUserDocs(String appId, String userId) {
		DBCollection coll = db.getCollection(UserDataColl);
		String allDoc = "";
		BasicDBObject query = new BasicDBObject();
		query.put("path", Pattern.compile(appId + ",users," + userId));
		DBCursor cursor = coll.find(query);
		while (cursor.hasNext()) {
			DBObject obj = cursor.next();
			allDoc += obj.toString();
		}
		return allDoc;
	}

	
	*/
	// *** OTHERS *** //
	
	//XPTO: isto é para apagar daqui. isto terá que ir para o geolocation
	@Override
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
	@Override
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
