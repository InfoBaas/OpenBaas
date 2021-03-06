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
package infosistema.openbaas.management;

import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.middleLayer.NotificationMiddleLayer;
import infosistema.openbaas.utils.ApplePushNotifications;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javapns.devices.Device;

public class FeedBackSchedule extends Thread {

  private static FeedBackSchedule instance = null;
  private static String SEPARATOR1 = ":";
  private int sleepTime = -1;
  private boolean keepRunning = false;

  private FeedBackSchedule() {
  }

  public static FeedBackSchedule get() {
    if (instance == null) {
      instance = new FeedBackSchedule();
    }
    return instance;
  }

  public static void startManager() {
      FeedBackSchedule mng = get();
      mng.keepRunning = true;
      mng.start();
  }

  public static void stopManager() {
	  FeedBackSchedule mng = get();
      mng.keepRunning = false;
      mng.interrupt();
  }

  public void run() {
	
	  sleepTime = Const.getAPNS_FEEDBACK_CICLE();
	  if (sleepTime == -1) {
	     // default sleep time 24 hours
	     sleepTime = 1440;
	  }
	  // in minutes -> sleepTime x (60000 milisec)
	  sleepTime = sleepTime * 60000;
	
	  while (keepRunning) {
		  try {
			  try {
				  get().removeInactiveDevices();
	          } catch (Exception e) {
	        	  Log.warning("FeedBackSchedule", this, "run", "Failed to using APNS feedback service: ", e);
	          }
		      sleep(sleepTime);
		  } catch (InterruptedException e) {
			  if (keepRunning) {
				  Log.error("FeedBackSchedule", this, "run", "Thread interrupted: ", e); 
			  } else {
				  Log.error("FeedBackSchedule", this, "run", "Stopping event manager...", e); 
			  }
	      } catch (Exception e) {
	    	  Log.error("FeedBackSchedule", this, "run", "Failed to check events: ", e); 
	      }
	  }
  }
  
  private void removeInactiveDevices() {
	  try{
		  HashMap<String, List<Device>> devicesMap = new HashMap<String, List<Device>>();
		  NotificationMiddleLayer noteMid = NotificationMiddleLayer.getInstance();
		  List<Certificate> list =  noteMid.getAllCertificates();
		  Iterator<Certificate> it = list.iterator();
		  while(it.hasNext()){
			  Certificate curr = it.next();
			  List<Device> listDevsCurr = ApplePushNotifications.pushFeedbackService(curr.getCertificatePath(), curr.getAPNSPassword(), Const.getAPNS_PROD());
			  if(listDevsCurr.size()>0){
				  devicesMap.put(curr.getAppId()+SEPARATOR1+curr.getClientId(), listDevsCurr);
			  }
		  }
		  for (Map.Entry<String, List<Device>> entry : devicesMap.entrySet()){
			  String key = entry.getKey();
			  List<Device> listDevsCurr =  entry.getValue();
			  String[] aux = key.split(SEPARATOR1);
			  Iterator<Device> it2 = listDevsCurr.iterator();
			  while(it2.hasNext()){
				  noteMid.remDeviceToken(aux[0], aux[1], it2.next().getToken());
			  }
		  }
	  }catch(Exception e) {
		  Log.error("removeInactiveDevices", this, "removeInactiveDevices", "An error occorred when getting devices list: ", e); 
	  }
  }
  
}