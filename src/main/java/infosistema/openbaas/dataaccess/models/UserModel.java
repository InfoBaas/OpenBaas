package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

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
	
	private static final String USER_FIELD_KEY_FORMAT = "app:%s:user:%s:%s";
	private static final String ALL = "all";
	
	private String getKey(String appId, String field, String id) {
		return String.format(USER_FIELD_KEY_FORMAT, appId, field, id); 
	}

	private String getUserKey(String appId, String userId) {
		return getKey(appId, ALL, userId); 
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
				super.insertDocumentInPath(appId, userId, null, obj);
				
				//TODO: APAGAR jedis.sadd("app:" + appId + ":users", userId);
				
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
						obj.append(key, value);
					}
				}
				super.updateDocumentInPath(appId, userId, null, obj);
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

	//TODO: Corrigir
	public List<String> getOperation(String appId, OperatorEnum oper, String attribute, String value) throws Exception {
		Jedis jedis = pool.getResource();
		List<String> listRes = new ArrayList<String>();
		try{
			Set<String> setUsers = null; //jedis.smembers("app:"+appId+":users:emails");
			Iterator<String> iter = setUsers.iterator();
			while(iter.hasNext()){
				String userId = iter.next();
				Map<String, String> mapUser = getUser(appId, userId);
				if(mapUser.containsKey(attribute)){
					if(mapUser.get(attribute).equals(value))
						listRes.add(userId);
				}
			}
		}
		catch (Exception e) {
			throw e;
		}
		return listRes;
	}


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
