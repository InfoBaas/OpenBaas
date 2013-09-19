package simulators;



import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import modelInterfaces.Application;
import modelInterfaces.User;


import resourceModelLayer.AppsMiddleLayer;
import rest_Models.DefaultApplication;
import rest_Models.DefaultUser;

public class AppSimulator {
//	Map<String, Application> apps;
//	AppsMiddleLayer testingApps;
//	
//	public AppSimulator(){
//		apps= new HashMap<String, Application>();
//		testingApps = new AppsMiddleLayer();
//		apps.put("1", new DefaultApplication("1"));
//		apps.put("2", new DefaultApplication("2"));
//		
////		Application temp = new DefaultApplication("1");
////		temp.putUser("m.aniceto", "lima.aniceto@gmail.com", "1234");
////		temp.putUser("t.rodrigues", "t.rodrigues@infosistema.com", "1234");
////		temp.putUser("joao", "joao@gmail.com", "1234");
////		temp.putUser("silva", "silva@infosistema.com", "1234");
//	}
////	public boolean createApp(String id, Date date){
////		testingApps.addApp(id, date);
////		Application temp = testingApps.getApp(id);
////		if(temp.getID().equals(id)){
////			return true;
////		}
////		return false;
////	}
//	public boolean createApp(Application input){
//		testingApps.addApp(input);
//		Application temp = testingApps.getApp(input.getID());
//		if(temp.getID().equals(input.getID())){
//			return true;
//		}
//		return false;
//	}
//	public boolean removeApp(String appId){
//		if(apps.containsKey(appId)){
//			this.apps.remove(appId);
//		}
//		if(!apps.containsKey(appId))
//			return true;
//		return false;
//	}
//	public boolean getApp1(String appId){
//		if (this.apps.containsKey(appId) && this.apps.get(appId).getAppID() == appId){
//			return true;
//		}
//		else{
//			return false;
//		}
//	}
//	//-------------------------------USERS---------------------------
//	public boolean putUser(String appId, String userId, String email, String password){
//		Application temp = apps.get(appId);
//		temp.putUser(appId, email, password);
//		apps.put(appId, temp);
//		temp = apps.get(appId);
//		if(temp.userExists(userId) && temp.getUser(userId).getId().equals(userId)){
//			return true;
//		}
//		return false;
//	}
//	
}
