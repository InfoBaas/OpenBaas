package infosistema.openbaas.dataaccess.sessions;

import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.dataaccess.models.Model;

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
public class RedisSessions implements SessionInterface {
	private static final int EXPIRETIME = 86400; // 24hours in seconds
	public static final long MAXCACHESIZE = 5242880; // bytes
	private static final int RedisSessionsAndEmailPORT = 6380;
	//private static final int MAXIMUMDISTANCE = 1; // 1 Km
	Jedis jedis;
	private final static String server = "localhost";

	public RedisSessions() {
		jedis = new Jedis(server, RedisSessionsAndEmailPORT);
	}

	public void createAdmin(String OPENBAASADMIN, byte[] adminSalt,
			byte[] adminHash) throws UnsupportedEncodingException {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",	RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset(OPENBAASADMIN, "adminSalt", new String(adminSalt,"ISO-8859-1"));
			jedis.hset(OPENBAASADMIN, "adminHash", new String(adminHash,"ISO-8859-1"));
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	public Map<String, String> getAdminFields(String OPENBAASADMIN) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",	RedisSessionsAndEmailPORT);
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

	public boolean adminExists(String admin) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	/**
	 * For sessions we use Redis expire mechanism, keys with more than 24 hours
	 * are automatically removed.
	 * 
	 * @param sessionId
	 * @param userId
	 */
	public void createSession(String sessionId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",	RedisSessionsAndEmailPORT);
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

	@Override
	public void createSession(String sessionId, String appId, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public Map<String, String> getSessionFields(String sessionId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public boolean sessionTokenExists(String sessionId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public boolean sessionTokenExistsForUser(String sessionToken, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public void deleteAdminSession(String adminId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public void createAdminSession(String sessionId, String adminId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public boolean deleteUserSession(String sessionToken, String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	/**
	 * Updates the session time, if it had 10 hours left until being deleted
	 * after we call refreshSession it will have EXPIRETIME until it is deleted
	 * (by default 24 hours).
	 */
	@Override
	public void refreshSession(String sessionToken, String date) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.expire("sessions:" + sessionToken, EXPIRETIME);
			jedis.hset("sessions:" + sessionToken, "date", date);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	public String getUserSession(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public boolean deleteAllUserSessions(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public Set<String> getAllUserSessions(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
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

	@Override
	public boolean sessionExistsForUser(String userId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",	RedisSessionsAndEmailPORT);
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

	@Override
	public void addLocationToSession(String location, String sessionToken, String userAgent) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("sessions:" + sessionToken, "location", location);
			jedis.hset("sessions:" + sessionToken, "userAgent", userAgent);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	// P1=(lat1, lon1) and P2=(lat2, lon2)
	// dist = arccos(sin(lat1) · sin(lat2) + cos(lat1) · cos(lat2) · cos(lon1 -
	// lon2)) · R
	@Override
	public boolean refreshSession(String sessionToken, String location,
			String date, String userAgent) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost", RedisSessionsAndEmailPORT);
		Jedis jedis = pool.getResource();
		try {
			jedis.expire("sessions:" + sessionToken, EXPIRETIME);
			jedis.hset("sessions:" + sessionToken, "date", date);
			String previousLocation = jedis.hget("sessions:" + sessionToken, "location");
			if (previousLocation == null) { // No previous Location, we simply
											// add it.
				addLocationToSession(location, sessionToken, userAgent);
			} else { // Calculate the distances
				// Split the data
				// previous location
				String[] previousLocationArray = previousLocation.split(":");
				double previousLatitudeValue, previousLongitudeValue,currentLatitudeValue,currentLongitudeValue;
				try{
				previousLatitudeValue = Double.parseDouble(previousLocationArray[0]);
				previousLongitudeValue = Double.parseDouble(previousLocationArray[1]);

				// Current Location
				String[] currentLocationArray = location.split(":");
				
				currentLatitudeValue = Double.parseDouble(currentLocationArray[0]);
				currentLongitudeValue = Double.parseDouble(currentLocationArray[1]);
				}catch(NumberFormatException e){
					return false;
				}
				Geolocation geo = new Geolocation();
				// Test if distance < MAXIMUM DISTANCE Spherical Law of Cosines
				double dist = geo.distance(previousLatitudeValue, previousLongitudeValue,currentLatitudeValue, currentLongitudeValue);
				if (dist < 1) { // The user is not taking the device with him.
					jedis.hset("sessions:" + sessionToken, "location", location);
					//refreshSession(sessionToken, date);
				} else { // the user is taking the device with him, update the
							// user location/date aswell.
					jedis.hset("sessions:" + sessionToken, "location", location);
					String userId = this.getUserUsingSessionToken(sessionToken);
					String appId = this.getAppUsingSessionToken(sessionToken);
					Model model = Model.getModel();
					model.updateUserLocationAndDate(userId, appId, sessionToken, location, date);
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return true;
	}
	

	private String getAppUsingSessionToken(String sessionToken) {
		return jedis.hget("sessions:" + sessionToken, "appId");
	}

	@Override
	public String getUserUsingSessionToken(String sessionToken) {
		return jedis.hget("sessions:" + sessionToken, "userId");
	}
}
