package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.utils.Const;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class AppModel {

	// request types
	private JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
	Jedis jedis;
	
	public AppModel() {
		jedis = new Jedis(Const.getRedisGeneralServer(), Const.getRedisGeneralPort());
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// *** *** APPS *** *** //
	
	// *** PRIVATE *** //
	// *** CREATE *** //

	
	/**
	 * Return codes: 1 = Created application -1 = Application exists;
	 * 
	 * @param appId
	 * @param creationDate
	 * @return
	 */
	public Boolean createApp(String appId, String appName, String creationDate, Boolean confirmUsersEmail, Boolean AWS, Boolean FTP, Boolean FileSystem) {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		try {
			if (!jedis.exists("apps:" + appId)) {
				jedis.hset("apps:" + appId, "creationDate", creationDate);
				jedis.hset("apps:" + appId, "updateDate", creationDate);
				jedis.hset("apps:" + appId, "alive", "true");
				jedis.hset("apps:" + appId, "appName", appName);
				jedis.hset("apps:" + appId, "confirmUsersEmail", ""+confirmUsersEmail);
				jedis.hset("apps:" + appId, "AWS", ""+AWS);
				jedis.hset("apps:" + appId, "FTP", ""+FTP);
				jedis.hset("apps:" + appId, "FileSystem", ""+FileSystem);
				long unixTime = System.currentTimeMillis() / 1000L;
				jedis.zadd("apps:time", unixTime, appId);
				sucess = true;
			}
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;

	}


	// *** UPDATE *** //
	
	/**
	 * Return codes 1 = Updated application successfully; -1 = Application with
	 * currentId does not exist.
	 * 
	 * @return
	 */
	public Boolean updateApp(String currentId, String newId, String alive) {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		try {
			if (jedis.exists("apps:" + currentId)) {
				jedis.zrem("apps:time", currentId);
				long unixTime = System.currentTimeMillis() / 1000L;
				jedis.zadd("apps:time", unixTime, newId);
				Map<String, String> tempValues = jedis.hgetAll("apps:"
						+ currentId);
				for (Map.Entry<String, String> entry : tempValues.entrySet()) {
					jedis.hset("apps:" + newId, entry.getKey(),
							entry.getValue());
				}
				jedis.del("apps:" + currentId);
				sucess = true;
			}
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;
	}

	/**
	 * Makes an inactive app turn active again.
	 * 
	 * @param alive
	 * @return
	 */
	public Boolean updateAppName(String appId, String newAppName) {
		Jedis jedis = pool.getResource();
		try {
			long unixTime = System.currentTimeMillis() / 1000L;
			jedis.zadd("apps:time", unixTime, appId);
			jedis.hset("apps:" + appId, "appName", newAppName);
		} finally {
			pool.returnResource(jedis);
		}
		return true;
	}

	public Boolean updateAllAppFields(String appId, String alive,
			String newAppName, Boolean confirmUsersEmail,boolean AWS,boolean FTP,boolean FileSystem) {
		Jedis jedis = pool.getResource();
		try {
			long unixTime = System.currentTimeMillis() / 1000L;
			jedis.zadd("apps:time", unixTime, appId);
			jedis.hset("apps:" + appId, "appName", newAppName);
			jedis.hset("apps:" + appId, "alive", alive);
			jedis.hset("apps:" + appId, "appName", newAppName);
			jedis.hset("apps:" + appId, "confirmUsersEmail", ""+confirmUsersEmail);
			jedis.hset("apps:" + appId, "AWS", ""+AWS);
			jedis.hset("apps:" + appId, "FTP", ""+FTP);
			jedis.hset("apps:" + appId, "FileSystem", ""+FileSystem);
			jedis.hset("apps:" + appId, "updateDate", new Date().toString());
		} finally {
			pool.returnResource(jedis);
		}
		return true;
	}

	public Boolean updateConfirmUsersEmailOption(String appId, Boolean confirmUsersEmail) {
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("apps:" + appId, "confirmUsersEmail", ""+confirmUsersEmail);
		} finally {
			pool.returnResource(jedis);
		}
		return true;
	}


	// *** GET LIST *** //

	/*
	public Set<String> getAllAppIds() {
		Jedis jedis = pool.getResource();
		Set<String> result;
		try {
			Set<String> allApps = jedis.keys("apps:");
			Set<String> inactiveApps = jedis.smembers("apps:inactive");
			result = new HashSet<String>(allApps.size());
			Iterator<String> i = allApps.iterator();
			while (i.hasNext()) {
				String element = i.next();
				if (!inactiveApps.contains(element))
					result.add(element);
			}
		} finally {
			pool.returnResource(jedis);
		}
		return result;
	}
	*/

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		// TODO Auto-generated method stub
		return null;
	}


	// *** GET *** //

	/**
	 * Returns the fields of the corresponding application
	 *//*
	public Map<String, String> getApplication(String appId) {
		Jedis jedis = pool.getResource();
		Map<String, String> appFields = null;
		try {
			if (jedis.exists("apps:" + appId)) {
				appFields = jedis.hgetAll("apps:" + appId);
			}
		} finally {
			pool.returnResource(jedis);
		}
		return appFields;
	}*/
	
	public Application getApplication(String appId) {
		Jedis jedis = pool.getResource();
		Application res = new Application();
		Map<String, String> fields = null;
		try {
			if (jedis.exists("apps:" + appId)) {
				fields = jedis.hgetAll("apps:" + appId);
			}
			if (fields != null) {
				for (Entry<String, String> entry : fields.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("creationdate"))
						res.setCreationDate(entry.getValue());
					else if (entry.getKey().equalsIgnoreCase("alive"))
						res.setAlive(entry.getValue());
					else if (entry.getKey().equalsIgnoreCase("appName"))
						res.setAppName(entry.getValue());
					else if (entry.getKey().equalsIgnoreCase("confirmUsersEmail"))
						res.setConfirmUsersEmail(Boolean.parseBoolean(entry.getValue()));
					else if (entry.getKey().equalsIgnoreCase("AWS"))
						res.setAWS(Boolean.parseBoolean(entry.getValue()));
					else if (entry.getKey().equalsIgnoreCase("FTP"))
						res.setFTP(Boolean.parseBoolean(entry.getValue()));
					else if (entry.getKey().equalsIgnoreCase("FileSystem"))
						res.setFileSystem(Boolean.parseBoolean(entry.getValue()));
					else if (entry.getKey().equalsIgnoreCase("updateDate"))
						res.setUpdateDate(entry.getValue());
				}
				res.setAppId(appId);
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean getConfirmUsersEmail(String appId) {
		Jedis jedis = pool.getResource();
		Boolean confirmUsersEmail = false;
		try {
			confirmUsersEmail = Boolean.parseBoolean(this.jedis.hget("apps:"+appId, "confirmUsersEmail"));
		}finally {
			pool.returnResource(jedis);
		}
		return confirmUsersEmail;
	}


	// *** DELETE *** //

	/**
	 * Return codes 1 = Action performed -1 = App does not exist 0 = No action
	 * was performed
	 * 
	 */
	public Boolean deleteApp(String appId) {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		try {
			if (jedis.exists("apps:" + appId)) {
				//long unixTime = System.currentTimeMillis() / 1000L;
				jedis.zrem("apps:time", appId);
				Set<String> inactiveApps = jedis.smembers("apps:inactive");
				Iterator<String> it = inactiveApps.iterator();
				Boolean inactive = false;
				while (it.hasNext() && !inactive) {
					if (it.next().equals(appId))
						inactive = true;
				}
				if (!inactive) {
					jedis.hset("apps:" + appId, "alive", "false");
					jedis.sadd("apps:inactive", appId);
					sucess = true;
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;
	}


	// *** EXISTS *** //

	public Boolean appExists(String appId) {
		Jedis jedis = pool.getResource();
		Boolean op;
		try {
			op = jedis.exists("apps:" + appId);
		}finally {
			pool.returnResource(jedis);
		}
		return op;
	}

	public void reviveApp(String appId) {
		Jedis jedis = pool.getResource();
		try {
			long unixTime = System.currentTimeMillis() / 1000L;
			jedis.zadd("apps:time", unixTime, appId);
			jedis.hset("apps:" + appId, "alive", "true");
		} finally {
			pool.returnResource(jedis);
		}
	}


	// *** OTHERS *** //

}