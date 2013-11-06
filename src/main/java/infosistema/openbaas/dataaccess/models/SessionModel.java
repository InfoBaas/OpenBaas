package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.ModelEnum;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.UnsupportedEncodingException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Sessions are stored in a separate redis instance, this redis instance is used
 * only for session operations.
 * 
 * Redis Sessions Instance -> Sessions Redis Cache Instance -> Everything else
 * (users/apps/ect)
 * 
 */
public class SessionModel {

	// *** MEMBERS *** //

	private static final int EXPIRETIME = 86400; // 24hours in seconds
	public static final long MAXCACHESIZE = 5242880; // bytes
	private Jedis jedis;
	private Geolocation geo = Geolocation.getInstance();
	private final static String server = "localhost";

	
	// *** CONSTRUCTOR *** //
	
	public SessionModel() {
		jedis = new Jedis(server, Const.REDIS_SESSION_PORT);
	}
	
	
	// *** CREATE *** //
	
	public void createAdmin(String OPENBAASADMIN, byte[] adminSalt, byte[] adminHash) throws UnsupportedEncodingException {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset(OPENBAASADMIN, "adminSalt", new String(adminSalt,"ISO-8859-1"));
			jedis.hset(OPENBAASADMIN, "adminHash", new String(adminHash,"ISO-8859-1"));
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	/**
	 * For sessions we use Redis expire mechanism, keys with more than 24 hours
	 * are automatically removed.
	 * 
	 * @param sessionId
	 * @param userId
	 */
	public void createSession(String sessionId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER,	Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.sadd("sessions:set", sessionId);
			jedis.hset("sessions:" + sessionId, "userId", userId);
			jedis.expire("sessions:" + sessionId, EXPIRETIME);
			jedis.sadd("user:sessions:" + userId, sessionId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	// *** AUX *** //

	private void addLocationToSession(String location, String sessionToken, String userAgent, String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("sessions:" + sessionToken, "location", location);
			jedis.hset("sessions:" + sessionToken, "userAgent", userAgent);
			String[] locationArray = location.split(":");
			
			double latitude = Double.parseDouble(locationArray[0]);
			double longitude = Double.parseDouble(locationArray[1]);
			geo.insertObjectInGrid(latitude, longitude, ModelEnum.users, appId, userId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	private void updateLocationToSession(double previousLatitudeValue, double previousLongitudeValue, double currentLatitudeValue, double currentLongitudeValue,
			String location, String sessionToken, String userAgent, String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("sessions:" + sessionToken, "location", location);
			geo.updateObjectInGrid(previousLatitudeValue, previousLongitudeValue, currentLatitudeValue,
					currentLongitudeValue, ModelEnum.users, appId, userId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}


	// *** UPDATE *** //
	
	/**
	 * Updates the session time, if it had 10 hours left until being deleted
	 * after we call refreshSession it will have EXPIRETIME until it is deleted
	 * (by default 24 hours).
	 */
	// P1=(lat1, lon1) and P2=(lat2, lon2)
	// dist = arccos(sin(lat1) · sin(lat2) + cos(lat1) · cos(lat2) · cos(lon1 - lon2)) · R
	public boolean refreshSession(String sessionToken, String location, String date, String userAgent) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.expire("sessions:" + sessionToken, EXPIRETIME);
			jedis.hset("sessions:" + sessionToken, "date", date);
			
			if (location != null && !"".equals(location)) {
				String previousLocation = jedis.hget("sessions:" + sessionToken, "location");
				String userId = this.getUserUsingSessionToken(sessionToken);
				String appId = this.getAppUsingSessionToken(sessionToken);
				if (previousLocation == null) { // No previous Location, we simply add it.
					addLocationToSession(location, sessionToken, userAgent, appId, userId);
				} else { // Calculate the distances
					// Split the data previous location
					String[] previousLocationArray = previousLocation.split(":");
					String[] currentLocationArray = location.split(":");
					double previousLatitudeValue, previousLongitudeValue, currentLatitudeValue, currentLongitudeValue;
					try{
						previousLatitudeValue = Double.parseDouble(previousLocationArray[0]);
						previousLongitudeValue = Double.parseDouble(previousLocationArray[1]);
		
						currentLatitudeValue = Double.parseDouble(currentLocationArray[0]);
						currentLongitudeValue = Double.parseDouble(currentLocationArray[1]);
					}catch (NumberFormatException e){
						Log.error("", this, "refreshSession", "Wrong number format of " +
								"previousLocationArray[0]=" + previousLocationArray[0] + " or " +
								"previousLocationArray[1=]=" + previousLocationArray[1] + " or " +
								"currentLocationArray[0]=" + currentLocationArray[0] + " or " +
								"currentLocationArray[1]=" + currentLocationArray[1], e); 
						return false;
					}
					// Test if distance < MAXIMUM DISTANCE Spherical Law of Cosines
					double dist = geo.distance(previousLatitudeValue, previousLongitudeValue, currentLatitudeValue, currentLongitudeValue);
					if (dist >= 1) {
						jedis.hset("sessions:" + sessionToken, "location", location);
						UserModel userModel = new UserModel(); 
						userModel.updateUserLocationAndDate(userId, appId, sessionToken, location, date);

						updateLocationToSession(previousLatitudeValue, previousLongitudeValue, currentLatitudeValue, currentLongitudeValue, 
								location, sessionToken, userAgent, appId, userId);
					}
				}
			}
			
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return true;
	}
	
	
	// *** DELETE *** //
	
	
	// *** GET LIST *** //
	
	// *** GET *** //
	
	public Map<String, String> getAdminFields(String OPENBAASADMIN) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER,	Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		Map<String, String> adminFields = null;
		try {
			adminFields = this.jedis.hgetAll(OPENBAASADMIN);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return adminFields;
	}

	
	// *** OTHERS *** //

	public boolean adminExists(String admin) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		boolean adminExists = false;
		try {
			if (jedis.exists(admin))
				adminExists = true;
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return adminExists;
	}

	public void createSession(String sessionId, String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.sadd("sessions:set", sessionId);
			jedis.hset("sessions:" + sessionId, "appId", appId);
			jedis.hset("sessions:" + sessionId, "userId", userId);
			jedis.expire("sessions:" + sessionId, EXPIRETIME);
			jedis.sadd("user:sessions:" + userId, sessionId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	public Map<String, String> getSessionFields(String sessionId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		Map<String, String> sessionFields = null;
		try {
			sessionFields = jedis.hgetAll("sessions:" + sessionId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sessionFields;
	}

	public boolean sessionTokenExists(String sessionId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		boolean sucess = false;
		try {
			Set<String> sessionIds = jedis.smembers("sessions:set");
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				if (it.next().equalsIgnoreCase(sessionId) && jedis.exists("sessions:" + sessionId)){
					sucess = true;
					break;
				}
			}

		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sucess;
	}

	public boolean sessionTokenExistsForUser(String sessionToken, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		boolean sucess = false;
		try {
			Set<String> sessionIds = jedis.smembers("user:sessions:" + userId);
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				String sessionId = it.next();
				if (jedis.exists("sessions:" + sessionId) && sessionId.equalsIgnoreCase(sessionToken))
					sucess = true;
				else
					jedis.srem("user:sessions:" + userId, sessionId);
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sucess;
	}

	public void deleteAdminSession(String adminId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		try {
			Set<String> sessionIds = jedis.smembers("sessions");
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				String id = it.next();
				Map<String, String> sessionFields = jedis.hgetAll("sessions:" + id);
				for (Entry<String, String> entry : sessionFields.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("userId")) {
						if (entry.getKey().equalsIgnoreCase("userId")) {
							if (entry.getValue().equalsIgnoreCase(adminId)) {
								jedis.del("sessions:" + id);
								jedis.srem("sessions", adminId);
							}
						}
					}
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	public void createAdminSession(String sessionId, String adminId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.sadd("sessions", sessionId);
			jedis.hset("sessions:" + sessionId, "adminId", adminId);
			jedis.expire("sessions:" + sessionId, EXPIRETIME);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	public boolean deleteUserSession(String sessionToken, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		boolean sucess = false;
		try {
			Set<String> sessionIds = jedis.smembers("user:sessions:" + userId);
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				String sessionId = it.next();
				if (sessionId.equalsIgnoreCase(sessionToken)) {
					jedis.srem("user:sessions:" + userId, sessionId);
					jedis.srem("sessions", sessionId);
					jedis.del("sessions:" + sessionId);
					sucess = true;
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sucess;
	}

	public String getUserSession(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		String sessionId = null;
		try {
			Set<String> sessionIds = jedis.smembers("sessions");
			Iterator<String> it = sessionIds.iterator();
			while (it.hasNext()) {
				String id = it.next();
				Map<String, String> sessionFields = jedis.hgetAll("sessions:" + id);
				for (Entry<String, String> entry : sessionFields.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("userId") && jedis.exists("sessions:" + id))
						if (entry.getValue().equalsIgnoreCase(userId))
							sessionId = id;
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sessionId;
	}

	public boolean deleteAllUserSessions(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		boolean sucess = false;
		try {
			if (jedis.exists("user:sessions:" + userId)) {
				jedis.del("user:sessions:" + userId);
				sucess = true;
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return sucess;
	}

	public Set<String> getAllUserSessions(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER, Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		Set<String> userSessions = null;
		try {
			if (jedis.exists("user:sessions:" + userId)) {
				userSessions = jedis.smembers("user:sessions:" + userId);
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return userSessions;
	}

	public boolean sessionExistsForUser(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_SESSION_SERVER,	Const.REDIS_SESSION_PORT);
		Jedis jedis = pool.getResource();
		boolean exists = false;
		try {
			Set<String> sessionIds = jedis.smembers("user:sessions:" + userId);
			Iterator<String> it = sessionIds.iterator();
			if (it.hasNext())
				exists = true;
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return exists;
	}

	private String getAppUsingSessionToken(String sessionToken) {
		return jedis.hget("sessions:" + sessionToken, "appId");
	}

	public String getUserUsingSessionToken(String sessionToken) {
		return jedis.hget("sessions:" + sessionToken, "userId");
	}
}
