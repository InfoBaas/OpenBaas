package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.utils.Const;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class MediaModel {

	// request types
	private static final String ID_FORMAT = "%s:%s";
	private static final String APP_ID_FORMAT = "app:%s:%s";
	private JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisGeneralServer(),Const.getRedisGeneralPort());
	
	public MediaModel() {
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
		String id = getId(type, objId);
		try {
			for (String key : fields.keySet()) {
				jedis.hset(id, key, fields.get(key));
			}
			jedis.sadd(appObjectId, id);
			sucess = true;
		} finally {
			pool.returnResource(jedis);
		}
		return sucess;
	}
	
	// *** UPDATE *** //

	
	// *** GET LIST *** //

	public List<String> getOperation(String appId, OperatorEnum oper, String attribute, String value, ModelEnum type) {
		//TODO IMPLEMENT
		return null;
	}

	//XPTO: PAGINAÇÃO
	/* Lixo apagar depois de consultar
 	public ArrayList<String> getAllMediaIds(String appId, ModelEnum type, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
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
	 */

	// *** GET *** //

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
				fields = jedis.hgetAll(getId(type, objId));
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
