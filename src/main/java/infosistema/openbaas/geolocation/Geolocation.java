package infosistema.openbaas.geolocation;

import infosistema.openbaas.dataModels.MongoDBDataModel;
import infosistema.openbaas.management.ValueComparator;
import infosistema.openbaas.utils.Const;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;



import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Geolocation implements GeoLocationOperations{
	
	private static final int numberGenerator = 7;
	final static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p',
			'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
	private final static String server = "localhost";
	public static final int MongoPort = 27017;
	private final static String MongoServer = "localhost";
	
	public Geolocation() {
	}

	double latp;
	double longp;

	public void createGridCache(double latPrecision, double longPrecision) {
		latp = latPrecision;
		longp = longPrecision;
	}

	private int determineLatitudeInGrid(double latitude) {
		double mid = latp / 2;
		double rem = latitude % latp;
		if (rem < mid) {
			return (int)(latitude - rem);
		} else {
			return (int)(latitude - rem + latp);
		}
	}

	private int determineLongitudeInGrid(double longitude) {
		double mid = longp / 2;
		double rem = longitude % longp;
		if (rem < mid) {
			return (int)(longitude - rem);
		} else {
			return (int)(longitude - rem + longp);
		}
	}


	public boolean insertObjectInGrid(double latitude, double longitude, String type, String objectId) {
		/*
		latitude += 90; // negative values
		longitude += 180;
		String latitudePointer = getLatitudeIndex(latitude);
		String longitudePointer = getLongitudeIndex(latitudePointer, longitude);
		String typePointer = getTypeIndex(latitudePointer, longitudePointer, longitude, type);
		createObject(latitudePointer, longitudePointer, typePointer, objectId);
		return true;*/
		return insertObjectInGridJM(latitude,longitude,type,objectId);
	}
	public boolean insertObjectInGridJM(double latitude, double longitude, String type, String objectId) {
		latitude += 90; // negative values
		longitude += 180;
		Boolean success = createObjectInGeo(latitude,longitude,type,objectId);
		return success;
	}

	private Boolean createObjectInGeo(double latitude, double longitude, String type, String objectId) {
		Boolean success=false;
		if(type.equals("jpg")) type = "image";
		if(type.equals("wmv")) type = "video";
		if(type.equals("mp3")) type = "audio";
		try{
			JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.SERVER, Const.REDIS_GEO_PORT);
			Jedis jedis = pool.getResource();
			if(type.equals("image")){
				jedis.zadd("latitudesImages",latitude, objectId);
				jedis.zadd("longitudesImages",longitude, objectId);
				//jedis.sadd("images", objectId);
				success = true;
			}
			if(type.equals("video")){
				jedis.zadd("latitudesVideos",latitude, objectId);
				jedis.zadd("longitudesVideos",longitude, objectId);
				//jedis.sadd("videos", objectId);
				success = true;
			}
			if(type.equals("audio")){
				jedis.zadd("latitudesAudios",latitude, objectId);
				jedis.zadd("longitudesAudios",longitude, objectId);
				//jedis.sadd("audios", objectId);
				success = true;
			}
		}catch(Exception e){
			System.err.println(e.toString());
		}
		return success;
	}

	private void createObject(String latitudePointer, String longitudePointer,
			String typePointer, String objectId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.SERVER, Const.REDIS_GEO_PORT);
		Jedis jedis = pool.getResource();
		try {
			if (!jedis.exists(latitudePointer + ":" + longitudePointer + ":"+ typePointer))
				jedis.sadd(latitudePointer + ":" + longitudePointer + ":" + typePointer, objectId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
	}

	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}


	private String getLatitudeIndex(double latitude) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.SERVER, Const.REDIS_GEO_PORT);
		Jedis jedis = pool.getResource();
		String id = null;
		try {
			// element does not exist, create it
			Set<String> elements = jedis.zrangeByScore("latitudes", latitude - latp, latitude + latp);
			if (elements.size() == 0) {
				id = getRandomString(numberGenerator);
				jedis.zadd("latitudes", /*determineLatitudeInGrid(latitude)*/latitude, id);
			} else {
				// check which of the elements is closest to the given latitude
				Iterator<String> it = elements.iterator();
				double closestSubtraction = 0;
				while (it.hasNext()) {
					String element = it.next();
					double score = jedis.zscore("latitudes", element);
					if (score - latitude < closestSubtraction) {
						closestSubtraction = score - latitude;
						id = element;
					}
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return id; // a new id if the latitude point does not exist, or the
					// closest latitude point within the
		// precision
	}

	private String getLongitudeIndex(String latitudePointer, double longitude) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.SERVER, Const.REDIS_GEO_PORT);
		Jedis jedis = pool.getResource();
		String id = null;
		try {
			// minimum: longitude - longitudePrecision
			// maximum: longitude + longitudePrecision
			// sorted set with key latPointer:longitudes
			Set<String> elements = jedis.zrangeByScore(latitudePointer + ":longitudes", longitude - longp, longitude + longp);
			if (elements.size() == 0) {
				id = getRandomString(numberGenerator);
				jedis.zadd(latitudePointer + ":longitudes", /*determineLongitudeInGrid(longitude)*/longitude, id);
			} else {
				// check which of the elements is closest to the given latitude
				Iterator<String> it = elements.iterator();
				double closestSubtraction = 0;
				while (it.hasNext()) {
					String element = it.next();
					double score = jedis.zscore(latitudePointer + ":longitudes", element);
					if (score - longitude < closestSubtraction) {
						closestSubtraction = score - longitude;
						id = element;
					}
				}
			}
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return id; // a new id if the longitude point does not exist, or the
					// closest longitude point within the
		// precision
	}

	private String getTypeIndex(String latitudePointer,	String longitudePointer, double longitude, String type) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.SERVER, Const.REDIS_GEO_PORT);
		Jedis jedis = pool.getResource();
		try {
			Set<String> elements = jedis.smembers(latitudePointer + ":"+ longitudePointer + ":types");
			if (!jedis.sismember(latitudePointer + ":" + longitudePointer+ ":types", type))
				jedis.sadd(latitudePointer + ":" + longitudePointer + ":types",	type);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return type;
	}

	/*
	 * private SerachObjectinGrid (double latitude, double longitude, int type,
	 * double radius(metros)) { determinar os graus de longitude e latitude a
	 * que corresponde uma distancia de radius metros vizinhaça do ponto de
	 * lat=0, long=0 determinar o canto superior esquerdo e o inferior direito
	 * em termos de latitude e longitude agora consegues determinar quais os
	 * indexes que estao dentro deste rectangulo inclisuve
	 * 
	 * 
	 * 
	 * pointerparalatitudedictionary = GetDiciotnaryLatitudeIndex (latitude);
	 * pointerparalongitudedictionary = GetDiciotnaryLatitudeIndex
	 * (pointerparadictionary, longitude); pointerparaltypeictionary =
	 * GetDiciotnaryLatitudeIndex (pointerparadictionary, longitude, type);
	 * insert (objectsdsd, pointerparaltypeictionary);
	 */

	public ArrayList<String> searchObjectsInGrid(double latitude, double longitude, String type, double radius,String appId) {
		/*
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",	RedisGeoPORT);
		Jedis jedis = pool.getResource();
		Set<String> elementsInGrid = new HashSet<String>();
		latitude += 90;
		longitude += 180;
		int latInGrid = determineLatitudeInGrid(latitude);
		int longInGrid = determineLongitudeInGrid(longitude);
		try {
			// calculate the minimum lat using the radius?
			Set<String> lats = jedis.zrangeByScore("latitudes",	latInGrid - latp, latInGrid + latp);
			//Set<String> lats = jedis.zrange("latitudes", latLong1,latLong2);
			Iterator<String> itLat = lats.iterator();
			while (itLat.hasNext()) {
				String latitudePointer = itLat.next();
				Set<String> longs = jedis.zrangeByScore(latitudePointer+ ":longitudes", longInGrid - longp, longInGrid + longp);
				//Set<String> longs = jedis.zrange(latitudePointer+ ":longitudes", longLong1, longLong2);
				Iterator<String> itLong = longs.iterator();
				while (itLong.hasNext()) {
					String longitudePointer = itLong.next();
					if (jedis.sismember(latitudePointer + ":"+ longitudePointer + ":types", type)) {
						//all the elements of the type are added to the structure
						elementsInGrid = jedis.smembers(latitudePointer + ":"+ longitudePointer + ":" + type);
						//check if the radius intersects with other grid squares
						//upper left
						double latUpperLeftCornerSquare = latInGrid + radius;
						double longUpperLeftCornerSquare = longInGrid - radius;
						if(jedis.exists(latUpperLeftCornerSquare + ":" + longUpperLeftCornerSquare + ":"+ type))
							elementsInGrid.addAll(jedis.smembers(latUpperLeftCornerSquare + ":"+ longUpperLeftCornerSquare + ":types"));
						//upper right
						double latUpperRightCornerSquare = latInGrid + radius;
						double longUpperRightCornerSquare = longInGrid + radius;
						if(jedis.exists(latUpperRightCornerSquare + ":" + longUpperRightCornerSquare + ":"+ type))
							elementsInGrid.addAll(jedis.smembers(latUpperRightCornerSquare + ":"+ longUpperRightCornerSquare + ":types"));
						//lower left
						double latLowerLeftCornerSquare = latInGrid - radius;
						double longLowerLeftCornerSquare = longInGrid - radius;
						if(jedis.exists(latLowerLeftCornerSquare + ":" + longLowerLeftCornerSquare + ":"+ type))
							elementsInGrid.addAll(jedis.smembers(latLowerLeftCornerSquare + ":"	+ longLowerLeftCornerSquare + ":types"));
						//lower right
						double latLowerRightCornerSquare = latInGrid - radius;
						double longLowerRightCornerSquare = longInGrid + radius;
						if(jedis.exists(latLowerRightCornerSquare + ":" + longLowerRightCornerSquare + ":"+ type))
							elementsInGrid.addAll(jedis.smembers(latLowerRightCornerSquare + ":"+ longLowerRightCornerSquare + ":types"));
					}
				}
			}

		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return elementsInGrid;*/
		return searchObjectsInGridJM(latitude, longitude, type, radius,appId); 
		
	}

	private ArrayList<String> searchObjectsInGridJM(double latitude,double longitude, String type, double meters, String appId) {
		ArrayList<String> elementsInGrid = new ArrayList<String>();
		HashMap<String,Double> elementsResult = new HashMap<String, Double>();
		ArrayList<String> elementsOrder = new ArrayList<String>();
		if(type.equals("jpg")) type = "image";
		if(type.equals("wmv")) type = "video";
		if(type.equals("mp3")) type = "audio";
		try{
			JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.SERVER, Const.REDIS_GEO_PORT);
			Jedis jedis = pool.getResource();
			JedisPool poolServer = new JedisPool(new JedisPoolConfig(), server);
			Jedis jedisServer = poolServer.getResource();
			//latitude += 90;
			//longitude += 180;
			double latOrig=latitude;
			double longOrig=longitude;
			
			double maxLat = latitude+transformMetersInDegreesLat(meters); 
			double minLat = latitude-transformMetersInDegreesLat(meters); 
			double maxLong = longitude+transformMetersInDegreesLong(meters,maxLat); 
			double minLong = longitude-transformMetersInDegreesLong(meters,minLat); 
			maxLat+= 90;
			minLat+= 90;
			maxLong+= 180;
			minLong+= 180;
			latitude += 90;
			longitude += 180;
			Set<String> elementsInLat = new HashSet<String>();
			Set<String> elementsInLong = new HashSet<String>();
			if(type.equals("image")){
				elementsInLat  = jedis.zrangeByScore("latitudesImages",	minLat, maxLat);
				elementsInLong = jedis.zrangeByScore("longitudesImages", minLong, maxLong);
			}
			if(type.equals("audio")){
				elementsInLat  = jedis.zrangeByScore("latitudesAudios",	minLat, maxLat);
				elementsInLong = jedis.zrangeByScore("longitudesAudios", minLong, maxLong);
			}
			if(type.equals("video")){
				elementsInLat  = jedis.zrangeByScore("latitudesVideos",	minLat, maxLat);
				elementsInLong = jedis.zrangeByScore("longitudesVideos", minLong, maxLong);
			}
			Iterator<String> iterator = elementsInLat.iterator();
		    while(iterator.hasNext()) {
		        String objId = iterator.next();
		        if(elementsInLong.contains(objId)){
		        	elementsInGrid.add(objId);
		        }
		    }
		    Iterator<String> iterator2 = elementsInGrid.iterator();
		    while(iterator2.hasNext()) {
		    	String objId = iterator2.next();
		    	String location = ":";
		    	if(type.equals("image"))
		    		location = jedisServer.hget("images:"+objId,"location");
		    		if(location==null)
		    		{
		    			MongoDBDataModel mongoModel = new MongoDBDataModel(MongoServer, MongoPort);
		    			location = mongoModel.getImageLocationUsingImageId(appId, objId);
		    		}
	        	if(type.equals("audio")){
	        		location = jedisServer.hget("audios:"+objId,"location");
	        		if(location==null)
		    		{
		    			MongoDBDataModel mongoModel = new MongoDBDataModel(MongoServer, MongoPort);
		    			location = mongoModel.getAudioLocationUsingAudioId(appId, objId);
		    		}
	        	}
	        	if(type.equals("video")){
	        		location = jedisServer.hget("videos:"+objId,"location");
	        		if(location==null)
		    		{
		    			MongoDBDataModel mongoModel = new MongoDBDataModel(MongoServer, MongoPort);
		    			location = mongoModel.getVideoLocationUsingVideoId(appId, objId);
		    		}
	        	}
				String[] locationArray = location.split(":");
				Double myLat = Double.parseDouble(locationArray[0]);
				double mylatOri = myLat;
				myLat+=90;
				Double myLong = Double.parseDouble(locationArray[1]);
				double mylongOri = myLong;
				myLong+=180;
				
				double dist2Or = getDistanceFromLatLonInKm(latOrig,longOrig,mylatOri,mylongOri);
				/*double distOr = distance(latOrig,longOrig,mylatOri,mylongOri);
				
				double dist2 = getDistanceFromLatLonInKm(latitude,longitude,myLat,myLong);
				double dist = distance(latitude,longitude,myLat,myLong);
				*/
				if(dist2Or<=(meters/1000)){
					elementsResult.put(objId,dist2Or);
				}
		    }
		    elementsOrder = orderHash(elementsResult);
		}catch(Exception e){
			System.err.println(e.toString());
		}
		return elementsOrder;
	}
	
	double getDistanceFromLatLonInKm(double lat1,double lon1,double lat2,double lon2) {
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

	double deg2rad2(double deg) {
		  return deg * (Math.PI/180);
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

	private double transformMetersInDegreesLat(double meters) {
		return ((meters/1000)/110.54);
		/*
		Double km = meters/1000;
		Double latTraveledKM = (km * 0.621371);
		Double latTraveledDeg = (1 / 110.54) * latTraveledKM;
		return Math.abs(latTraveledDeg);
		*/
	}
	private double transformMetersInDegreesLong(double meters, double currentLat) {
		//return ((meters/1000)/87.86);
		return Math.abs((meters/1000)/(111.320*Math.cos(currentLat)));
		/*
		Double km = meters/1000;
		Double longTraveledKM = km * 0.621371;
		Double longTraveledDeg = (1 / (111.320 * Math.cos(currentLat))) * longTraveledKM;
		return Math.abs(longTraveledDeg);
		*/
	}

	// http://www.geodatasource.com/developers/java
	//return kilometers
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
	public double distance2(double lat1, double lon1, double lat2, double lon2) {
		double d2r = (180 / Math.PI);
		double d=0;
		try{
		    double dlong = (lon2 - lon1) * d2r;
		    double dlat = (lat2 - lat1) * d2r;
		    double a =
		        Math.pow(Math.sin(dlat / 2.0), 2)
		            + Math.cos(lat1 * d2r)
		            * Math.cos(lat2 * d2r)
		            * Math.pow(Math.sin(dlong / 2.0), 2);
		    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		    d = 6367 * c;
		} catch(Exception e){
		    e.printStackTrace();
		}
		return d;
	}
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts decimal degrees to radians : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts radians to decimal degrees : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}

	
}
