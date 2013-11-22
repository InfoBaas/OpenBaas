package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.utils.Const;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
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
	 * @throws UnsupportedEncodingException 
	 */
	public Boolean createApp(String appId, String appKey, byte[] hash, byte[] salt, String appName, String creationDate, 
			Boolean confirmUsersEmail, Boolean AWS, Boolean FTP, Boolean FileSystem) throws UnsupportedEncodingException {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		try {
			if (!jedis.exists("apps:" + appId)) {
				jedis.hset("apps:" + appId, "creationDate", creationDate);
				jedis.hset("apps:" + appId, "updateDate", creationDate);
				jedis.hset("apps:" + appId, "alive", "true");
				jedis.hset("apps:" + appId, "appName", appName);
				jedis.hset("apps:" + appId, "appKey", appKey);
				jedis.hset("apps:" + appId, "salt", new String(salt, "ISO-8859-1"));
				jedis.hset("apps:" + appId, "hash", new String(hash, "ISO-8859-1"));
				jedis.hset("apps:" + appId, "confirmUsersEmail", ""+confirmUsersEmail);
				jedis.hset("apps:" + appId, FileMode.aws.toString(), "" + AWS);
				jedis.hset("apps:" + appId, FileMode.ftp.toString(), "" + FTP);
				jedis.hset("apps:" + appId, FileMode.filesystem.toString(), "" + FileSystem);
				sucess = true;
			}
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;

	}


	// *** UPDATE *** //
	
	public Boolean updateAppFields(String appId, String alive, String newAppName, Boolean confirmUsersEmail,
			Boolean aws, Boolean ftp, Boolean fileSystem) {
		Jedis jedis = pool.getResource();
		try {
			if (newAppName != null)
				jedis.hset("apps:" + appId, "appName", newAppName);
			if (alive != null)
				jedis.hset("apps:" + appId, "alive", alive);
			if (newAppName != null)
				jedis.hset("apps:" + appId, "appName", newAppName);
			if (confirmUsersEmail != null)
				jedis.hset("apps:" + appId, "confirmUsersEmail", ""+confirmUsersEmail);
			if (fileSystem != null && fileSystem)
				aws = ftp = false;
			if (aws != null && aws)
				fileSystem = ftp = false;
			if (ftp != null && ftp)
				fileSystem = aws = false;
			if (aws != null)
				jedis.hset("apps:" + appId, FileMode.aws.toString(), ""+aws);
			if (ftp != null)
				jedis.hset("apps:" + appId, FileMode.ftp.toString(), ""+ftp);
			if (fileSystem != null)
				jedis.hset("apps:" + appId, FileMode.filesystem.toString(), ""+fileSystem);
		} finally {
			pool.returnResource(jedis);
		}
		return true;
	}


	// *** GET LIST *** //

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		// TODO Auto-generated method stub
		return null;
	}

	
	// *** GET *** //

	/**
	 * Returns the fields of the corresponding application
	 */
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
					else if (entry.getKey().equalsIgnoreCase(FileMode.aws.toString()))
						res.setAWS(Boolean.parseBoolean(entry.getValue()));
					else if (entry.getKey().equalsIgnoreCase(FileMode.ftp.toString()))
						res.setFTP(Boolean.parseBoolean(entry.getValue()));
					else if (entry.getKey().equalsIgnoreCase(FileMode.filesystem.toString()))
						res.setFileSystem(Boolean.parseBoolean(entry.getValue()));
					else if (entry.getKey().equalsIgnoreCase("updateDate"))
						res.setUpdateDate(entry.getValue());
					else if (entry.getKey().equalsIgnoreCase("appKey"))
						res.setAppKey(entry.getValue());
				}
				res.setAppId(appId);
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	/**
	 * Returns the auth fields 
	 */
	public HashMap<String, String> getApplicationAuth(String appId) {
		Jedis jedis = pool.getResource();
		HashMap<String, String> fieldsAuth = new HashMap<String, String>();
		try {
			if (jedis.exists("apps:" + appId)) {
				fieldsAuth.put("hash", jedis.hget("apps:"+appId, "hash"));// = jedis.hgetAll("apps:" + appId);
				fieldsAuth.put("salt", jedis.hget("apps:"+appId, "salt"));
			}
		} finally {
			pool.returnResource(jedis);
		}
		return fieldsAuth;
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

	public FileMode getApplicationFileMode(String appId) {
		Jedis jedis = pool.getResource();
		boolean aws = false;
		boolean ftp = false;
		try {
			aws =  Boolean.parseBoolean(jedis.hget("apps:" + appId, FileMode.aws.toString()));
		} catch (Exception e) { }
		try {
			ftp = Boolean.parseBoolean(jedis.hget("apps:" + appId, FileMode.ftp.toString()));
		} catch (Exception e) { }
		
		if (aws) return FileMode.aws;
		else if (ftp) return FileMode.ftp;
		else return FileMode.filesystem;
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


	// *** OTHERS *** //

	public void reviveApp(String appId) {
		Jedis jedis = pool.getResource();
		try {
			jedis.hset("apps:" + appId, "alive", "true");
		} finally {
			pool.returnResource(jedis);
		}
	}

}
