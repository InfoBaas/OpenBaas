package infosistema.openbaas.management;

import java.util.Iterator;
import java.util.List;

import infosistema.openbaas.middleLayer.NotificationMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

public class NotificationsThread extends Thread {
			 
	 private static NotificationsThread instance = null;
	 private static String SEPARATOR3 = ":_:";
	 private int sleepTime = -1;
	 private boolean keepRunning = false;
	 private static NotificationMiddleLayer noteMid; 

    private NotificationsThread() {
    	
    }

    public static NotificationsThread get() {
      noteMid = NotificationMiddleLayer.getInstance();
      if (instance == null) {
        instance = new NotificationsThread();
      }
      return instance;
    }

    public static void startManager() {
    	NotificationsThread mng = get();
        mng.keepRunning = true;
        mng.start();
    }

    public static void stopManager() {
    	NotificationsThread mng = get();
        mng.keepRunning = false;
        mng.interrupt();
    }

    public void run() {
      sleepTime = Const.getAPNS_PUSH_CICLE();
  	  if (sleepTime == -1) {
  	  	// default sleep time 24 hours
  	  	sleepTime = 60000;
  	  }
  	
  	  while (keepRunning) {
  		  try {
  			  try {
  				  get().processCommand();
  	          } catch (Exception e) {
  	        	  Log.warning("PushNotifications", this, "run", "Failed to using APNS PushNotifications service: ", e);
  	          }
  		      sleep(sleepTime);
  		  } catch (InterruptedException e) {
  			  if (keepRunning) {
  				  Log.error("PushNotifications", this, "run", "Thread interrupted: ", e); 
  			  } else {
  				  Log.info("PushNotifications", this, "run", "Stopping event manager..."); 
  			  }
  	      } catch (Exception e) {
  	    	  Log.error("PushNotifications", this, "run", "Failed to check events: ", e); 
  	      }
  	  }
    }
    
    private void processCommand() {
        try {
        	List<String> notificationList = noteMid.getPushNotificationsTODO();
        	Iterator<String> it = notificationList.iterator();
        	while(it.hasNext()){
        		String notificationCurr = it.next(); 
	        	String[] arrayNotifications =  notificationCurr.split(SEPARATOR3);
	        	Log.error("",  "thread", "", "###1 -"+arrayNotifications[0]+" - "+arrayNotifications[1]+" - "+arrayNotifications[2]);
	        	noteMid.pushNotificationCombine(arrayNotifications[0], arrayNotifications[1], arrayNotifications[2]);
        	}
        	List<String> badgesList = noteMid.getPushBadgesTODO();
        	it = badgesList.iterator();
        	while(it.hasNext()){
        		String notificationCurr = it.next(); 
	        	String[] arrayNotifications =  notificationCurr.split(SEPARATOR3);
	        	noteMid.pushAllBadges(arrayNotifications[0], arrayNotifications[1]);
        	}
        } catch (Exception e) {
        	Log.error("NotificationsThread", this, "Error in NotificationsThread", "Error in NotificationsThread",e);
        }
    }		
}
