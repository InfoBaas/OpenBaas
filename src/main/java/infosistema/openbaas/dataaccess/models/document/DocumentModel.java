package infosistema.openbaas.dataaccess.models.document;

import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.model.ModelEnum;

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
	public static final String APP_COLL_FORMAT = "app_%s";

	public static final String SERVER = "localhost";
	public static final String UserDataColl = "users:data";
	public static final int PORT = 27017;
	private static final String specialCharacter = "~";
	Geolocation geo;
	
	public DocumentModel() {
		mongoClient = null;
		geo = Geolocation.getInstance();
		try {
			mongoClient = new MongoClient(SERVER, PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongoClient.getDB("openbaas");
	}

	// *** PRIVATE *** //
	
	private DBCollection getAppCollection(String appId) {
		return db.getCollection(String.format(APP_COLL_FORMAT, appId));
	}
	
	
	// *** CREATE *** //
	
	//XPTO antes de inserir os elementos que vêm lá apagar todos os que estão. O insert apaga tudo
	public boolean insertDocumentInPath(String appId, String userId, String inPath, JSONObject data, String location) throws JSONException {
		DBCollection coll = getAppCollection(appId);
		Iterator<?> it = data.keys();// iterate the new content and
		// make it accessible
		while (it.hasNext()) {
			String path = "";
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


	// *** GET LIST *** //

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

	
	// *** GET *** //

	//XPTO: eu acho que isto devia devolver um jason, mas é preciso ver o que fazem as funções que chamam isto
	@Override
	public String getDocumentInPath(String appId, String userId, String path) {
		DBCollection coll = getAppCollection(appId);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", path);
		DBCursor cursor = coll.find(searchQuery);
		DBObject obj = null;
		String data = null;
		obj = cursor.next();
		data = (String) obj.get("data");
		return data;
	}

	
	// *** DELETE *** //

	//XPTO: eliminar o doc que está na path
	@Override
	public boolean deleteDocumentInPath(String appId, String userId, String path) {
		String[] array = path.split("/");
		DBCollection coll = getAppCollection(appId);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("path", Pattern.compile(array[array.length - 1]));
		DBCursor cursor = coll.find(searchQuery);
		while (cursor.hasNext()) {
			coll.remove(cursor.next());
		}
		return true;
	}

	
	// *** EXISTS *** //
	
	//XPTO: verificar se a path exist
	@Override
	public boolean existsDocumentInPath(String appId, String userId, String path) {
		DBCollection coll = getAppCollection(appId);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("path", path);
		DBCursor cursor = coll.find(searchQuery);
		boolean exists = false;
		if (cursor.hasNext())
			exists = true;
		return exists;
	}

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
