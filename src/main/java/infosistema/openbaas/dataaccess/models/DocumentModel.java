/*****************************************************************************************
Infosistema - OpenBaas
Copyright(C) 2002-2014 Infosistema, S.A.
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
www.infosistema.com
info@openbaas.com
Av. José Gomes Ferreira, 11 3rd floor, s.34
Miraflores
1495-139 Algés Portugal
****************************************************************************************/
package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.utils.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class DocumentModel extends ModelAbstract {

	// *** CONTRUCTORS *** //

	public DocumentModel() {
		super();
	}


	// *** PRIVATE *** //

	private static BasicDBObject dataProjection = null; 	
	private static BasicDBObject dataProjectionMetadata = null; 	


	// *** PROTECTED *** //

	@Override
	protected DBCollection getCollection(String appId) {
		return super.getCollection(String.format(APP_DATA_COLL_FORMAT, appId));
	}
	
	public String getDocumentPath(List<String> path) {
		if (path == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size(); i++)
			if (!"".equals(path.get(i))) sb.append(path.get(i)).append("/");
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	protected String getDocumentParentPath(List<String> path) {
		if (path == null) return "";
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < path.size() - 1; i++)
			if (!"".equals(path.get(i))) sb.append(path.get(i)).append("/");
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	public String getDocumentId(String userId, List<String> path) {
		StringBuilder sb = new StringBuilder();
		if (userId != null) sb.append(userId).append("/");
		sb.append("data/");
		if (path != null) {
			for(int i = 0; i < path.size(); i++)
				if (!"".equals(path.get(i))) sb.append(path.get(i)).append("/");
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

	@Override
	protected BasicDBObject getDataProjection(boolean getMetadata, List<String> toShow, List<String> toHide) {
		if (toShow != null && toShow.size()>0) {
			BasicDBObject projection = new BasicDBObject();
			//Ciclo por string a 1 na projection
			Iterator<String> it = toShow.iterator();
			while(it.hasNext()){
				projection.append(it.next(), 1);
			}
			return projection;
		} else if (toHide != null && toHide.size()>0) {
			BasicDBObject projection = super.getDataProjection(new BasicDBObject(), getMetadata);
			projection.append(_KEY, 0);
			projection.append(_USER_ID, 0);
			projection.append(_PARENT_PATH, 0);
			//Ciclo por string a 0 na projection
			Iterator<String> it = toHide.iterator();
			while(it.hasNext()){
				projection.append(it.next(), 0);
			}
			return projection;
		} else if (getMetadata) {
			if (dataProjectionMetadata == null) {
				dataProjectionMetadata = super.getDataProjection(new BasicDBObject(), true);
				dataProjectionMetadata.append(_KEY, 0);
				dataProjectionMetadata.append(_USER_ID, 0);
				dataProjectionMetadata.append(_PARENT_PATH, 0);
			}
			return dataProjectionMetadata;
		} else {
			if (dataProjection == null) {
				dataProjection = super.getDataProjection(new BasicDBObject(), false);
				dataProjection.append(_KEY, 0);
				dataProjection.append(_USER_ID, 0);
				dataProjection.append(_PARENT_PATH, 0);
			}
			return dataProjection;
		}
	}

	
	// *** CONSTANTS *** //

	// *** KEYS *** //

	private static final String DATA = "data";
	private static final String _PARENT_PATH = "_parentPath"; 
	private static final String _KEY = "_key";
	private static final String PARENT_PATH_QUERY_FORMAT = "{\"" + _PARENT_PATH + "\": \"%s\"}";
	private static final String APP_DATA_COLL_FORMAT = "app%sdata";

	
	// *** CREATE *** //

	public synchronized JSONObject insertDocumentInPath(String appId, String userId, List<String> path, JSONObject data, Map<String, String> extraMetadata) throws JSONException {
		deleteDocumentInPath(appId, userId, path);
		JSONObject newData = insertDocument(appId, userId, path, data, extraMetadata); 
		if (updateAscendents(appId, userId, path, data, extraMetadata))
			return newData;
		else
			return null;
	}
	
	private JSONObject insertDocument(String appId, String userId, List<String> path, JSONObject data, Map<String, String> metadata) throws JSONException{
		if (!isMetadataCreate(metadata))  metadata = getMetadataCreate(userId, metadata);
		else metadata.remove(Metadata.LOCATION);
		try{
			Iterator<?> it = data.keys();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object value = data.get(key);
				if (value instanceof JSONObject) {
					List<String> newPath = addPath(path, key);
					insertDocument(appId, userId, newPath, (JSONObject)value, metadata);
				}
			}
			return insert(appId, userId, path, data, metadata);
		} catch (Exception e) {
			Log.error("", this, "insertDocumentInPath", "An error ocorred.", e);
			return null;
		}
	}

	private JSONObject insert(String appId, String userId, List<String> path, JSONObject value, Map<String, String> metadataFields) throws JSONException{
		String id = getDocumentId(userId, path);
		JSONObject  data = new JSONObject(value.toString());
		data.put(_KEY, getDocumentKey(path));
		data.put(_ID, id);
		if (userId != null && !"".equals(userId))
			data.put(_USER_ID, userId);
		data.put(_PARENT_PATH, getDocumentParentPath(path));
		if(!existsDocument(appId, userId, path) || !id.equals(userId)) {
			JSONObject metadata = getMetadaJSONObject(metadataFields);
			JSONObject geolocation = getGeolocation(metadata);
			super.insert(appId, data, metadata, geolocation);
		}
		return getDocument(appId, id, true, null, null);//<-- este true diz que é para devolver o metadata
	}
	
	// *** UPDATE *** //
	
	public synchronized JSONObject updateDocumentInPath(String appId, String userId, List<String> path, JSONObject data, Map<String, String> extraMetadata) throws JSONException{
		JSONObject newData = null; 
		try{
			String id = getDocumentId(userId, path);
			Map<String, String> metadataUpdate = getMetadataUpdate(userId, extraMetadata);
			if (!existsNode(appId, id)) {
				newData = insertDocument(appId, userId, path, data, getMetadataCreate(userId, extraMetadata)); 
				if (!updateAscendents(appId, userId, path, data, extraMetadata)) return null;
			}
			updateDocumentValues(appId, userId, path, data, true, metadataUpdate);
			newData = (JSONObject)getDocumentInPath(appId, userId, path, false, null, null);
			updateAscendents(appId, userId, path, newData, metadataUpdate);
		} catch (Exception e) {
			Log.error("", this, "updateDocumentInPath", "An error ocorred.", e); 
			return null;
		}
		return newData;
	}
	
	private Boolean updateDocumentValues(String appId, String userId, List<String> path, JSONObject data, boolean updateChilds, Map<String, String> metadata) throws JSONException{
		String id = getDocumentId(userId, path); 
		Iterator<?> it = data.keys();
		Map<String, String> metadataCreate = null;
		while (it.hasNext()) {
			String key = it.next().toString();;
			Object value = data.get(key);
			if (updateChilds && value instanceof JSONObject) {
				List<String> newPath = addPath(path, key);
				if (existsNode(appId, id + "/" + key)) {
					deleteDocument(appId, id+"/"+key);
				}
				if (metadataCreate == null) {
					metadataCreate = getMetadataCreate(userId, metadata);
					metadataCreate.remove(Metadata.LOCATION);
				}
				insertDocument(appId, userId, newPath, (JSONObject)value, metadataCreate);
			}
			updateDocumentValue(appId, id, key, value);
		}
		if (!isMetadataUpdate(metadata)) metadata = getMetadataUpdate(userId, metadata);
		updateMetadata(appId, id, metadata);
		return true;
	}
	
	private Boolean updateAscendents(String appId, String userId, List<String> path, JSONObject data, Map<String, String> metadata) throws JSONException{
		if (!isMetadataUpdate(metadata)) metadata = getMetadataUpdate(userId, metadata);
		String key = getDocumentKey(path);
		path = removeLast(path);
		String id = getDocumentId(userId, path);
		if (!existsNode(appId, id))
			insert(appId, userId, path, new JSONObject(), getMetadataCreate(userId, metadata));
		if ("".equals(key)) {
			if (userId != null && !"".equals(userId))
				updateDocumentValue(appId, userId, DATA, (JSONObject)getDocumentInPath(appId, userId, path, false, null, null));
			return true;
		} else {
			updateDocumentValue(appId, id, key, data);
			return updateAscendents(appId, userId, path, (JSONObject)getDocumentInPath(appId, userId, path, false, null, null), metadata);
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
		if (path == null || path.size() <= 0) {
			if (userId != null && !"".equals(userId)) {
				super.removeKeyFromDocument(appId, userId, "data." + key);
			}
			return true;
		}
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

	public Object getDocumentInPath(String appId, String userId, List<String> path, boolean getMetadata, List<String> toShow, List<String> toHide) throws JSONException {
		String id = getDocumentId(userId, path);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append(_ID, id);
		if (existsNode(appId, id)) {
			return super.getDocument(appId, id, getMetadata, toShow, toHide);
		} else if (path != null && path.size() > 0) {
			String key = getDocumentKey(path);
			path = removeLast(path);
			id = getDocumentId(userId, path);
			if (existsNode(appId, id)) {
				List<String> lst = new ArrayList<String>();
				lst.add(key);
				return super.getDocument(appId, id, false, lst, null).get(key);
			}
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
