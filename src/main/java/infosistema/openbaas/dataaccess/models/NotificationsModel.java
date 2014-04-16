package infosistema.openbaas.dataaccess.models;

import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javapns.devices.Device;
import javapns.devices.implementations.basic.BasicDevice;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class NotificationsModel {

	// *** CONTRUCTORS *** //

	public NotificationsModel() {
		pool = new JedisPool(new JedisPoolConfig(), Const.getRedisChatServer(),Const.getRedisChatPort());
	}


	// *** PRIVATE *** //

	private JedisPool pool;
	

	// *** CONSTANTS *** //

	private static final int MAXELEMS = 9999999;

	private static final String PUSHLIST = "PushList";
	private static final String CERT = "Cert";
	private static final String DEVICEID = "deviceId";
	public static final String DEVICETOKEN = "deviceToken";
	public static final String CLIENTID = "clientId";
	private static final String LASTREGISTER= "lastRegister";
	private static final String USERID= "userId";
	private static final String PUSH_BADGES_LIST = "PushBadgesList";

	
	// *** KEYS *** //

	private static final String CLIENT_KEY_FORMAT = "%s:"+CERT+":%s";
	private static final String DEVICE_KEY_FORMAT = "%s:Device:%s:%s";
	private static final String DEVICE_LIST_KEY_FORMAT = "%s_DTList_%s_%s";
	private static final String BADGE_VALUE_FORMAT = "%s:_:%s";
	//private static final String NOTIFICATION_VALUE_FORMAT = "%s:_:%s:_:%s:_:%s:_:%s:_:%s:_:%s:_:%s:_:";
	private static final String NOTIFICATION_VALUE_FORMAT = "%s:_:%s:_:%s";
	
	private String getClientKey(String appId, String clientId) {
		return String.format(CLIENT_KEY_FORMAT, appId, clientId);
	}
	
	private String getDeviceKey(String appId, String clientId, String deviceToken) {
		return String.format(DEVICE_KEY_FORMAT, appId, clientId, deviceToken);
	}
	
	private String getDeviceListKey(String appId, String userId, String clientId) {
		return String.format(DEVICE_LIST_KEY_FORMAT, appId, userId, clientId);
	}

	private String getBadgeValue(String appId, String userId) {
		return String.format(BADGE_VALUE_FORMAT, appId, userId);
	}

	private String getNotificationValue(String appId, String userId, String roomId) {
		return String.format(NOTIFICATION_VALUE_FORMAT, appId, userId, roomId);
	}
	
	// *** CREATE *** //

	//device
	
	public Boolean addDeviceId(String appId,String userId, String clientId, String deviceToken) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			String user = getDeviceUser(appId, clientId, deviceToken);
			String deviceListKey = getDeviceListKey(appId, userId, clientId);
			if(user!=userId){
				jedis.lrem(getDeviceListKey(appId, user, clientId), 0, deviceToken);
			} else{
				jedis.lrem(deviceListKey, 0, deviceToken);
			}
			jedis.rpush(deviceListKey, deviceToken);
			res = true;
		} catch(Exception e){
			res = false;
			Log.error("", this, "addDeviceId", "Error in addDeviceId to list", e);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean createUpdateDevice(String appId, String userId, String clientId, Device device) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			Long milliseconds = device.getLastRegister().getTime();
			String deviceKey = getDeviceKey(appId, clientId, device.getToken());
			jedis.hset(deviceKey, DEVICEID, device.getDeviceId());
			jedis.hset(deviceKey, DEVICETOKEN, device.getToken());
			jedis.hset(deviceKey, LASTREGISTER, milliseconds.toString());
			jedis.hset(deviceKey, USERID, userId);
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	//certificate
	
	public Boolean createUpdateCertificate(String appId, Certificate cert) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			long milliseconds = cert.getCreatedDate().getTime();
			String clientKey = getClientKey(appId, cert.getClientId());
			jedis.hset(clientKey, Application.APNS_CLIENT_ID, cert.getClientId());
			jedis.hset(clientKey, Application.APNS_CERTIFICATION_PATH, cert.getCertificatePath());
			jedis.hset(clientKey, Application.APNS_PASSWORD, cert.getAPNSPassword());
			jedis.hset(clientKey, Application.CREATION_DATE, String.valueOf(milliseconds));
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	//badges
	
	public Boolean setNewBadgesTODO(String appId, String userId) {
		Jedis jedis = pool.getResource();
		Boolean res = false;
		try {
			String value = getBadgeValue(appId, userId);
			jedis.lrem(PUSH_BADGES_LIST, 0, value);
			jedis.rpush(PUSH_BADGES_LIST, value);
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	//notifications

	public Boolean setNewNotifications(String appId, String userId, String roomId) {
		Jedis jedis = pool.getResource();
		Boolean res = false;
		try {
			String value = getNotificationValue(appId, userId, roomId);
			jedis.lrem(PUSHLIST, 0, value);
			jedis.rpush(PUSHLIST, value);
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	
	// *** GET LIST *** //

	//device
	
	public List<Device> getDeviceIdList(String appId,String userId, String clientId) {
		List<Device> res = new ArrayList<Device>();
		List<String> aux = new ArrayList<String>();
		Jedis jedis = pool.getResource();
		try {
			aux = jedis.lrange(getDeviceListKey(appId, userId, clientId), 0, MAXELEMS);
			Iterator<String> it = aux.iterator();
			while(it.hasNext()){
				res.add(getDevice(appId, clientId, it.next()));
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	//certificate
	
	public List<Certificate> getAllCertificateList() {
		List<Certificate> res = new ArrayList<Certificate>();
		Jedis jedis = pool.getResource();
		try {
			Set<String> setCert = jedis.keys("*"+CERT+"*");
			Iterator<String> it = setCert.iterator();
			while(it.hasNext()){
				String str = it.next();
				String[] aux = str.split(Const.COLON);
				if(aux.length==3)
					res.add(getCertificate(aux[0], aux[2]));				
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	//badges

	public List<String> getAllBadgesTODO() {
		Jedis jedis = pool.getResource();
		List<String> res = new ArrayList<String>();
		try {
			res = jedis.lrange(PUSH_BADGES_LIST, 0, MAXELEMS);
			if (res.size()>0) jedis.del(PUSH_BADGES_LIST);
		} catch (Exception e) {
			Log.error("", this, "getAllBadgesTODO", "Error in getAllBadgesTODO redis."+ res.size(), e); 
		}	
		finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	//notifications

	public List<String> getAllNotificationsTODO() {
		Jedis jedis = pool.getResource();
		List<String> res = new ArrayList<String>();
		try {
			res = jedis.lrange(PUSHLIST, 0, MAXELEMS);
			if(res.size()>0)
				jedis.del(PUSHLIST);
		} catch (Exception e) {
			Log.error("", this, "getAllNotificationsTODO", "Error in getAllNotificationsTODO redis."+ res.size(), e); 
		}	
		finally {
			pool.returnResource(jedis);
		}
		return res;
	}


	// *** GET *** //

	//device
	
	public Device getDevice(String appId,String clientId, String deviceToken) {
		Device res = new BasicDevice();
		Jedis jedis = pool.getResource();
		try {
			String deviceKey = getDeviceKey(appId, clientId, deviceToken);
			String deviceId = jedis.hget(deviceKey, DEVICEID);
			long l = Long.valueOf(jedis.hget(deviceKey, LASTREGISTER));
			res.setLastRegister(new Timestamp(l));
			res.setToken(deviceToken);
			res.setDeviceId(deviceId);
			//res.setUserId(userId);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public String getDeviceUser(String appId,String clientId, String deviceToken) {
		String res = null;
		Jedis jedis = pool.getResource();
		try {
			String userId = jedis.hget(getDeviceKey(appId, clientId, deviceToken), USERID);
			res=userId;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	//certificate
	
	public Certificate getCertificate(String appId, String clientId) {
		Certificate res = null;
		Jedis jedis = pool.getResource();
		try {
			String clientKey = getClientKey(appId, clientId);
			String path = jedis.hget(clientKey, Application.APNS_CERTIFICATION_PATH);
			String pass = jedis.hget(clientKey, Application.APNS_PASSWORD);
			long l = Long.valueOf(jedis.hget(clientKey, Application.CREATION_DATE));
			res = new Certificate();
			res.setClientId(clientId);
			res.setCertificatePath(path);
			res.setAPNSPassword(pass);
			res.setCreatedDate(new Timestamp(l));
			res.setAppId(appId);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	
	// *** DELETE *** //

	//device
	
	public String removeDevice(String appId,String clientId, String deviceToken) {
		String res = null;
		Jedis jedis = pool.getResource();
		try {
			String deviceKey = getDeviceKey(appId, clientId, deviceToken);
			if(jedis.exists(deviceKey)){
				res = jedis.hget(deviceKey, USERID);
				jedis.del(deviceKey);
			}
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
	public Boolean removeDeviceId(String appId,String userId, String clientId, String deviceToken) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			Long a=(long) -1;
			a = jedis.lrem(getDeviceListKey(appId, userId, clientId), 0, deviceToken);
			if(a>0)				
				res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}
	
}
