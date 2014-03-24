package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.geolocation.Geo;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

public abstract class ModelAbstract {

	// *** CONTRUCTORS *** //

	public ModelAbstract() {
		try {
			//Utils.printMemoryStats();
			if(mongoClient == null){
				if(Const.getMongoAuth()){
					MongoCredential credential = MongoCredential.createMongoCRCredential(Const.getMongoUser(), Const.getMongoDb(), Const.getMongoPass().toCharArray());
					ServerAddress server = new ServerAddress(Const.getMongoServer(), Const.getMongoPort());
					mongoClient = new MongoClient(server, Arrays.asList(credential));
				}else{
					mongoClient = new MongoClient(Const.getMongoServer(), Const.getMongoPort());
				}
			}
			geo = Geo.getInstance();
		} catch (UnknownHostException e) {
			Log.error("", this, "DocumentModel", "Unknown Host.", e); 
		}
	}
	

	// *** PRIVATE *** //

	private static MongoClient mongoClient = null;
	private DB db;
	private static Geo geo;

	
	// *** PROTECTED *** //
	
	protected static final String _USER_ID = "_userId";
	protected static final String DESC = "desc";
	protected static final String _DIST = "_dist";

	protected abstract BasicDBObject getDataProjection(boolean getMetadata, List<String> toShow, List<String> toHide);

	protected BasicDBObject getDataProjection(BasicDBObject dataProjection, boolean getMetadata) {
		dataProjection.append(_ID, 0);
		if (!getMetadata)
			dataProjection.append(_METADATA, 0);
		dataProjection.append(_GEO, 0);
		return dataProjection;
	}

	protected DBCollection getCollection(String collStr) {
		db = mongoClient.getDB(Const.getMongoDb());
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
	
	private String getMetadataKey(String key) {
		return String.format(METADATA_KEY_FORMAT, key);
	}
	
	protected JSONObject getGeolocation(JSONObject metadata) {
		try {
			String location = metadata.getString(Const.LOCATION);
			if (location == null || "".equals(location)) return null;
			String[] locationArray = location.split(":");
			Double latitude = Double.parseDouble(locationArray[0]);
			Double longitude = Double.parseDouble(locationArray[1]);
			if (latitude == null || longitude == null) return null;
			Double gridLatitude = geo.getGridLatitude(latitude);
			Double gridLongitude = geo.getGridLongitude(longitude);
			return new JSONObject(String.format(GEO_FORMAT, latitude, longitude, gridLatitude, gridLongitude));
		} catch (Exception e) {
			return null;
		}
	}
	
	protected JSONObject getMetadaJSONObject(Map<String, String> metadata) {
		JSONObject obj = new JSONObject();
		for (String key: metadata.keySet()) {
			try {
				obj.put(key, metadata.get(key));
			} catch (JSONException e) {
				Log.error("", this, "getMetadaJSONObject", "Error getting metadata JSONObject.", e);
			}
		}
		return obj;
	}
	
	protected Map<String, Object> convertJsonToMap(JSONObject json) {
		Map<String,Object> map = new HashMap<String,Object>();
		ObjectMapper mapper = new ObjectMapper();
		try {
			if(json!=null)
				map = mapper.readValue(json.toString(), new TypeReference<HashMap<String,Object>>(){});	 
		} catch (Exception e) {
			Log.error("", this, "convertJsonToMap", "Error trasnforming JSONObject to map.", e);
		}
		return map;
	}
	
	protected Map<String, String> convertJsonToMap2(JSONObject json) {
		Map<String,String> map = new HashMap<String,String>();
		ObjectMapper mapper = new ObjectMapper();
	 
		try {
			map = mapper.readValue(json.toString(), new TypeReference<HashMap<String,String>>(){});	 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	
	// *** CONSTANTS *** //
	
	public static final String _ID = "_id"; 
	public static final String DATA = "data"; 
	public static final String METADATA = "metadata";
	public static final String _METADATA = "_metadata";
	public static final String _TYPE = "_type";
	public static final String _GEO = "_geo"; 
	private static final String LATITUDE = "latitude";
	private static final String LONGITUDE = "longitude";
	private static final String GRID_LATITUDE = "gridLatitude";
	private static final String GRID_LONGITUDE = "gridLongitude";


	// *** KEYS *** //

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
	private static final String LOCATION_QUERY_FORMAT = "{\"" + _GEO + "." + GRID_LATITUDE + "\": {$gte: %s}, \"" +
																_GEO + "." + GRID_LATITUDE + "\": {$lte: %s}, \"" + 
																_GEO + "." + GRID_LONGITUDE + "\": {$gte: %s}, \"" + 
																_GEO + "." + GRID_LONGITUDE + "\": {$lte: %s}, }";
	private static final String METADATA_KEY_FORMAT = _METADATA + ".%s"; 
	private static final String GEO_FORMAT = "{" + LATITUDE + ": %s, " + LONGITUDE + ": %s, " + GRID_LATITUDE + ": %s, " + GRID_LONGITUDE + ": %s}";
	
	
	// *** CREATE *** //

 	protected JSONObject insert(String appId, JSONObject value, JSONObject metadata, JSONObject geolocation) throws JSONException{
		DBCollection coll = getCollection(appId);
		JSONObject data = new JSONObject(value.toString());
		
		
		if (metadata != null){
			Map<String, Object> metaMap = convertJsonToMap(metadata);
			data.put(_METADATA, metaMap);
		}
		if (geolocation != null){
			Map<String, Object> metaGeo = convertJsonToMap(geolocation);
			data.put(_GEO, metaGeo);
		}
		DBObject dbData = (DBObject)JSON.parse(data.toString());
		coll.insert(dbData);
		
		return data;		
	}
	
 	
	// *** UPDATE *** //
	
	protected JSONObject updateDocumentValue(String appId, String id, String key, Object value) throws JSONException{
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
			return null;
		}
		return getDocument(appId, id, true, null, null);
	}

	protected void updateMetadata(String appId, String id, Map<String, String> metadata) {
		if (metadata == null) return;
		for (String key: metadata.keySet()) {
			try {
				updateDocumentValue(appId, id, getMetadataKey(key), metadata.get(key));
			} catch (JSONException e) {
				Log.error("", this, "updateMetadata", "Error updating metadata key: " + key, e);
			}
		}
		try {
			JSONObject geolocation = getGeolocation(getJSonObject(metadata));
			if (geolocation != null) 
				updateDocumentValue(appId, id, _GEO, geolocation);
		} catch (JSONException e) {
			Log.error("", this, "updateMetadata", "Error updatingo Document Geolocation.", e);
		}

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
		try{
			DBCollection coll = getCollection(appId);
			BasicDBObject ob = new BasicDBObject();
			BasicDBObject dbRemove = new BasicDBObject();
			ob.append(key, "");
			dbRemove.append("$unset", ob);
			BasicDBObject dbQuery = new BasicDBObject();
			dbQuery.append(_ID, id); 		
			coll.update(dbQuery, dbRemove);
		}
		catch(Exception e){
			return false;
		}
		return true;	
	}
	
	
	// *** GET LIST *** //

	public List<DBObject> getDocuments(String appId, String userId, String path, 
			Double latitude, Double longitude, Double radius, JSONObject query, 
			String orderType, String orderBy, List<String> toShow) throws Exception {
		String strParentPathQuery = getParentPathQueryString(path);
		String strQueryLocation = getQueryLocationString(latitude, longitude, radius);
		String strQuery = getQueryString(appId, path, query, orderType);
		String searchQuery = getAndQueryString(strParentPathQuery, strQuery);
		if (strQueryLocation != null)
			searchQuery = getAndQueryString(searchQuery, strQueryLocation);
		if (userId != null && !"".equals(userId))
			searchQuery = getAndQueryString(searchQuery, getUserIdQueryString(userId));
		DBCollection coll = getCollection(appId);
		DBObject queryObj = (DBObject)JSON.parse(searchQuery); 
		
/*
{"_parentPath": "specials/precincts", , "_geo.gridLatitude": {$gte: 70.0}, "_geo.gridLatitude": {$lte: 270.0}, 
"_geo.gridLongitude": {$gte: -110.0}, "_geo.gridLongitude": {$lte: 110.0}, }
 */
		BasicDBObject projection = new BasicDBObject();
		if(toShow.size()>0){
			projection = getDataProjection(true, toShow, null);
		}
		
		if (strQueryLocation != null)
			projection.append(_GEO, 1);
		
		projection.append(_ID, 1);
		DBObject sortQuery = getSortQuery(orderBy, orderType);
		//Log.debug(userId, "", "getDocuments", "Query: "+query.toString());
		//Log.debug(userId, "", "getDocuments", "Query Obj: "+queryObj+" - Projection: "+projection+" - SortQuery: "+sortQuery);
		DBCursor cursor = coll.find(queryObj, projection).sort(sortQuery);
		List<DBObject> retObj = new ArrayList<DBObject>();
		HashMap<DBObject, String> lstIdDists = new HashMap<DBObject, String>();
		while (cursor.hasNext()) {
			DBObject obj = cursor.next();
			try {
				if (strQueryLocation != null) {
					DBObject _geo = (DBObject)obj.get(_GEO);
					Double objLatitude = Double.valueOf("" + _geo.get(LATITUDE));
					Double objLongitude = Double.valueOf("" + _geo.get(LONGITUDE));
					if (!geo.isWithinDistance(objLatitude, objLongitude, latitude, longitude, radius))
						continue;
					else if (orderBy.equals(_DIST)){
						Double dist = geo.getDistanceFromLatLonInKm(latitude, longitude, objLatitude, objLongitude);
						lstIdDists.put(obj,dist.toString());
					}
				}else{
					String idAux =obj.get(_ID).toString();
					String[] splitArray = idAux.split("/");
					String id = splitArray[splitArray.length-1];
					//obj.removeField(_ID);
					DBObject res = new BasicDBObject();
					res.put(_ID, id);
					DBObject meta = new BasicDBObject();
					  try{
						  meta = (DBObject) obj.get(_METADATA);
						  obj.removeField(_METADATA);
					  }catch(Exception e){ }
					  res.put(METADATA,meta);
					res.put(DATA,obj);
					retObj.add(res);
				}
			} catch (Exception e) {
				Log.error("", this, "getDocuments", "Error determining location distance for objectId = " + obj.get(_ID).toString() + " .");
			}
		}
		if (orderBy.equals(_DIST)){
			retObj = sortByValues(lstIdDists, orderType);
		}
		return retObj;
	}
	
	private static List<DBObject> sortByValues(Map<DBObject, String> map, String orderType){
		List<DBObject> retObj = new ArrayList<DBObject>();
		List<Map.Entry<DBObject, String>> entries = new LinkedList<Map.Entry<DBObject, String>>(map.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<DBObject, String>>() {
            @Override
            public int compare(Entry<DBObject, String> o1, Entry<DBObject, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
     
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<DBObject, String> sortedMap = new LinkedHashMap<DBObject, String>();
        for(Map.Entry<DBObject, String> entry: entries){
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        String[] splitArray = null;
        Iterator<Entry<DBObject,String>> entries2 = sortedMap.entrySet().iterator();
		while (entries2.hasNext()) { 
		  Entry<DBObject,String> thisEntry = entries2.next();
		  DBObject key = thisEntry.getKey();
		  
		  String id = (String) key.get(_ID);
		  if(id.contains("/"))
			  splitArray = id.split("/");
		  else{
			  splitArray = new String[1];
			  splitArray[0]=id;
		  }
		  key.removeField(_GEO);
		  DBObject res = new BasicDBObject();
		  res.put(_ID, splitArray[splitArray.length-1]);
		  res.put(DATA,key);
		  DBObject meta = new BasicDBObject();
		  try{
			  meta = (DBObject) key.get(_METADATA);
			  key.removeField(_METADATA);
		  }catch(Exception e){ }
		  res.put(METADATA,meta);
		  retObj.add(res);
		}
		if(orderType.equals("desc")){
			Collections.reverse(retObj);
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
	                Object obj = query.get(QueryParameters.ATTR_VALUE);
	                if (obj instanceof String) value = "\"" + obj + "\"";
	                else value = "" + obj;
	                String attribute = null;
					attribute = query.getString(QueryParameters.ATTR_ATTRIBUTE).replace("/", ".");
					return getOperationQueryString(oper, attribute, value);
				}
			} else {
				return query.toString();
			}
		} else {
			return null;
		}
	}
	
	protected String getParentPathQueryString(String path) {
		return "";
	}
	
	private String getQueryLocationString(Double latitude, Double longitude, Double radius) {
		if (latitude == null || longitude == null || radius == null) return null;
		try {
			Double latitudeIni = geo.getGridLatitude(latitude - geo.transformMetersInDegreesLat(radius)); 
			Double latitudeEnd = geo.getGridLongitude(latitude + geo.transformMetersInDegreesLat(radius)); 
			Double longitudeIni = geo.getGridLatitude(longitude - geo.transformMetersInDegreesLong(radius, latitudeIni)); 
			Double longitudeEnd = geo.getGridLongitude(longitude + geo.transformMetersInDegreesLong(radius, latitudeEnd));
			return String.format(LOCATION_QUERY_FORMAT, latitudeIni.toString(), latitudeEnd.toString(), longitudeIni.toString(), longitudeEnd.toString());
		} catch (Exception e) {
			Log.error("", this, "getQueryLocationString", "Erros getting location query.", e);
			return null;
		}

	}
	
	private String getAndQueryString(String oper1, String oper2) {
		if (oper1 == null || "".equals(oper1) || oper1.contains("null")) return (oper2 == null || "".equals(oper2)) ? "" : oper2;
		if (oper2 == null || "".equals(oper2) || oper2.contains("null")) return oper1;
		if (oper1.startsWith("{")) oper1 = oper1.substring(1);
		if (oper1.endsWith("}")) oper1 = oper1.substring(0, oper1.length() - 1);
		if (oper2.startsWith("{")) oper2 = oper2.substring(1);
		if (oper2.endsWith("}")) oper2 = oper2.substring(0, oper2.length() - 1);
		if ("".equals(oper1.trim())) return "{" + oper2 + "}";
		if ("".equals(oper2.trim())) return "{" + oper1 + "}";
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
	
	private DBObject getSortQuery(String orderBy, String orderType) {
		if(orderBy.equals(_DIST)) orderBy=_ID;
		Integer order = 1;
		if(orderType.equals(DESC)) order = -1;
		BasicDBObject sortQuery = new BasicDBObject();
		sortQuery.put(orderBy.replace("/", "."), order);
		return sortQuery;
	}


	// *** GET *** //

	protected JSONObject getDocument(String appId, String id, boolean getMetadata, List<String> toShow, List<String> toHide) throws JSONException {
		DBCollection coll = getCollection(appId);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(_ID, id);
		BasicDBObject projection = getDataProjection(getMetadata, toShow, toHide);
		DBCursor cursor = coll.find(searchQuery, projection);
		if (cursor.hasNext()) {
			//mongoClient.close();
			return new JSONObject(JSON.serialize(cursor.next()));
		}
		//mongoClient.close();
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
			//mongoClient.close();
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
		} finally {
			//mongoClient.close();
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
		finally {
			//mongoClient.close();
		}
		return false;
	}
	
	
	// *** METADATA *** //

	protected Map<String, String> getMetadataCreate(String userId, Map<String, String> extraMetadata) {
		Map<String, String> metadata = new HashMap<String, String>();
		if (extraMetadata != null) metadata.putAll(extraMetadata);
		metadata.put(Metadata.CREATE_DATE, (new Date()).toString());
		metadata.put(Metadata.CREATE_USER, userId);
		metadata.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		metadata.put(Metadata.LAST_UPDATE_USER, userId);
		return metadata;
	}
	
	protected Map<String, String> getMetadataUpdate(String userId, Map<String, String> extraMetadata) {
		Map<String, String> metadata = new HashMap<String, String>();
		if (extraMetadata != null) metadata.putAll(extraMetadata);
		metadata.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		metadata.put(Metadata.LAST_UPDATE_USER, userId);
		return metadata;
	}

	protected Map<String, String> removeLocation(Map<String, String> metadata) {
		Map<String, String> newMetadata = metadata;
		if (newMetadata != null && newMetadata.containsKey(Metadata.LOCATION)) {
			newMetadata = new HashMap<String, String>(metadata);
			newMetadata.remove(Metadata.LOCATION);
		}
		return newMetadata;
	}
	
	protected boolean isMetadataCreate(Map<String, String> metadata) {
		return metadata != null && metadata.containsKey(Metadata.CREATE_DATE);		
	}

	protected boolean isMetadataUpdate(Map<String, String> metadata) {
		return metadata != null && metadata.containsKey(Metadata.LAST_UPDATE_DATE);		
	}

}
