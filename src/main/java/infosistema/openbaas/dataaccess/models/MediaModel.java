package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.utils.Log;

import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

public class MediaModel extends ModelAbstract {

	// request types
	public static final String APP_MEDIA_COLL_FORMAT = "app%sdata";
	protected static final String _TYPE = "_type";
	private static final String TYPE_QUERY_FORMAT = "{" + _TYPE + ": \"%s\"";
	private static final String ID_FORMAT = "%s:%s";

	private static BasicDBObject dataProjection = null; 	

	public MediaModel() {
	}

	// *** *** MEDIA *** *** //
	
	// *** PRIVATE *** //
	
	@Override
	protected DBCollection getCollection(String appId) {
		return super.getCollection(String.format(APP_MEDIA_COLL_FORMAT, appId));
	}
	
	protected BasicDBObject getDataProjection() {
		if (dataProjection == null) {
			dataProjection = super.getDataProjection(new BasicDBObject());
			dataProjection.append(_TYPE, 0);
		}
		return dataProjection;
	}

	private String getMediaId(ModelEnum type, String objId) {
		return String.format(ID_FORMAT, type.toString(), objId);
	}
	
	private String getTypeQuery(ModelEnum type) {
		if (type == null) return "";
		return String.format(TYPE_QUERY_FORMAT, type.toString());
	}

	// *** CREATE *** //
	
	public Boolean createMedia(String appId, ModelEnum type, String objId, Map<String, String> fields) {
		try{
			if (type == null) {
				Log.error("", this, "createMedia", "Media as no type.");
				return false;
			}
			JSONObject data = getJSonObject(fields);
			data.put(_ID, objId);
			data.put(_TYPE, type.toString());
			super.insert(appId, data);
		} catch (Exception e) {
			Log.error("", this, "createMedia", "An error ocorred.", e);
			return false;
		}
		return true;
	}
	
	
	// *** UPDATE *** //

	// *** GET LIST *** //
	
	public List<String> getMedia(String appId, ModelEnum type, JSONObject query, String orderType,String orderBy) throws Exception {
		JSONObject finalQuery = new JSONObject();
		if (type != null) {
			finalQuery.append(OperatorEnum.oper.toString(), OperatorEnum.and.toString());
			finalQuery.append(OperatorEnum.op1.toString(), getTypeQuery(type));
			finalQuery.append(OperatorEnum.op2.toString(), query.toString());
		} else {
			finalQuery = query;
		}
		return super.getDocuments(appId, null, null, finalQuery, orderType,orderBy);
	}

	// *** GET *** //

	public Map<String, String> getMedia(String appId, ModelEnum type, String objId) {
		//CACHE
		try {
			return getObjectFields(super.getDocument(appId, objId));
		} catch (JSONException e) {
			Log.error("", this, "getMedia", "Error getting Media.", e);
		}
		return null;
	}

	/**
	 * Returns the directory of the specified 'id' file.
	 * 
	 * @param appId
	 * @param id
	 * @param folderType
	 * @param requestType
	 * @return
	 */
	public String getMediaField(String appId, ModelEnum type, String objId, String field) {
		//CACHE
		try {
			return getMedia(appId, type, objId).get(field).toString();
		} catch (Exception e) {
			return null;
		}
	}


	// *** DELETE *** //
	
	public Boolean deleteMedia(String appId, ModelEnum type, String objId) {
		//CACHE
		String id = getMediaId(type, objId);
		return super.deleteDocument(appId, id);		
	}

	
	// *** EXISTS *** //
	
	public Boolean mediaExists(String appId, ModelEnum type, String objId) {
		//CACHE
		String id =objId;
		return super.existsNode(appId, id);
	}

	// *** OTHERS *** //

}
