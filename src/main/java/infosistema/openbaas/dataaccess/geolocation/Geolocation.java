package infosistema.openbaas.dataaccess.geolocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import infosistema.openbaas.data.ModelEnum;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.ValueComparator;

public class Geolocation {

	private static final String OBJECTID_FORMAT = "%s:%s:%s"; // latitude;longitude;objectid
	private static final String GRID_FORMAT = "%s:%s:%s:%s"; // latitude;longitude;appid;objecttype
	
	//TODO: fazer por app
	private double latp = Const.LATITUDE_PRECISION;
	private double longp = Const.LONGITUDE_PRECISION;

	/*
	final static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'b', 'c', 'd', 'e',
			'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
	 */
	private static Geolocation instance = null;
	
	private Geolocation() {
	}
	
	public static Geolocation getInstance() {
		if (instance == null) instance = new Geolocation();
		return instance;
	}

	// *** AUX *** //
	
	private double correctLatitude(double latitude) {
		return latitude + 90;
	}
	
	private double correctLongitude(double longitude) {
		return longitude + 180;
	}
	
	private double getGridLatitude(double latitude) {
		return ((int)(latitude / latp)) * latp; 
	}

	private double getGridLongitude(double longitude) {
		return ((int)(longitude / longp)) * longp; 
	}

	private String getObjectId(double latitude, double longitude, String objectId) {
		return String.format(OBJECTID_FORMAT, latitude, longitude, objectId);
	}
	
	private String getGridSquareId(double latitude, double longitude, String appId, ModelEnum objectType) {
		String aux = String.format(GRID_FORMAT, latitude, longitude, appId, objectType.toString());
		//String twoPoints = ":";
		//String aux = latitude+twoPoints+longitude+twoPoints+appId+twoPoints+objectType;	
		return aux;
	}

	//Distances 
	private double transformMetersInDegreesLat(double meters) {
		return ((meters/1000)/110.54);
	}

	private double transformMetersInDegreesLong(double meters, double currentLat) {
		return Math.abs((meters/1000)/(111.320*Math.cos(currentLat)));
	}

	private double getDistanceFromLatLonInKm(double lat1,double lon1,double lat2,double lon2) {
		double R = 6371; // Radius of the earth in km
		  double dLat = deg2rad2(lat2-lat1);  // deg2rad below
		  double dLon = deg2rad2(lon2-lon1); 
		  double a = 
		    Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(deg2rad(lat1)) * 
		    Math.cos(deg2rad(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2); 
		  double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		  double d = R * c; // Distance in km
		  return d;
	}

	//TODO: Porquê duas funções????
	private double deg2rad2(double deg) {
	  return deg * (Math.PI/180);
	}

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}


	// *** CREATE *** //
	
	public boolean insertObjectInGrid(double latitude, double longitude, ModelEnum type, String appId, String objectId) {
		String gridObjectId = getObjectId(latitude, longitude, objectId);

		latitude = correctLatitude(latitude);
		longitude = correctLongitude(longitude);
		double gridLatitude = getGridLatitude(latitude);
		double gridLongitude = getGridLongitude(longitude);

		String gridSquareId = getGridSquareId(gridLatitude, gridLongitude, appId, type);

		return insert(gridSquareId, gridObjectId);
	}

	//private
	private boolean insert(String gridSquareId, String gridObjectId) {
		Boolean success = false;
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_GEO_SERVER, Const.REDIS_GEO_PORT);
		Jedis jedis = pool.getResource();
		try{
			jedis.sadd(gridSquareId, gridObjectId);
			success = true;
		} catch (Exception e) {
			Log.error("", this, "insert", "An error ocorred.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return success;
	}
	

	// *** UPDATE *** //
	
	public boolean updateObjectInGrid(double srcLatitude, double srcLongitude, double destLatitude, double destLongitude, ModelEnum type, String appId, String objectId) {
		String srcGridObjectId = getObjectId(srcLatitude, srcLongitude, objectId);
		String destGridObjectId = getObjectId(destLatitude, destLongitude, objectId);

		srcLatitude = correctLatitude(srcLatitude);
		srcLongitude = correctLongitude(srcLongitude);
		destLatitude = correctLatitude(destLatitude);
		destLongitude = correctLongitude(destLongitude);
		double srcGridLatitude = getGridLatitude(srcLatitude);
		double srcGridLongitude = getGridLongitude(srcLongitude);
		double destGridLatitude = getGridLatitude(destLatitude);
		double destGridLongitude = getGridLongitude(destLongitude);

		String srcGridSquareId = getGridSquareId(srcGridLatitude, srcGridLongitude, appId, type);
		String destGridSquareId = getGridSquareId(destGridLatitude, destGridLongitude, appId, type);

		delete(srcGridSquareId, srcGridObjectId);
		insert(destGridSquareId, destGridObjectId);
		
		return true;
	}
	

	// *** DELETE *** //
	
	public boolean deleteObjectFromGrid(double latitude, double longitude, ModelEnum type, String appId, String objectId) {
		String gridObjectId = getObjectId(latitude, longitude, objectId);

		latitude = correctLatitude(latitude);
		longitude = correctLongitude(longitude);
		double gridLatitude = getGridLatitude(latitude);
		double gridLongitude = getGridLongitude(longitude);

		String gridSquareId = getGridSquareId(gridLatitude, gridLongitude, appId, type);

		return delete(gridSquareId, gridObjectId);
	}
	
	//private
	private boolean delete(String gridSquareId, String gridObjectId) {
		Boolean success = false;
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_GEO_SERVER, Const.REDIS_GEO_PORT);
		Jedis jedis = pool.getResource();
		try{
			jedis.srem(gridSquareId, gridObjectId);
			success = true;
		}catch(Exception e){
			Log.error("", this, "delete", "An error ocorred.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return success;
	}

	
	// *** GET LIST *** //

	public ArrayList<String> getObjectsInGrid(double latitudeIni, double longitudeIni, double latitudeEnd, double longitudeEnd, String appId, ModelEnum type) {
		ArrayList<String> retObj = new ArrayList<String>();
		latitudeIni = correctLatitude(latitudeIni);
		longitudeIni = correctLongitude(longitudeIni);
		latitudeEnd = correctLatitude(latitudeEnd);
		longitudeEnd = correctLongitude(longitudeEnd);
		double gridLatitudeIni = getGridLatitude(latitudeIni);
		double gridLongitudeIni = getGridLongitude(longitudeIni);
		double gridLatitudeEnd = getGridLatitude(latitudeEnd);
		double gridLongitudeEnd = getGridLongitude(longitudeEnd);

		while (gridLatitudeIni <= gridLatitudeEnd) {
			while (gridLongitudeIni <= gridLongitudeEnd) {
				String gridSquareId = getGridSquareId(gridLatitudeIni, gridLongitudeIni, appId, type);
				retObj.addAll(getObjectsIn(gridSquareId));
				gridLongitudeIni += longp;
			}
			gridLatitudeIni += latp;
		}
		return retObj;
	}

	public ArrayList<String> getObjectsInDistance(double latitude, double longitude, double radius, String appId, ModelEnum type) {
		ArrayList<String> retObj = new ArrayList<String>(); 

		double latitudeIni = latitude-transformMetersInDegreesLat(radius); 
		double latitudeEnd = latitude+transformMetersInDegreesLat(radius); 
		double longitudeIni = longitude-transformMetersInDegreesLong(radius, latitudeIni); 
		double longitudeEnd = longitude+transformMetersInDegreesLong(radius, latitudeEnd); 
		ArrayList<String> objectsInGrid = getObjectsInGrid(latitudeIni, longitudeIni, latitudeEnd, longitudeEnd, appId, type);
		HashMap<String,Double> elementsToOrder = new HashMap<String, Double>();
		for (String object : objectsInGrid) {
			String[] objectArray = object.split(":");
			Double objLatitude = Double.parseDouble(objectArray[0]);
			Double objLongitude = Double.parseDouble(objectArray[1]);
			String objId = objectArray[2];
			double dist2Or = getDistanceFromLatLonInKm(latitude, longitude, objLatitude, objLongitude);
			if( dist2Or <= (radius / 1000)) {
				elementsToOrder.put(objId, dist2Or);
		    }
		}
		retObj = orderHash(elementsToOrder);

		return retObj;
	}

	//private
	private Set<String> getObjectsIn(String gridSquareId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.REDIS_GEO_SERVER, Const.REDIS_GEO_PORT);
		Jedis jedis = pool.getResource();
		Set<String> retObj = new TreeSet<String>();
		try {
			//if (jedis.exists("sessions:" + sessionId))
			retObj = jedis.smembers(gridSquareId);
		} finally {
			pool.returnResource(jedis);
			pool.destroy();
		}
		return retObj;
	}
	
	private ArrayList<String> orderHash(HashMap<String,Double> hash2Order) {
	{  
			ArrayList<String> res = new ArrayList<String>();
	        ValueComparator bvc =  new ValueComparator(hash2Order);
	        TreeMap<String,Double> sorted_map = new TreeMap<String,Double>(bvc);
	        sorted_map.putAll(hash2Order);
	        for (Map.Entry<String,Double> entry : sorted_map.entrySet()) {
	        	res.add(entry.getKey());
	        }
	        return res ;
	    }
    } 

	// *** GET *** //
	
	// *** OTHERS *** //
	
	// http://www.geodatasource.com/developers/java
	// return kilometers
	public double distance(double lat1, double lon1, double lat2, double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
				* Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		dist = dist * 1.609344;
		return (dist);
	}

}
