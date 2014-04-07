package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class UserModel extends ModelAbstract {

	// *** CONTRUCTORS *** //

	public UserModel() {
		pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
	}


	// *** PRIVATE *** //

	private JedisPool pool;
	private static final String ALL = "all";
	private static BasicDBObject dataProjection = null;
	private static BasicDBObject dataProjectionMetadata = null; 	


	// *** PROTECTED *** //

	@Override
	protected DBCollection getCollection(String appId) {
		return super.getCollection(String.format(APP_DATA_COLL_FORMAT, appId));
	}
	
	@Override
	protected BasicDBObject getDataProjection(boolean getMetadata, List<String> toShow, List<String> toHide) {
		if (getMetadata) {
			if (dataProjectionMetadata == null) {
				dataProjectionMetadata = super.getDataProjection(new BasicDBObject(), true);
				dataProjectionMetadata.append(_USER_ID, 0);
				//Users
				//TODO ERROR dataProjectionMetadata.append(_SN_SOCIALNETWORK_ID, 0);
				dataProjectionMetadata.append(User.BASE_LOCATION_OPTION, 0);
				dataProjectionMetadata.append(User.HASH, 0);
				dataProjectionMetadata.append(User.EMAIL, 0);
				dataProjectionMetadata.append(User.ALIVE, 0);
				dataProjectionMetadata.append(User.SALT, 0);
			}
			return dataProjectionMetadata;
		} else {
			if (dataProjection == null) {
				dataProjection = super.getDataProjection(new BasicDBObject(), false);
				dataProjection.append(_USER_ID, 0);
				//Users
				//TODO ERROR dataProjection.append(User.SN_SOCIALNETWORK_ID, 0);
				dataProjection.append(User.BASE_LOCATION_OPTION, 0);
				dataProjection.append(User.HASH, 0);
				dataProjection.append(User.EMAIL, 0);
				dataProjection.append(User.ALIVE, 0);
				dataProjection.append(User.SALT, 0);
			}
			return dataProjection;
		}
	}

	
	// *** CONSTANTS *** //

	// *** KEYS *** //
	
	private static final String USER_FIELD_KEY_FORMAT = "app:%s:user:%s:%s";
	private static final String APP_DATA_COLL_FORMAT = "app%sdata";
	
	private String getKey(String appId, String field, String id) {
		return String.format(USER_FIELD_KEY_FORMAT, appId, field, id); 
	}

	private String getUserKey(String appId, String userId) {
		return getKey(appId, ALL, userId); 
	}


	// *** CREATE *** //

	public JSONObject createUser(String appId, String userId, Map<String, String> userFields, Map<String, String> extraMetadata) {
		Jedis jedis = pool.getResource();
		try {
			String userKey = getUserKey(appId, userId);
			JSONObject metadata = getMetadaJSONObject(getMetadataCreate(userId, extraMetadata));
			JSONObject geolocation = getGeolocation(metadata);
			JSONObject obj = new JSONObject();
			if (!jedis.exists(userKey)) {
				for (String key : userFields.keySet()) {
					if (userFields.get(key) != null) {
						String value = userFields.get(key);
						if (User.isIndexedField(key)) {
							jedis.set(getKey(appId, key, value), userId);
						}
						jedis.hset(userKey, key, value);
						obj.put(key, value);
					}
				}
				obj.put(_USER_ID, userId);
				obj.put(_ID, userId);
				if (metadata != null){ 
					Map<?,?> metaMap = convertJsonToMap(metadata);
					obj.put(_METADATA, metaMap);
					jedis.hset(userKey, _METADATA, metadata.toString());
				}
				if (geolocation != null){ 
					Map<?,?> metaGeo = convertJsonToMap(geolocation);
					obj.put(_GEO, metaGeo);
					jedis.hset(userKey, _GEO, geolocation.toString());
				}
				super.insert(appId, obj, metadata, geolocation);				
			}
			return obj;
		} catch (Exception e) {
			Log.error("", this, "createUser", "Error creating User", e);
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}


	// *** UPDATE *** //

	/**
	 * Updates the user, depending on the fields. If the only field sent by the
	 * request was alive, then only the alive field is updated.
	 * 
	 * @param appId
	 * @param userId
	 * @param email
	 * @param hash
	 * @param salt
	 * @param alive
	 * @throws UnsupportedEncodingException 
	 */

	public JSONObject updateUser(String appId, String userId, Map<String, String> fields, Map<String, String> extraMetadata) {
		Jedis jedis = pool.getResource();
		try {
			String userKey = getUserKey(appId, userId);
			if (jedis.exists(userKey)) {
				JSONObject obj = new JSONObject();
				for (String key : fields.keySet()) {
					if (fields.get(key) != null) {
						String value = fields.get(key);
						if (User.isIndexedField(key)) {
							String oldValue = jedis.hget(userKey, key);
							if (oldValue != null) jedis.del(getKey(appId, key, oldValue));
							jedis.set(getKey(appId, key, value), userId);
						}
						jedis.hset(userKey, key, value);
						super.updateDocumentValue(appId, userId, key, value);
						obj.put(key, value);
					}
				}
			}
			updateMetadata(appId, userId, getMetadataUpdate(userId, extraMetadata));
			return getUser(appId, userId, true);
		} catch (Exception e) {
			Log.error("", this, "updateUser", "Error updating User", e);
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}

	@Override
	protected synchronized void updateMetadata(String appId, String userId, Map<String, String> metadata) {
		Jedis jedis = pool.getResource();
		JSONObject geolocation = getGeolocation(getMetadaJSONObject(metadata));
		String userKey = getUserKey(appId, userId);
		try {
			if (metadata != null){
				String str = null;
				try {
					str = jedis.hget(userKey, _METADATA);
				} catch (Exception e) {
					Log.error("", this, "erro", "********erro no hget************hget("+userKey+" "+_METADATA+")",e);
				}
				Map<String, String> m = null;
				if(str!=null){
					
						JSONObject json = new JSONObject(str);
						m = convertJsonToMap2(json);
						m.putAll(metadata);
				}
				if (m!=null){
					jedis.hset(userKey, _METADATA, new JSONObject(m).toString());
				}
				else {
					jedis.hset(userKey, _METADATA, new JSONObject(metadata).toString());
				}
			}
		} catch (Exception e) {
			Log.error("", this, "err", "********update metadata************",e);
		}
		if (geolocation != null){ 
			jedis.hset(userKey, _GEO, geolocation.toString());
		}
		super.updateMetadata(appId, userId, metadata);
	}

	
	public void updateUserLocation(String appId, String userId, String location) {
		try {
			if (location != null) {
				super.updateDocumentValue(appId, userId, Const.LOCATION, location);
				super.updateMetadata(appId, userId, Metadata.getNewMetadata(location));
			}
			JSONObject geolocation = getGeolocation(new JSONObject("{location: \"" + location + "\"}"));
			if (geolocation != null) super.updateDocumentValue(appId, userId, _GEO, geolocation);
		} catch (JSONException e) {
			Log.error("", this, "updateUserLocation", "Error updating user location.", e);
		}
	}

	// *** GET LIST *** //


	// *** GET *** //

	/**
	 * Checks if user is present in the app:{appId}:all:users and if it is returns
	 * its fields
	 * 
	 * @param appId
	 * @param userId
	 * @return
	 */
	public JSONObject getUser(String appId, String userId, boolean getMetadata) {
		SessionModel sessionModel = new SessionModel();
		Jedis jedis = pool.getResource();
		Map<String, String> userFields = null;
		try {
			String userKey = getUserKey(appId, userId);
			userFields = jedis.hgetAll(userKey);
			Boolean online = sessionModel.isUserOnline(userId);
			userFields.put("online", online.toString());
			if (!getMetadata) userFields.remove(_METADATA);
			if (userFields == null || userFields.size() <= 0)
				return null;
			return getJSonObject(userFields);
		} catch (Exception e) {
			Log.error("", this, "getUser", "Error getting user", e);
			return null;
		} finally {
			pool.returnResource(jedis);
		}
	}

	public String getUserField(String appId, String userId, String field) {
		Jedis jedis = pool.getResource();
		try {
			String userKey = getUserKey(appId, userId);
			return jedis.hget(userKey, field);
		} finally {
			pool.returnResource(jedis);
		}
	}
	
	public String getUserIdUsingSocialInfo(String appId, String socialId, String socialNetwork) {
		return getUserIdUsingField(appId, User.SOCIAL_NETWORK_ID(socialNetwork), socialId);
	}	

	public String getUserIdUsingUserName(String appId, String userName) {
		return getUserIdUsingField(appId, User.USER_NAME, userName);
	}
	
	public String getUserIdUsingEmail(String appId, String email) {
		Log.error("", "", "", "%%%%%%%% 0");
		return getUserIdUsingField(appId, User.EMAIL, email);
	}

	private String getUserIdUsingField(String appId, String field, String value) {
		Jedis jedis = pool.getResource();
		try {
			return jedis.get(getKey(appId, field, value));
		} catch (Exception e){
			Log.error("", this, "getUserIdUsingField", "Error getting User Id", e);
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}


	// *** DELETE *** //


	// *** EXISTS *** //

 	public Boolean userIdExists(String appId, String userId) {
		return fieldExists(appId, ALL, userId);
	}
	
	public Boolean userEmailExists(String appId, String email) {
		return fieldExists(appId, User.EMAIL, email);
	}
	
	public Boolean userNameExists(String appId, String userName) {
		return fieldExists(appId, User.USER_NAME, userName);
	}
	
	public Boolean socialUserExists(String appId, String socialId,	String socialNetwork) {
		return fieldExists(appId, User.SOCIAL_NETWORK_ID(socialNetwork), socialId);
	}

	private Boolean fieldExists(String appId, String field, String value) {
		Jedis jedis = pool.getResource();
		Boolean exists = false;
		try {
			exists = jedis.exists(getKey(appId, field, value));
		} catch (Exception e){
			Log.error("", this, "userIdExistsInApp", "Error getting User", e);
		} finally {
			pool.returnResource(jedis);
		}
		return exists;
	}


	// *** OTHERS *** //

}
