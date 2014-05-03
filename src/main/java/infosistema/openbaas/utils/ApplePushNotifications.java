package infosistema.openbaas.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javapns.Push;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.devices.Device;
import javapns.notification.PushedNotification;

public class ApplePushNotifications {

	
	
	public static void pushCombineNotification(String alertText, int badge, String keystore, String password, Boolean production, Object devices) throws CommunicationException, KeystoreException {
		Log.error("", "", "pushCombineNotification", "********pushCombineNotification ###### alert:"+alertText);
		List<Device> devs = (List<Device>)devices;
		Iterator<Device> it = devs.iterator();
		while(it.hasNext()){
			Log.error("", "", "push", "push0:" +"keystore:" +keystore+" - password:"+password+" - production:"+production+" - devices token:"+ it.next().getToken());
		}
		List<PushedNotification> notifications = Push.combined(alertText, badge, "default", keystore, password, production, devices);
		printPushedNotifications(notifications);
	}
	
	public static List<Device> pushFeedbackService(String keystore, String password, Boolean production) throws CommunicationException, KeystoreException {
		
		List<Device> res = new ArrayList<Device>();
		res = Push.feedback(keystore, password, production);
		return res;
	}
	
	public static void pushBadgeService(int badge,String keystore, String password, Boolean production, Object devices) throws CommunicationException, KeystoreException {
		List<Device> devs = (List<Device>)devices;
		Iterator<Device> it = devs.iterator();
		while(it.hasNext()){
			Log.error("", "", "pushBadge", "pushBadge:" +"keystore:" +keystore+" - password:"+password+" - production:"+production+" - devices token:"+ it.next().getToken());
		}
		List<PushedNotification> notifications = Push.badge(badge, keystore, password, production, devices);
		printPushedNotifications(notifications);
		
	}
	
	/**
	 * Print to the console a comprehensive report of all pushed notifications and results.
	 * @param notifications a raw list of pushed notifications
	 */
	public static void printPushedNotifications(List<PushedNotification> notifications) {
		List<PushedNotification> failedNotifications = PushedNotification.findFailedNotifications(notifications);
		List<PushedNotification> successfulNotifications = PushedNotification.findSuccessfulNotifications(notifications);
		int failed = failedNotifications.size();
		int successful = successfulNotifications.size();

		if (successful > 0 && failed == 0) {
			printPushedNotifications("All notifications pushed successfully (" + successfulNotifications.size() + "):", successfulNotifications);
		} else if (successful == 0 && failed > 0) {
			printPushedNotifications("All notifications failed (" + failedNotifications.size() + "):", failedNotifications);
		} else if (successful == 0 && failed == 0) {
			Log.error("","", "printPushedNotifications ->", "No notifications could be sent, probably because of a critical error");
		} else {
			printPushedNotifications("Some notifications failed (" + failedNotifications.size() + "):", failedNotifications);
			printPushedNotifications("Others succeeded (" + successfulNotifications.size() + "):", successfulNotifications);
		}
	}
	
	/**
	 * Print to the console a list of pushed notifications.
	 * @param description a title for this list of notifications
	 * @param notifications a list of pushed notifications to print
	 */
	public static void printPushedNotifications(String description, List<PushedNotification> notifications) {
		for (PushedNotification notification : notifications) {
			try {
				Log.error("","", "printPushedNotifications desc->", description +" Notification:"+ notification.toString());
			} catch (Exception e) {
				Log.error("","", "printPushedNotifications desc->","Error in Notification:"+ notification.toString()+ e.toString());
			}
		}
	}
}
