/*****************************************************************************************
Infosistema - OpenBaas
Copyright(C) 2002-2014 Infosistema, S.A.
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
www.infosistema.com
info@openbaas.com
Av. José Gomes Ferreira, 11 3rd floor, s.34
Miraflores
1495-139 Algés Portugal
****************************************************************************************/
package infosistema.openbaas.utils;

import java.util.ArrayList;
import java.util.List;

import javapns.Push;
import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.devices.Device;
import javapns.notification.PushedNotification;

public class ApplePushNotifications {

	
	
	public static void pushCombineNotification(String alertText, int badge, String keystore, String password, Boolean production, Object devices) throws CommunicationException, KeystoreException {
		List<PushedNotification> notifications = Push.combined(alertText, badge, "default", keystore, password, production, devices);
		printPushedNotifications(notifications);
	}
	
	public static List<Device> pushFeedbackService(String keystore, String password, Boolean production) throws CommunicationException, KeystoreException {
		
		List<Device> res = new ArrayList<Device>();
		res = Push.feedback(keystore, password, production);
		return res;
	}
	
	public static void pushBadgeService(int badge,String keystore, String password, Boolean production, Object devices) throws CommunicationException, KeystoreException {
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
			Log.warning("","", "printPushedNotifications ->", "No notifications could be sent, probably because of a critical error");
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
				Log.debug("","", "printPushedNotifications desc->", description +" Notification:"+ notification.toString());
			} catch (Exception e) {
				Log.error("","", "printPushedNotifications desc->","Error in Notification:"+ notification.toString()+ e.toString(), e);
			}
		}
	}
}
