package infosistema.openbaas.middleLayer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javapns.devices.Device;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.data.models.DeviceOB;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.ChatModel;
import infosistema.openbaas.dataaccess.models.NotificationsModel;
import infosistema.openbaas.utils.ApplePushNotifications;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;


public class NotificationMiddleLayer {

	private NotificationsModel noteModel;
	private ChatModel chatModel;
	private AppModel appModel;


	// *** INSTANCE *** //
	private static NotificationMiddleLayer instance = null;
	
	private NotificationMiddleLayer() {
		super();
		noteModel = new NotificationsModel();
		chatModel = new ChatModel();
		appModel = new AppModel();
	}
	
	public static NotificationMiddleLayer getInstance() {
		if (instance == null) instance = new NotificationMiddleLayer();
		return instance;
	}

	public Map<String, Device> addDeviceToken(String appId, String userId, String clientId, String deviceToken) {
		Map<String, Device> res = new HashMap<String, Device>();
		Device device = new DeviceOB();
		Timestamp time = new Timestamp(new Date().getTime());
		device.setDeviceId(deviceToken);
		device.setLastRegister(time);
		device.setToken(deviceToken);
		Boolean addId = noteModel.addDeviceId(appId, userId, clientId, deviceToken); 
		Boolean addDev = noteModel.createUpdateDevice(appId, userId, clientId, device);
		if(addId && addDev)
			res.put(clientId, device);
		else 
			res = null;
		return res;
	}
	
	public Boolean remDeviceToken(String appId, String clientId, String deviceToken) {
		Boolean res = false;
		
		String userId = noteModel.removeDevice(appId, clientId, deviceToken);
		Boolean remId = noteModel.removeDeviceId(appId, userId, clientId, deviceToken);
		if(userId!=null && remId)
			res = true;
		return res;
	}

	public List<Certificate> getAllCertificates() {
		return noteModel.getAllCertificateList();
	}

	public void pushBadge(String appId, String userId, String chatRoomId) {
		try {
			Application app = appModel.getApplication(appId);
			Boolean flagNotification = chatModel.hasNotification(appId,chatRoomId);
			List<Certificate> certList = new ArrayList<Certificate>();
			
			if(flagNotification){
				List<String> clientsList = app.getClients();
				Iterator<String> it2 = clientsList.iterator();
				while(it2.hasNext()){
					String clientId = it2.next();
					certList.add(noteModel.getCertificate(appId,clientId));
				}
				Iterator<Certificate> it3 = certList.iterator();
				while(it3.hasNext()){
					Certificate certi = it3.next();
					List<Device> devices = noteModel.getDeviceIdList(appId, userId, certi.getClientId());
					if(devices!=null && devices.size()>0){
						int numberBadge = chatModel.getTotalUnreadMsg(appId, userId).size();
						ApplePushNotifications.pushBadgeService(numberBadge, certi.getCertificatePath(), certi.getAPNSPassword(), Const.getAPNS_PROD(), devices);
					}
				}
			}
		} catch (Exception e) {
			Log.error("", this, "pushBadge", "Error pushing the badge.", e);
		}
	}

	public void pushAllBadges(String appId, String userId) {
		try {
			List<String> chats = chatModel.getAllUserChats(appId, userId);
			for (String chatRoomId: chats){ 
				pushBadge(appId, userId, chatRoomId);
			}
		} catch (Exception e) {
			Log.error("", this, "pushBadge", "Error pushing the badge.", e);
		}
	}

	public void pushNotificationCombine(String appId, String sender,String chatRoomId, String fileText, 
			String videoText, String imageText, String audioText, String messageText) {
	
		List<String> participants = new ArrayList<String>();
		participants = chatModel.getListParticipants(appId, chatRoomId);
		try{
			if(participants.size()>0 && participants!=null){
				Boolean flagNotification = chatModel.hasNotification(appId,chatRoomId);
				Application app = appModel.getApplication(appId);
				List<String> clientsList = app.getClients();
				List<Certificate> certList = new ArrayList<Certificate>();
				if(flagNotification){
					Iterator<String> it2 = clientsList.iterator();
					while(it2.hasNext()){
						String clientId = it2.next();
						certList.add(noteModel.getCertificate(appId,clientId));
					}
				}
				List<String> unReadUsers = new ArrayList<String>();
				Iterator<String> it = participants.iterator();
				while(it.hasNext()){
					String curr = it.next();
					if(!curr.equals(sender)){
						unReadUsers.add(curr);
						if(flagNotification){
							if(app!=null){
								if(clientsList!= null && clientsList.size()>0){
									if(certList.size()>0){
										Iterator<Certificate> it3 = certList.iterator();
										while(it3.hasNext()){
											Certificate certi = it3.next();
											List<Device> devices = noteModel.getDeviceIdList(appId, curr, certi.getClientId());
											if(devices!=null && devices.size()>0){
												int badge = chatModel.getTotalUnreadMsg(appId, curr).size();
												ApplePushNotifications.pushCombineNotification("Recebeu uma mensagem nova",badge,certi.getCertificatePath(), certi.getAPNSPassword(), Const.getAPNS_PROD(), devices);
											}
										}
									}								
								}
							}
						}
					}
				}
			}
		}catch (Exception e) {
			Log.error("", this, "pushNotificationCombine", "Error in pushNotificationCombine.", e);
		}
		
	}

	public List<String> getPushBadgesTODO() {
		List<String> res = new ArrayList<String>();
		try{
			res = noteModel.getAllBadgesTODO();
		}catch(Exception e){
			Log.error("", this, "getPushNotificationsTODO", "Error in getPushNotificationsTODO."+res.size(), e);
		}
		return res;
	}	
	
	public Boolean setPushBadgesTODO(String appId, String userId) {
		Boolean res = false;
		try {
			res = noteModel.setNewBadgesTODO(appId, userId);
		} catch(Exception e){
			Log.error("", this, "getPushNotificationsTODO", "Error in getPushNotificationsTODO."+res, e);
		}
		return res;
	}	
	
	public List<String> getPushNotificationsTODO() {
		List<String> res = new ArrayList<String>();
		try{
			res = noteModel.getAllNotificationsTODO();
		}catch(Exception e){
			Log.error("", this, "getPushNotificationsTODO", "Error in getPushNotificationsTODO."+res.size(), e);
		}
		return res;
	}	
	
	public Boolean setPushNotificationsTODO(String appId, String userId, String chatRoomId, String fileText,String videoText,
			String imageText, String audioText, String messageText) {
		Boolean res = false;
		try {
			res = noteModel.setNewNotifications(appId, userId, chatRoomId, fileText, videoText, imageText, audioText, messageText);
		} catch(Exception e){
			Log.error("", this, "getPushNotificationsTODO", "Error in getPushNotificationsTODO."+res, e);
		}
		return res;
	}	
}
