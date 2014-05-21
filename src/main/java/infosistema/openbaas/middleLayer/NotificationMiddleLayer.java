package infosistema.openbaas.middleLayer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.devices.Device;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.data.models.DeviceOB;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.ChatModel;
import infosistema.openbaas.dataaccess.models.NotificationsModel;
import infosistema.openbaas.utils.ApplePushNotifications;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;


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

	public void pushBadge(String appId, String userId, String roomId) {
		try {
			Date startDate = Utils.getDate();
			int numberBadge = 0;
			Application app = appModel.getApplication(appId);
			List<String> unReadMsgs =  chatModel.getTotalUnreadMsg(appId, userId);
			Boolean flagNotification = null;
			Date endDate = Utils.getDate();
			Log.info("", this, "$$$$$$", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time$1:" + (endDate.getTime()-startDate.getTime()));
			startDate = endDate;
			if(roomId != null){
				numberBadge = unReadMsgs.size();
				flagNotification = chatModel.hasNotification(appId, roomId);
				endDate = Utils.getDate();
				Log.info("", this, "$$$$$$", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time$2:" + (endDate.getTime()-startDate.getTime()));
				startDate = endDate;
				if (flagNotification){
					sendBadge(appId, userId, app, numberBadge,roomId);
				}
				endDate = Utils.getDate();
				Log.info("", this, "$$$$$$", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time$3:" + (endDate.getTime()-startDate.getTime()));
				startDate = endDate;
			}else{
				Iterator<String> it = unReadMsgs.iterator();
				while(it.hasNext()){
					String messageId = it.next();
					ChatMessage msg = chatModel.getMessage(appId, messageId);
					endDate = Utils.getDate();
					Log.info("", this, "$$$$$$", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time$4:" + (endDate.getTime()-startDate.getTime()));
					startDate = endDate;
					flagNotification = chatModel.hasNotification(appId, msg.getRoomId());
					endDate = Utils.getDate();
					Log.info("", this, "$$$$$$", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time$5:" + (endDate.getTime()-startDate.getTime()));
					startDate = endDate;
					if(flagNotification){
						numberBadge++;
						sendBadge(appId, userId, app, numberBadge,msg.getRoomId());
						endDate = Utils.getDate();
						Log.info("", this, "$$$$$$", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time$6:" + (endDate.getTime()-startDate.getTime()));
						startDate = endDate;
					}
				}
			}
			endDate = Utils.getDate();
			Log.info("", this, "$$$$$$", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time$3:" + (endDate.getTime()-startDate.getTime()));
			startDate = endDate;
		} catch (Exception e) {
			Log.error("", this, "pushBadge", "Error pushing the badge.", e);
		}
	}

	private void sendBadge(String appId, String userId, Application app, int numberBadge, String roomId) {
		List<Certificate> certList = new ArrayList<Certificate>();
		try {
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
					ApplePushNotifications.pushBadgeService(numberBadge, certi.getCertificatePath(), certi.getAPNSPassword(), Const.getAPNS_PROD(), devices);
				}
			}
		} catch (Exception e) {
			Log.error("", this, "sendBadge", "Error sending the badge.", e);
		} 
	}

	public void pushAllBadges(String appId, String userId) {
		try {
			List<String> chats = chatModel.getAllUserChats(appId, userId);
			for (String roomId: chats){ 
				pushBadge(appId, userId, roomId);
			}
		} catch (Exception e) {
			Log.error("", this, "pushBadge", "Error pushing the badge.", e);
		}
	}

	public void pushNotificationCombine(String appId, String sender, String roomId) {
		//Log.debug("",  "pushNotificationCombine", "", "###0 -appId:"+appId+" - sender: "+sender+" - roomId:"+roomId);
		List<String> participants = new ArrayList<String>();
		participants = chatModel.getListParticipants(appId, roomId);
		try{
			if(participants.size()>0 && participants!=null){
				Boolean flagNotification = chatModel.hasNotification(appId, roomId);
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
				//Log.debug("",  "pushNotificationCombine", "", "###1 -participants size:"+participants.size());
				while(it.hasNext()){
					String curr = it.next();
					if(!curr.equals(sender)){
						//Log.debug("",  "pushNotificationCombine", "", "###2 ");
						unReadUsers.add(curr);
						if(flagNotification){
							//Log.debug("",  "pushNotificationCombine", "", "###3 ");
							if(app!=null){
								if(clientsList!= null && clientsList.size()>0){
									//Log.debug("",  "pushNotificationCombine", "", "###4 ");
									if(certList.size()>0){
										Iterator<Certificate> it3 = certList.iterator();
										while(it3.hasNext()){
											Certificate certi = it3.next();
											//Log.debug("",  "pushNotificationCombine", "", "###5 ");
											List<Device> devices = noteModel.getDeviceIdList(appId, curr, certi.getClientId());
											if(devices!=null && devices.size()>0){
												int badge = chatModel.getTotalUnreadMsg(appId, curr).size();
												//Log.debug("",  "pushNotificationCombine", "", "###6 -badge:"+badge+" - devices size"+devices.size());
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
			Log.error("", this, "getPushBadgesTODO", "Error in getPushBadgesTODO."+res.size(), e);
		}
		return res;
	}	
	
	public Boolean setPushBadgesTODO(String appId, String userId) {
		Boolean res = false;
		try {
			res = noteModel.setNewBadgesTODO(appId, userId);
		} catch(Exception e){
			Log.error("", this, "setPushBadgesTODO", "Error in setPushBadgesTODO."+res, e);
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
	
	public Boolean setPushNotificationsTODO(String appId, String userId, String roomId) {
		Boolean res = false;
		try {
			res = noteModel.setNewNotifications(appId, userId, roomId);
		} catch(Exception e){
			Log.error("", this, "getPushNotificationsTODO", "Error in getPushNotificationsTODO."+res, e);
		}
		return res;
	}	
}
