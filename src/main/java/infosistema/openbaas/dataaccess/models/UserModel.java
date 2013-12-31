package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.models.User;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class UserModel extends ModelAbstract {

	// request types
	private JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
	Jedis jedis;
	
	public UserModel() {
		jedis = new Jedis(Const.getRedisGeneralServer(), Const.getRedisGeneralPort());
	}

	// *** PRIVATE *** //
	
	private static final String APP_DATA_COLL_FORMAT = "app%sdata";
	protected static final String _SN_SOCIALNETWORK_ID = "SN_SocialNetwork_ID";
	protected static final String _BASE_LOCATION_OPTION = "baseLocationOption";
	protected static final String _HASH = "hash";
	protected static final String _EMAIL = "email";
	protected static final String _ALIVE = "alive";
	protected static final String _SALT = "salt";
		
	private static final String USER_FIELD_KEY_FORMAT = "app:%s:user:%s:%s";
	private static final String ALL = "all";
	
	private String getKey(String appId, String field, String id) {
		return String.format(USER_FIELD_KEY_FORMAT, appId, field, id); 
	}

	private String getUserKey(String appId, String userId) {
		return getKey(appId, ALL, userId); 
	}

	private static BasicDBObject dataProjection = null;

	// *** PROTECTED *** //

	@Override
	protected DBCollection getCollection(String appId) {
		return super.getCollection(String.format(APP_DATA_COLL_FORMAT, appId));
	}
	

	protected BasicDBObject getDataProjection() {
		if (dataProjection == null) {
			dataProjection = super.getDataProjection(new BasicDBObject());
			dataProjection.append(_USER_ID, 1);
			//Users
			dataProjection.append(_SN_SOCIALNETWORK_ID, 1);
			dataProjection.append(_BASE_LOCATION_OPTION, 1);
			dataProjection.append(_HASH, 1);
			dataProjection.append(_EMAIL, 1);
			dataProjection.append(_ALIVE, 1);
			dataProjection.append(_SALT, 1);
		}
		return dataProjection;
	}

	
	// *** CREATE *** //

	public Boolean createUser(String appId, String userId, Map<String, String> fields) {
		Jedis jedis = pool.getResource();
		Boolean res = false;
		try {
			String userKey = getUserKey(appId, userId);
			if (!jedis.exists(userKey)) {
				JSONObject obj = new JSONObject();
				for (String key : fields.keySet()) {
					if (fields.get(key) != null) {
						String value = fields.get(key);
						if (User.isIndexedField(key)) {
							jedis.set(getKey(appId, key, value), userId);
						}
						jedis.hset(userKey, key, value);
						obj.put(key, value);
					}
				}
				obj.put(_USER_ID, userId);
				obj.put(_ID, userId);
				super.insert(appId, obj);
				res = true;
			}
		} catch (Exception e) {
			Log.error("", this, "createUser", "Error creating User", e);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
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

	public Boolean updateUser(String appId, String userId, Map<String, String> fields) {
		Jedis jedis = pool.getResource();
		Boolean res = false;
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
			res = true;
		} catch (Exception e) {
			Log.error("", this, "updateUser", "Error updating User", e);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
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
	public Map<String, String> getUser(String appId, String userId) {
		Jedis jedis = pool.getResource();
		Map<String, String> userFields = null;
		try {
			String userKey = getUserKey(appId, userId);
			userFields = jedis.hgetAll(userKey);
			if (userFields == null || userFields.size() <= 0)
				return null;
		} finally {
			pool.returnResource(jedis);
		}
		return userFields;
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
