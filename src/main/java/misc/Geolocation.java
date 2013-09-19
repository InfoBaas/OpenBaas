package misc;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class Geolocation implements GeoLocationOperations{
	private static final int RedisGeoPORT = 6381;
	private Jedis jedis;
	private final static String server = "localhost";
	private static final int squareSize = 100; // 100meters
	private static int numbits = 6 * 5;
	double[] latitudeRange = { -90, 90 };
	double[] longitudeRange = { -180, 180 };
	private static final int numberGenerator = 7;
	final static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'm', 'n', 'p',
			'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
	final static HashMap<Character, Integer> lookup = new HashMap<Character, Integer>();
	static {
		int i = 0;
		for (char c : digits)
			lookup.put(c, i++);
	}

	public Geolocation() {
		jedis = new Jedis(server, RedisGeoPORT);
	}

	public double[] decode(String geohash) {
		StringBuilder buffer = new StringBuilder();
		for (char c : geohash.toCharArray()) {
			int i = lookup.get(c) + 32;
			buffer.append(Integer.toString(i, 2).substring(1));
		}
		BitSet lonset = new BitSet();
		BitSet latset = new BitSet();
		// even bits
		int j = 0;
		for (int i = 0; i < numbits * 2; i += 2) {
			boolean isSet = false;
			if (i < buffer.length())
				isSet = buffer.charAt(i) == '1';
			lonset.set(j++, isSet);
		}
		// odd bits
		j = 0;
		for (int i = 1; i < numbits * 2; i += 2) {
			boolean isSet = false;
			if (i < buffer.length())
				isSet = buffer.charAt(i) == '1';
			latset.set(j++, isSet);
		}
		double lon = decode(lonset, -180, 180);
		double lat = decode(latset, -90, 90);
		return new double[] { lat, lon };
	}

	private double decode(BitSet bs, double floor, double ceiling) {
		double mid = 0;
		for (int i = 0; i < bs.length(); i++) {
			mid = (floor + ceiling) / 2;
			if (bs.get(i))
				floor = mid;
			else
				ceiling = mid;
		}
		return mid;
	}

	public String encode(double lat, double lon) {
		BitSet latbits = getBits(lat, -90, 90);
		BitSet lonbits = getBits(lon, -180, 180);
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < numbits; i++) {
			buffer.append((lonbits.get(i)) ? '1' : '0');
			buffer.append((latbits.get(i)) ? '1' : '0');
		}
		return base32(Long.parseLong(buffer.toString(), 2));
	}

	private BitSet getBits(double lat, double floor, double ceiling) {
		BitSet buffer = new BitSet(numbits);
		for (int i = 0; i < numbits; i++) {
			double mid = (floor + ceiling) / 2;
			if (lat >= mid) {
				buffer.set(i);
				floor = mid;
			} else {
				ceiling = mid;
			}
		}
		return buffer;
	}

	public static String base32(long i) {
		char[] buf = new char[65];
		int charPos = 64;
		boolean negative = (i < 0);
		if (!negative)
			i = -i;
		while (i <= -32) {
			buf[charPos--] = digits[(int) (-(i % 32))];
			i /= 32;
		}
		buf[charPos] = digits[(int) (-i)];

		if (negative)
			buf[--charPos] = '-';
		return new String(buf, charPos, (65 - charPos));
	}

	// http://www.geodatasource.com/developers/java
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

	double numberLongPositions;
	double numberLatPositions;
	double latp;
	double longp;

	public void createGridCache(double latPrecision, double longPrecision) {
		numberLatPositions = 180 / latPrecision;
		numberLongPositions = 360 / longPrecision;
		latp = latPrecision;
		longp = longPrecision;
	}

	public String determinePointInGrid(double latitude, double longitude) {
		int latIndex = (int) (latitude / latp);
		int longIndex = (int) (longitude / longp);
		return latIndex + ":" + longIndex;
	}

	private int determineLatitudeInGrid(double latitude) {
		return (int) (latitude / latp);
	}

	private int determineLongitudeInGrid(double longitude) {
		return (int) (longitude / longp);
	}

	public boolean insertObjectInGrid(double latitude, double longitude,
			String type, String objectId) {
		latitude += 90; // negative values
		longitude += 180;
		String latitudePointer = getLatitudeIndex(latitude);
		String longitudePointer = getLongitudeIndex(latitudePointer, longitude);
		String typePointer = getTypeIndex(latitudePointer, longitudePointer,
				longitude, type);
		createObject(latitudePointer, longitudePointer, typePointer, objectId);
		return true;

	}

	private void createObject(String latitudePointer, String longitudePointer,
			String typePointer, String objectId) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisGeoPORT);
		Jedis jedis = pool.getResource();
		try {
			if (!jedis.exists(latitudePointer + ":" + longitudePointer + ":"
					+ typePointer))
				jedis.sadd(latitudePointer + ":" + longitudePointer + ":"
						+ typePointer, objectId);
		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();

	}

	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}

	private String getLatitudeIndex(double latitude) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisGeoPORT);
		Jedis jedis = pool.getResource();
		String id = null;
		try {
			// element does not exist, create it
			Set<String> elements = jedis.zrangeByScore("latitudes", latitude
					- latp, latitude + latp);
			if (elements.size() == 0) {
				id = getRandomString(numberGenerator);
				jedis.zadd("latitudes", determineLatitudeInGrid(latitude), id);
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
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisGeoPORT);
		Jedis jedis = pool.getResource();
		String id = null;
		try {
			// minimum: longitude - longitudePrecision
			// maximum: longitude + longitudePrecision
			// sorted set with key latPointer:longitudes
			Set<String> elements = jedis.zrangeByScore(latitudePointer
					+ ":longitudes", longitude - longp, longitude + longp);
			if (elements.size() == 0) {
				id = getRandomString(numberGenerator);
				jedis.zadd(latitudePointer + ":longitudes",
						determineLongitudeInGrid(longitude), id);
			} else {
				// check which of the elements is closest to the given latitude
				Iterator<String> it = elements.iterator();
				double closestSubtraction = 0;
				while (it.hasNext()) {
					String element = it.next();
					double score = jedis.zscore(
							latitudePointer + ":longitudes", element);
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

	private String getTypeIndex(String latitudePointer,
			String longitudePointer, double longitude, String type) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisGeoPORT);
		Jedis jedis = pool.getResource();
		try {
			Set<String> elements = jedis.smembers(latitudePointer + ":"
					+ longitudePointer + ":types");
			if (!jedis.sismember(latitudePointer + ":" + longitudePointer
					+ ":types", type))
				jedis.sadd(latitudePointer + ":" + longitudePointer + ":types",
						type);
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
	public Set<String> searchObjectsInGrid(double latitude, double longitude,
			String type, double radius) {
		JedisPool pool = new JedisPool(new JedisPoolConfig(), "localhost",
				RedisGeoPORT);
		Jedis jedis = pool.getResource();
		Set<String> elementsInGrid = new HashSet<String>();
		latitude += 90;
		longitude += 180;
		int latInGrid = determineLatitudeInGrid(latitude);
		int longInGrid = determineLongitudeInGrid(longitude);
		try {
			// calculate the minimum lat using the radius?

			Set<String> lats = jedis.zrangeByScore("latitudes",
					latInGrid - latp, latInGrid + latp);
			Iterator<String> itLat = lats.iterator();
			while (itLat.hasNext()) {
				String latitudePointer = itLat.next();
				Set<String> longs = jedis.zrangeByScore(latitudePointer
						+ ":longitudes", longInGrid - longp, longInGrid + longp);
				Iterator<String> itLong = longs.iterator();
				while (itLong.hasNext()) {
					String longitudePointer = itLong.next();
					if (jedis.sismember(latitudePointer + ":"
							+ longitudePointer + ":types", type)) {
						//all the elements of the type are added to the structure
						elementsInGrid = jedis.smembers(latitudePointer + ":"
								+ longitudePointer + ":" + type);
						//check if the radius intersects with other grid squares
						//upper left
						double latUpperLeftCornerSquare = latInGrid + radius;
						double longUpperLeftCornerSquare = longInGrid - radius;
						if(jedis.exists(latUpperLeftCornerSquare + ":" + longUpperLeftCornerSquare + ":"
								+ type))
							elementsInGrid.addAll(jedis.smembers(latUpperLeftCornerSquare + ":"
									+ longUpperLeftCornerSquare + ":types"));
						//upper right
						double latUpperRightCornerSquare = latInGrid + radius;
						double longUpperRightCornerSquare = longInGrid + radius;
						if(jedis.exists(latUpperRightCornerSquare + ":" + longUpperRightCornerSquare + ":"
								+ type))
							elementsInGrid.addAll(jedis.smembers(latUpperRightCornerSquare + ":"
									+ longUpperRightCornerSquare + ":types"));
						//lower left
						double latLowerLeftCornerSquare = latInGrid - radius;
						double longLowerLeftCornerSquare = longInGrid - radius;
						if(jedis.exists(latLowerLeftCornerSquare + ":" + longLowerLeftCornerSquare + ":"
								+ type))
							elementsInGrid.addAll(jedis.smembers(latLowerLeftCornerSquare + ":"
									+ longLowerLeftCornerSquare + ":types"));
						//lower right
						double latLowerRightCornerSquare = latInGrid - radius;
						double longLowerRightCornerSquare = longInGrid + radius;
						if(jedis.exists(latLowerRightCornerSquare + ":" + longLowerRightCornerSquare + ":"
								+ type))
							elementsInGrid.addAll(jedis.smembers(latLowerRightCornerSquare + ":"
									+ longLowerRightCornerSquare + ":types"));
					}
				}
			}

		} finally {
			pool.returnResource(jedis);
		}
		pool.destroy();
		return elementsInGrid;
	}

	/*
	 * private CreateGridCache (double LatPrecision, double LongPrecision)
	 * {static double numberlongpositions = 360 / longprecision; static double
	 * numberlatpositions = 180 / latprecision;
	 * 
	 * static latp = LatPrecision static longp = LatPrecision }
	 * 
	 * 
	 * private determinePointinGrid (latitude, longitude) { static latp =
	 * LatPrecisiont int latindex = int (latitude / LatPrecision); int longindex
	 * = int (longitude / LongPrecision); }
	 * 
	 * private InsertObjectinGrid (double latitude, double longitude, int type,
	 * obj objectdfd) { pointerparalatitudedictionary =
	 * GetDiciotnaryLatitudeIndex (latitude); pointerparalongitudedictionary =
	 * GetDiciotnaryLatitudeIndex (pointerparadictionary, longitude);
	 * pointerparaltypeictionary = GetDiciotnaryLatitudeIndex
	 * (pointerparadictionary, longitude, type); insert (objectsdsd,
	 * pointerparaltypeictionary);
	 * 
	 * 
	 * }
	 * 
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
	 * 
	 * 
	 * }
	 */

}
