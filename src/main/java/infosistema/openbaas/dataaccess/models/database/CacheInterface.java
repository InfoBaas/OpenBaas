package infosistema.openbaas.dataaccess.models.database;

import java.util.Map;
import java.util.Set;

public interface CacheInterface extends DatabaseInterface{
	/**
	 * Gets the bytes used by the cache.
	 * @return
	 */
	public long getCacheSize();
	/**
	 * Retrieves the oldest element.
	 * @return
	 */
	public Map<String, String> getOldestElement();
	/**
	 * Deletes the oldest element.
	 */
	public void deleteOldestElement();
	/**
	 * Returns all the cached element ids.
	 */
	public Set<String> allCachedElements();
	void destroyPool();
}
