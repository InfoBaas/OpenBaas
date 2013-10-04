package Document;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import misc.GeoLocationOperations;
import misc.Geolocation;

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
	public static final String SERVER = "localhost";
	public static final String DataColl = "data";
	public static final String UserDataColl = "users:data";
	public static final int PORT = 27017;
	private static final String specialCharacter = "~";
	private static final String AUDIO = "audio";
	private static final String IMAGE = "jpg";
	GeoLocationOperations geo;
	public DocumentModel() {
		mongoClient = null;
		geo = new Geolocation();
		try {
			mongoClient = new MongoClient(SERVER, PORT);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db = mongoClient.getDB("openbaas");
	}

	@Override
	public boolean createDocumentForApplication(String appId) {
		if (this.docExistsForApp(appId))
			return false;
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject data = new BasicDBObject(); //"_id", appId
		data.put("path", null);
		coll.insert(data);
		return true;
	}

	@Override
	public boolean elementExistsInDocument(String url) {
		DBCollection coll = db.getCollection(DataColl);
		String[] array = url.split(",");
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("path", url);
		DBCursor cursor = coll.find(searchQuery);
		boolean exists = false;
		if (cursor.hasNext())
			exists = true;
		return exists;
	}

	public boolean idExistsInTree(String id) {
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", id);
		DBCursor cursor = coll.find(searchQuery);
		boolean exists = false;
		if (cursor.hasNext())
			exists = true;
		return exists;
	}

	public boolean dataExistsForElement(String url) {
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", url);
		searchQuery.append("data", new BasicDBObject("$exists", true));
		DBCursor cursor = coll.find(searchQuery);
		DBObject obj = null;
		boolean sucess = false;
		if (cursor.hasNext())
			sucess = true;
		return sucess;
	}

	@Override
	public String getDataInDocument(String url) {
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", url);
		DBCursor cursor = coll.find(searchQuery);
		DBObject obj = null;
		String data = null;
		obj = cursor.next();
		data = (String) obj.get("data");
		return data;
	}

	@Override
	public boolean insertIntoDocument(String appId, String url,
			JSONObject data, String location) throws JSONException {
		DBCollection coll = db.getCollection(DataColl);
		String[] array = url.split("/");
		String tempURL = appId;
		for (int i = 1; i < array.length; i++) {
			tempURL += "," + array[i];
			if (elementExistsInDocument(tempURL)) { // key already exists delete
													// it and its childs
				BasicDBObject query = new BasicDBObject();
				query.append("path", Pattern.compile(tempURL));
				DBCursor cursor = coll.find(query);
				while (cursor.hasNext()) { // delete childs
					coll.remove(cursor.next());
				}
			}
			// Create the element and its childs
			BasicDBObject obj = new BasicDBObject();
			obj.append("path", tempURL);
			obj.append("data", data.toString());
			if (location != null)
				obj.put("location", location);
			coll.insert(obj);
			Iterator<String> keys = data.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				BasicDBObject temp = new BasicDBObject();
				temp.append("data", data.get(key));
				tempURL += "," + key;
				temp.append("path", tempURL);
				if (location != null){
					temp.append("location", location);
					String[] splitted = location.split(":");
					geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), key, tempURL);
				}
				coll.insert(temp);
				keys.remove();
			}
		}
		return true;
	}

	@Override
	public boolean deleteDataInDocument(String url) {
		String[] array = url.split("/");
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("path", Pattern.compile(array[array.length - 1]));
		DBCursor cursor = coll.find(searchQuery);
		while (cursor.hasNext()) {
			coll.remove(cursor.next());
		}
		return true;
	}

	@Override
	public boolean docExistsForApp(String appId) {
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", appId);
		DBCursor cursor = coll.find(searchQuery);
		boolean sucess = false;
		if (cursor.hasNext())
			sucess = true;
		return sucess;
	}

	@Override
	public boolean updateDataInDocument(String url, String data) {
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", url);

		BasicDBObject newDocument = new BasicDBObject();
		newDocument.append("data", data);
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.append("$set", newDocument);

		coll.update(searchQuery, updateObj);
		return true;
	}

	// /**
	// * DocumentModel method for PATCH HTTP Requests. It partially updates the
	// * object, modifying existing fields and adds the fields not present in
	// the
	// * document.
	// *
	// * @throws JSONException
	// */
	// @Override
	// public String patchDataInElement(String url, JSONObject inputJson,
	// String location) throws JSONException {
	// DBCollection coll = db.getCollection(DataColl);
	// BasicDBObject searchQuery = new BasicDBObject();
	// searchQuery.put("path", url);
	//
	// BasicDBObject newDocument = new BasicDBObject();
	// if (location != null)
	// newDocument.append("$set",
	// new BasicDBObject().append("data", inputJson.toString())
	// .append("location", location));
	// else
	// newDocument.append("$set",
	// new BasicDBObject().append("data", inputJson.toString()));
	// coll.update(searchQuery, newDocument);
	// JSONObject dataJson = null;
	// Iterator it = inputJson.keys();
	// if (it.hasNext()) {
	// String data = (String) it.next();
	// if (data.equals("data")) {
	// dataJson = (JSONObject) inputJson.get(data);
	// Iterator dataIt = dataJson.keys();
	// while (dataIt.hasNext()) { // iterate all the keys inside the
	// // json
	// String key = (String) dataIt.next();
	// BasicDBObject keyQuery = new BasicDBObject().append("_id",
	// key);
	// DBCursor cursor = coll.find(keyQuery);
	// String tempURL = url;
	// if (!cursor.hasNext()) { // Key does not exist in tree,
	// // let's create it
	// BasicDBObject temp = new BasicDBObject();
	// temp.put("_id", key);
	// temp.put("data", dataJson.get(key));
	// tempURL += "," + key;
	// temp.put("path", tempURL);
	// if (location != null)
	// temp.put("location", location);
	// coll.insert(temp);
	// dataIt.remove();
	// } else { // object already exists, let us update it
	// coll.remove(keyQuery);
	// tempURL += "," + key;
	// BasicDBObject newKey = new BasicDBObject()
	// .append("_id", key)
	// .append("data", dataJson.toString())
	// .append("path", tempURL);
	// if (location != null)
	// newKey.put("location", location);
	// coll.insert(newKey);
	// dataIt.remove();
	// }
	// }
	// }
	// }
	//
	// // TODO Auto-generated method stub
	// return dataJson.toString();
	// }
	// This is a recursive attempt to implement the abstract
	// data
	/**
	 * DocumentModel method for PATCH HTTP Requests. It partially updates the
	 * object, modifying existing fields and adds the fields not present in the
	 * document.
	 * 
	 * @throws JSONException
	 */
	@Override
	public String patchDataInElement(String url, JSONObject inputJson,
			String location) {
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("path", url);
		String[] array = url.split("/");
		Iterator<String> keys = inputJson.keys();
		String tempURL = array[0]; // appId
		while (keys.hasNext()) {
			String element = keys.next();
			if (element.equalsIgnoreCase("data")) {// update the content of the
													// url
				BasicDBObject newDocument = new BasicDBObject().append("$set",	new BasicDBObject().append("data",inputJson.toString()));
				coll.update(searchQuery, newDocument);
				break;
			}
			tempURL += "," + element;
			patchDataInElementRec(coll, tempURL, inputJson, location, element);
		}
		return inputJson.toString();
	}

	public void patchDataInElementRec(DBCollection coll, String tempURL,
			JSONObject inputJson, String location, String element) {
		JSONObject childs = null;
		String value = null;
		String child = null;
		try {
			childs = (JSONObject) inputJson.get(element);
			Iterator<String> childsIt = childs.keys();
			if (childsIt.hasNext()) {
				child = childsIt.next();
				tempURL += "," + child;
				patchDataInElementRec(coll, tempURL, (JSONObject) childs,location, child);
			}
		} catch (JSONException e) {// Java 7 allows multiple exception catches
									// but
			// < 7 doesn't!
			try {
				value = (String) inputJson.get(element);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (ClassCastException e1) {
			try {
				value = (String) inputJson.get(element);
			} catch (JSONException e2) {
				// TODO Auto-generated catch block
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
			geo.insertObjectInGrid(Double.parseDouble(splitted[0]),
					Double.parseDouble(splitted[1]), child, tempURL);
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

	@Override
	public boolean insertDocumentRoot(String appId, JSONObject data,
			String location) throws JSONException {
		DBCollection coll = db.getCollection(DataColl);
		String tempURL = appId;
		Iterator it = data.keys();// iterate the new content and
		// make it accessible
		while (it.hasNext()) {
			String key = (String) it.next();
			BasicDBObject temp = new BasicDBObject();
			temp.append("data", data.get(key));
			if (location != null){
				temp.append("location", location);
				String[] splitted = location.split(":");
				geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), key, tempURL);
			}
			tempURL += "," + key;
			temp.append("path", tempURL);
			coll.insert(temp);
			it.remove();
			tempURL = appId;
		}

		return true;
	}

	@Override
	public String getAllDocInApp(String appId) {
		DBCollection coll = db.getCollection(DataColl);
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
	public boolean createNonPublishableDocument(String appId, JSONObject data,
			String url, String location) {
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", url);
		DBCursor cursor = coll.find(searchQuery);
		boolean sucess = false;
		if (cursor.hasNext()) {// update the data inside the doc
			BasicDBObject newDocument = new BasicDBObject();
			if (location != null)
				newDocument.append("$set",new BasicDBObject().append("data",specialCharacter + data.toString()).append("location", location));
			// ~data
			
			else
				newDocument.append(	"$set",	new BasicDBObject().append("data", specialCharacter	+ data.toString())); 
			// ~data
			coll.update(searchQuery, newDocument);
			sucess = true;
		} else {// create the element and insert data
			String[] arrayUrl = url.split(",");
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

	@Override
	public boolean insertIntoUserDocument(String appId, String userId,
			JSONObject data, String url, String location) throws JSONException {
		DBCollection coll = db.getCollection(UserDataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("path", url);
		String[] array = url.split("/");
		Iterator<String> keys = data.keys();
		String tempURL = appId + ",users," + userId; // appId
		while (keys.hasNext()) {
			String element = keys.next();
			if (element.equalsIgnoreCase("data")) {// update the content of the
													// url
				BasicDBObject newDocument = new BasicDBObject()
						.append("$set",
								new BasicDBObject().append("data",
										data.toString()));
				coll.update(searchQuery, newDocument);
				break;
			}
			tempURL += "," + element;
			insertIntoUserDocumentRec(coll, appId, userId, tempURL, data, location, element);
		}
		return true;
	}
	//Diferences from the patchRec -> this one uses insert instead of update but the process is the same
	private void insertIntoUserDocumentRec(DBCollection coll, String appId,
			String userId, String tempURL, JSONObject data, String location,
			String element) {
		JSONObject childs = null;
		String value = null;
		try {
			childs = (JSONObject) data.get(element);
			Iterator<String> childsIt = childs.keys();
			if (childsIt.hasNext()) {
				String child = childsIt.next();
				tempURL += "," + child;
				patchDataInElementRec(coll, tempURL, (JSONObject) childs,
						location, child);
			}
			//Java 7 allows multiple exception catches but version < 7 doesn't
		} catch (JSONException e) {
			try {
				value = (String) data.get(element);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (ClassCastException e1) {
			try {
				value = (String) data.get(element);
			} catch (JSONException e2) {
				// TODO Auto-generated catch block
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

	@Override
	public String getElementInUserDocument(String appId, String userId,
			String url) {
		DBCollection coll = db.getCollection(DataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", url);
		DBCursor cursor = coll.find(searchQuery);
		DBObject obj = null;
		String data = null;
		obj = cursor.next();
		data = (String) obj.get("data");
		return data;
	}
	//have a look at the recursive function used in patchDataInElement
	@Override
	public boolean insertUserDocumentRoot(String appId, String userId,
			JSONObject data, String location) throws JSONException {
		DBCollection coll = db.getCollection(DataColl);
		String tempURL = appId + ",users," + userId;
		Iterator it = data.keys();// iterate the new content and
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

	@Override
	public boolean createNonPublishableUserDocument(String appId,
			String userId, JSONObject data, String url, String location) {
		DBCollection coll = db.getCollection(UserDataColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("path", url);
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
			String[] arrayUrl = url.split(",");
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
//GEO Location with iteration over all elements and haversine
//	@Override
//	public String getAllDocsInRadius(String appId, double latitude,
//			double longitude, double radius) {
//		DBCollection coll = db.getCollection(DataColl);
//		String allDoc = "";
//		BasicDBObject query = new BasicDBObject();
//		query.put("path", Pattern.compile(appId));
//		DBCursor cursor = coll.find(query);
//		Geolocation geo = new Geolocation();
//		while (cursor.hasNext()) {
//			DBObject obj = cursor.next();
//			String location = (String) obj.get("location");
//			if (location != null) {
//				String[] locationArray = location.split(":");
//				if (geo.distance(Double.parseDouble(locationArray[0]),
//						Double.parseDouble(locationArray[1]), latitude,
//						longitude) < radius)
//					allDoc += obj.toString();
//			}
//		}
//		return allDoc;
//	}
	@Override
	public ArrayList<String> getAllDocsInRadius(String appId, double latitude,
			double longitude, double radius) {
		DBCollection coll = db.getCollection(DataColl);
		Geolocation geo =new Geolocation();
		geo.createGridCache(180, 360);
		String type = appId+"docs";
		ArrayList<String> all = geo.searchObjectsInGrid(latitude, longitude, type , radius,appId);
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
			//geo location iterating over all elements
//	@Override
//	public String getDataInDocumentInRadius(String appId, String url,
//			double latitude, double longitude, double radius) {
//		DBCollection coll = db.getCollection(DataColl);
//		BasicDBObject searchQuery = new BasicDBObject();
//		searchQuery.put("path", url);
//		DBCursor cursor = coll.find(searchQuery);
//		DBObject obj = null;
//		String data = null;
//		String all = new String("");
//		Geolocation geo = new Geolocation();
//		while (cursor.hasNext()) {
//			obj = cursor.next();
//			String location = (String) obj.get("location");
//			if (location != null) {
//				String[] locationArray = location.split(":");
//				if (geo.distance(Double.parseDouble(locationArray[0]),
//						Double.parseDouble(locationArray[1]), latitude,
//						longitude) < radius)
//					all += obj.toString();
//			}
//		}
//		return all;
//	}
	public ArrayList<String> getDataInDocumentInRadius(String appId, String url, double latitude, double longitude,
			double radius){
		DBCollection coll = db.getCollection(DataColl);
		Geolocation geo =new Geolocation();
		geo.createGridCache(180, 360);
		String type = appId+"docs";
		ArrayList<String> all = geo.searchObjectsInGrid(latitude, longitude, type , radius, appId);
		Iterator<String> allIt = all.iterator();
		ArrayList<String> allElements = new ArrayList<String>();
		while(allIt.hasNext()){
			String next = allIt.next();
			BasicDBObject query = new BasicDBObject();
			query.append("path", Pattern.compile(next));
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
		Geolocation geo =new Geolocation();
		geo.createGridCache(180, 360);
		String type = appId+"docs";
		ArrayList<String> all = geo.searchObjectsInGrid(latitude, longitude, type , radius, appId);
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
	//geolocation iterating all user data
//	@Override
//	public String getAllUserDocsInRadius(String appId, String userId,
//			double latitude, double longitude, double radius) {
//		DBCollection coll = db.getCollection(UserDataColl);
//		String allDoc = "";
//		BasicDBObject query = new BasicDBObject();
//		query.put("path", Pattern.compile(appId + ",users," + userId));
//		DBCursor cursor = coll.find(query);
//		Geolocation geo = new Geolocation();
//		while (cursor.hasNext()) {
//			DBObject obj = cursor.next();
//			String location = (String) obj.get("location");
//			if (location != null) {
//				String[] locationArray = location.split(":");
//				if (geo.distance(Double.parseDouble(locationArray[0]),
//						Double.parseDouble(locationArray[1]), latitude,
//						longitude) < radius)
//					allDoc += obj.toString();
//			}
//		}
//		return allDoc;
//	}

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

	@Override
	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude, double longitude, double radius) {
		DBCollection coll = db.getCollection(UserDataColl);
		Geolocation geo =new Geolocation();
		geo.createGridCache(180, 360);
		String type = appId+"docs";
		ArrayList<String> all = geo.searchObjectsInGrid(latitude, longitude, AUDIO , radius, appId);
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
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,double longitude, 
			double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		DBCollection coll = db.getCollection(UserDataColl);
		Geolocation geo =new Geolocation();
		geo.createGridCache(10, 10);
		//String type = appId+"docs";
		ArrayList<String> all = geo.searchObjectsInGrid(latitude, longitude, IMAGE , radius, appId);
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
