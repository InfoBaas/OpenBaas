package infosistema.openbaas.dataaccess.models.cache;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.utils.Const;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class MediaModelCache {

	// request types
	private static final String ID_FORMAT = "%s:%s";
	private static final String APP_ID_FORMAT = "app:%s:%s";
	private JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
	
	public MediaModelCache() {
	}

	// *** *** MEDIA *** *** //
	
	// *** PRIVATE *** //
	
	private String getId(ModelEnum type, String objId) {
		return String.format(ID_FORMAT, type.toString(), objId);
	}

	private String getAppObjectId(ModelEnum type, String appId) {
		return String.format(APP_ID_FORMAT, type.toString(), appId);
	}
	
	// *** CREATE *** //
	
	public Boolean createMedia(String appId, ModelEnum type, String objId, Map<String, String> fields) {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		String appObjectId = getAppObjectId(type, appId);
		//String id = getId(type, objId);
		try {
			for (String key : fields.keySet()) {
				if(fields.get(key)!=null)
					jedis.hset(objId, key, fields.get(key));
			}
			jedis.sadd(appObjectId, objId);
			sucess = true;
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;
	}
	
	// *** UPDATE *** //

	
	// *** GET LIST *** //

	public List<String> getOperation(String appId, String attribute, String value, ModelEnum type) throws Exception {
		Jedis jedis = pool.getResource();
		List<String> listRes = new ArrayList<String>();
		try{
			Set<String> setUsers = jedis.smembers("app:"+type+":"+appId);
			Iterator<String> iter = setUsers.iterator();
			while(iter.hasNext()){
				String userId = iter.next();
				Map<String, String> mapMedia = getMedia(appId,type, userId);
				if(mapMedia.containsKey(attribute)){
					if(mapMedia.get(attribute).equals(value))
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

	public ArrayList<String> getAllMediaIds(String appId, ModelEnum type) {
		Jedis jedis = pool.getResource();
		ArrayList<String> mediaIds = new ArrayList<String>();
		try {
			String id = null;
			if (type == null || type == ModelEnum.audio) {
				id = getAppObjectId(ModelEnum.audio, appId);
				mediaIds.addAll(jedis.smembers(id));
			}
			if (type == null || type == ModelEnum.image) {
				id = getAppObjectId(ModelEnum.image, appId);
				mediaIds.addAll(jedis.smembers(id));
			}
			if (type == null || type == ModelEnum.video) {
				id = getAppObjectId(ModelEnum.video, appId);
				mediaIds.addAll(jedis.smembers(id));
			}
			if (type == ModelEnum.storage) {
				id = getAppObjectId(ModelEnum.storage, appId);
				mediaIds.addAll(jedis.smembers(id));
			}
		} finally {
			pool.returnResource(jedis);
		}
		return mediaIds;		
	}
	
	public Map<String, String> getMedia(String appId, ModelEnum type, String objId) {
		Jedis jedis = pool.getResource();
		Map<String, String> fields = null;
		try {
			Set<String> imagesInApp = jedis.smembers(getAppObjectId(type, appId));
			Iterator<String> it = imagesInApp.iterator();
			Boolean imageExistsForApp = false;
			while (it.hasNext())
				if (it.next().equalsIgnoreCase(objId))
					imageExistsForApp = true;
			if (imageExistsForApp)
				fields = jedis.hgetAll(objId);
		} finally {
			pool.returnResource(jedis);
		}
		return fields;
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
		Jedis jedis = pool.getResource();
		String fileDirectory = null;
		try {
			fileDirectory = jedis.hget(getAppObjectId(type, appId), field);
		} finally {
			pool.returnResource(jedis);
		}
		return fileDirectory;
	}


	// *** DELETE *** //
	
	public Boolean deleteMedia(String appId, ModelEnum type, String objId) {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		try {
			jedis.srem(getAppObjectId(type, appId));
			jedis.del(getId(type, objId));
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;
	}

	
	// *** EXISTS *** //
	
	public Boolean mediaExists(String appId, ModelEnum type, String objId) {
		Jedis jedis = pool.getResource();
		Boolean sucess = false;
		try {
			Set<String> audioInApp = jedis.smembers(getAppObjectId(type, appId));
			Iterator<String> it = audioInApp.iterator();
			while (it.hasNext())
				if (it.next().equalsIgnoreCase(objId)){
					sucess = true;
					break;
				}
					
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;
	}


	// *** OTHERS *** //

	public Integer countAllMedia(String appId, ModelEnum type) {
		// TODO Auto-generated method stub
		return null;
	}
}
