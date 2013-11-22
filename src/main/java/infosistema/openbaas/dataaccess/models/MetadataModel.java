package infosistema.openbaas.dataaccess.models;

import java.util.Map;
import java.util.Set;

import infosistema.openbaas.utils.Const;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class MetadataModel {

	// *** MEMBERS *** //

	private JedisPool pool;

	
	// *** CONSTRUCTOR *** //
	
	public MetadataModel() {
		pool = new JedisPool(new JedisPoolConfig(), Const.getRedisMetadataServer(), Const.getRedisMetadataPort());
	}
	
	
	// *** CREATE *** //
	
	// *** UPDATE *** //
	
	public boolean createUpdateMetadata(String key, Map<String, String> fields) {
		Jedis jedis = pool.getResource();
		try {
			for (String field: fields.keySet()) {
				if(fields.get(field) != null)
					jedis.hset(key, field, fields.get(field));
			}
			return true;
		} finally {
			pool.returnResource(jedis);
			pool.destroy();
		}
	}


	// *** DELETE *** //
	
	public boolean deleteMetadata(String key) {
		return deleteMetadata(key, false);
	}

	public boolean deleteMetadata(String key, boolean deleteSub) {
		Jedis jedis = pool.getResource();
		try {
			if (!deleteSub) {
				jedis.del(key);
			} else {
				Set<String> keys = jedis.keys(key + "*");
				for (String k : keys) {
					jedis.del(k);
				}
			}
			return true;
		} finally {
			pool.returnResource(jedis);
			pool.destroy();
		}
	}

	
	// *** GET LIST *** //
	
	// *** GET *** //
	
	public Map<String, String> getMetadata(String key) {
		Jedis jedis = pool.getResource();
		try {
			return jedis.hgetAll(key);
		} finally {
			pool.returnResource(jedis);
			pool.destroy();
		}
	}

	
	// *** EXISTS *** //
	
	public boolean existsMetadata(String key) {
		Jedis jedis = pool.getResource();
		try {
			return jedis.exists(key);
		} finally {
			pool.returnResource(jedis);
			pool.destroy();
		}
	}
	
	// *** OTHERS *** //

}
