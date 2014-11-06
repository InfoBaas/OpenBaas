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

import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class AppModel {

	// *** CONTRUCTORS *** //

	public AppModel() {
		pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
	}

	// isto é preciso?
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	// *** PRIVATE *** //
	
	private JedisPool pool;
	

	// *** CONSTANTS *** //
	
	private static final int MAXELEMS = 9999999;

	
	// *** KEYS *** //
	
	private static final String APP_KEY_FORMAT = "apps:%s";
	private static final String APP_CLIENTS_LIST_KEY_FORMAT = "apps_%sClientsList_";
	
	private String getAppKey(String appId) {
		return String.format(APP_KEY_FORMAT, appId);
	}
	
	private String getAppClientsListKey(String appId) {
		return String.format(APP_CLIENTS_LIST_KEY_FORMAT, appId);
	}

	
	// *** CREATE *** //
	
	/**
	 * Return codes: 1 = Created application -1 = Application exists;
	 * 
	 * @param appId
	 * @param creationDate
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public Application createApp(String appId, String appKeyId, byte[] hash, byte[] salt, String appName, String creationDate, 
			Boolean confirmUsersEmail, Boolean AWS, Boolean FTP, Boolean FileSystem, List<String> clientsList) throws UnsupportedEncodingException {
		Jedis jedis = pool.getResource();
		try {
			String appKey = getAppKey(appId);
			if (!jedis.exists(appKey)) {
				jedis.hset(appKey, Application.CREATION_DATE, creationDate);
				jedis.hset(appKey, Application.CREATION_DATE, creationDate);
				jedis.hset(appKey, Application.ALIVE, "true");
				jedis.hset(appKey, Application.APP_NAME, appName);
				jedis.hset(appKey, Application.APP_KEY, appKeyId);
				jedis.hset(appKey, Application.SALT, new String(salt, "ISO-8859-1"));
				jedis.hset(appKey, Application.HASH, new String(hash, "ISO-8859-1"));
				jedis.hset(appKey, Application.CONFIRM_USERS_EMAIL, ""+confirmUsersEmail);
				jedis.hset(appKey, FileMode.aws.toString(), "" + AWS);
				jedis.hset(appKey, FileMode.ftp.toString(), "" + FTP);
				jedis.hset(appKey, FileMode.filesystem.toString(), "" + FileSystem);
				if (clientsList != null && clientsList.size()>0){
					Iterator<String> it = clientsList.iterator();
					while(it.hasNext()){
						jedis.lpush(getAppClientsListKey(appId), it.next());
					}
				}
				return getApplication(appId);
			}
		} catch (Exception e) {
			Log.error("", this, "createApp", "Error creating app.", e);
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}
	
	public JSONObject createAppResolutions(JSONObject res, String appId, ModelEnum type) {
		Jedis jedis = pool.getResource();
		try {
			String appKey = getAppKey(appId);
			jedis.del(appKey + ":"+type.toString());
			Iterator<?> keys = res.keys();
			while(keys.hasNext()){
				String key = (String)keys.next();
				String value = res.getString(key);
				jedis.hset(appKey + ":"+type.toString(), key, value);
			}
			return res;
		} catch (Exception e) {
			Log.error("", this, "createAppImageResolutions", "Error.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		return null;
	}

	
	// *** UPDATE *** //
	
	public Application updateAppFields(String appId, String alive, String newAppName, Boolean confirmUsersEmail,
			Boolean aws, Boolean ftp, Boolean fileSystem,List<String> clientsList) {
		Jedis jedis = pool.getResource();
		try {
			String appKey = getAppKey(appId);
			String appClientListKey = getAppClientsListKey(appId);
			if (newAppName != null)
				jedis.hset(appKey, Application.APP_NAME, newAppName);
			if (alive != null)
				jedis.hset(appKey, Application.ALIVE, alive);
			if (newAppName != null)
				jedis.hset(appKey, Application.APP_NAME, newAppName);
			if (confirmUsersEmail != null)
				jedis.hset(appKey, Application.CONFIRM_USERS_EMAIL, ""+confirmUsersEmail);
			if (fileSystem != null && fileSystem)
				aws = ftp = false;
			if (aws != null && aws)
				fileSystem = ftp = false;
			if (ftp != null && ftp)
				fileSystem = aws = false;
			if (aws != null)
				jedis.hset(appKey, FileMode.aws.toString(), ""+aws);
			if (ftp != null)
				jedis.hset(appKey, FileMode.ftp.toString(), ""+ftp);
			if (fileSystem != null)
				jedis.hset(appKey, FileMode.filesystem.toString(), ""+fileSystem);
			if (clientsList != null && clientsList.size()>0){
				jedis.del(appClientListKey);
				Iterator<String> it = clientsList.iterator();
				while(it.hasNext()){
					jedis.lpush(appClientListKey,it.next());
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		return getApplication(appId);
	}


	// *** GET LIST *** //

	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		// TODO Auto-generated method stub
		return null;
	}

	
	// *** GET *** //

	public String getFileQuality(String appId, ModelEnum type, String key) {
		String res=null;
		if(key!=null){
			Jedis jedis = pool.getResource();
			try {
				String appKey = getAppKey(appId);
				if (jedis.exists(appKey + ":"+type.toString())) {
					res = jedis.hget(appKey + ":"+type.toString(),key);
				}
			} finally {
				pool.returnResource(jedis);
			}
		}
		return res;
	}
	
	/**
	 * Returns the fields of the corresponding application
	 */
	public Application getApplication(String appId) {
		Jedis jedis = pool.getResource();
		Application res = new Application();
		Map<String, String> fields = null;
		Map<String, String> imageRes = null;
		Map<String, String> videoRes = null;
		Map<String, String> audioRes = null;
		Map<String, String> barsColors = null;
		List<String> clients = null;
		try {
			String appKey = getAppKey(appId);
			if (jedis.exists(appKey)) {
				fields = jedis.hgetAll(appKey);
				imageRes = jedis.hgetAll(appKey + ":"+ModelEnum.image);
				videoRes = jedis.hgetAll(appKey + ":"+ModelEnum.video);
				audioRes = jedis.hgetAll(appKey + ":"+ModelEnum.audio);
				barsColors = jedis.hgetAll(appKey + ":"+ModelEnum.bars);
				clients = jedis.lrange(getAppClientsListKey(appId), 0, MAXELEMS);
			}
			if (fields != null) {
				res.setCreationDate(fields.get(Application.CREATION_DATE));
				res.setAlive(fields.get(Application.ALIVE));
				res.setAppName(fields.get(Application.APP_NAME));
				res.setConfirmUsersEmail(Boolean.parseBoolean(fields.get(Application.CONFIRM_USERS_EMAIL)));
				res.setAWS(Boolean.parseBoolean(fields.get(FileMode.aws.toString())));
				res.setFTP(Boolean.parseBoolean(fields.get(FileMode.ftp.toString())));
				res.setFileSystem(Boolean.parseBoolean(fields.get(FileMode.filesystem.toString())));
				res.setUpdateDate(fields.get(Application.CREATION_DATE));
				res.setAppKey(fields.get(Application.APP_KEY));
				res.set_id(appId);
			}
			if(imageRes!=null){
				res.setImageResolutions(imageRes);
			}
			if(videoRes!=null){
				res.setVideoResolutions(videoRes);
			}
			if(audioRes!=null){
				res.setAudioResolutions(audioRes);
			}
			if(barsColors!=null){
				res.setBarsColors(barsColors);
			}
			if(clients!=null && clients.size()>0){
				res.setClients(clients);
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
			String appKey = getAppKey(appId);
			if (jedis.exists(appKey)) {
				fieldsAuth.put(Application.HASH, jedis.hget(appKey, Application.HASH));
				fieldsAuth.put(Application.SALT, jedis.hget(appKey, Application.SALT));
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
			confirmUsersEmail = Boolean.parseBoolean(jedis.hget(getAppKey(appId), Application.CONFIRM_USERS_EMAIL));
		}finally {
			pool.returnResource(jedis);
		}
		return confirmUsersEmail;
	}

	public FileMode getApplicationFileMode(String appId) {
		Jedis jedis = pool.getResource();
		boolean aws = false;
		boolean ftp = false;
		String appKey = getAppKey(appId);
		try{
			try {
				aws =  Boolean.parseBoolean(jedis.hget(appKey, FileMode.aws.toString()));
			} catch (Exception e) { }
			try {
				ftp = Boolean.parseBoolean(jedis.hget(appKey, FileMode.ftp.toString()));
			} catch (Exception e) { }	
		}finally {
			pool.returnResource(jedis);
		}
		
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
			String appKey = getAppKey(appId);
			if (jedis.exists(appKey)) {
				Set<String> inactiveApps = jedis.smembers("apps:inactive");
				Iterator<String> it = inactiveApps.iterator();
				Boolean inactive = false;
				while (it.hasNext() && !inactive) {
					if (it.next().equals(appId))
						inactive = true;
				}
				if (!inactive) {
					jedis.hset(appKey, Application.ALIVE, "false");
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
			op = jedis.exists(getAppKey(appId));
		}finally {
			pool.returnResource(jedis);
		}
		return op;
	}


	// *** OTHERS *** //

	public void reviveApp(String appId) {
		Jedis jedis = pool.getResource();
		try {
			jedis.hset(getAppKey(appId), Application.ALIVE, "true");
		} finally {
			pool.returnResource(jedis);
		}
	}

	
	
}
