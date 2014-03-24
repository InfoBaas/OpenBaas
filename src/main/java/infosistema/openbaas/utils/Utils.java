package infosistema.openbaas.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import infosistema.openbaas.dataaccess.models.SessionModel;

public class Utils {
	
	/*
	 * Returns a code corresponding to the sucess or failure Codes: 
	 * -2 -> Forbidden
	 * -1 -> Bad request
	 * 1 ->
	 * sessionExists
	 */
	public static int treatParameters(UriInfo ui, HttpHeaders hh) {
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		Map<String, Cookie> cookiesParams = hh.getCookies();
		int code = -1;
		String userAgent = null;
		String location = null;
		Cookie sessionToken = null;
		try {
			sessionToken = new Cookie(Const.SESSION_TOKEN, headerParams.getFirst(Const.SESSION_TOKEN));
		} catch (Exception e) {
			try {
				sessionToken = cookiesParams.get(Const.SESSION_TOKEN);
			} catch (Exception e2) { }
		}
		if (sessionToken != null) {
			SessionModel sessions = new SessionModel();
			if (sessions.sessionTokenExists(sessionToken.getValue())) {
				code = 1;
				sessions.refreshSession(sessionToken.getValue(), location, new Date().toString(), userAgent);
			} else {
				code = -2;
			}
		}
		return code;
	}
	
	public static int treatParametersAdmin(UriInfo ui, HttpHeaders hh) {
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		Map<String, Cookie> cookiesParams = hh.getCookies();
		int code = -1;
		String userAgent = null;
		String location = null;
		Cookie sessionToken = null;
		try {
			sessionToken = new Cookie(Const.SESSION_TOKEN, headerParams.getFirst(Const.SESSION_TOKEN));
		} catch (Exception e) {
			try {
				sessionToken = cookiesParams.get(Const.SESSION_TOKEN);
			} catch (Exception e2) { }
		}
		if (sessionToken != null && sessionToken.getValue().equals(Const.getADMIN_TOKEN())) {
			SessionModel sessions = new SessionModel();
			if (sessions.sessionTokenExists(sessionToken.getValue())) {
				code = 1;
				sessions.refreshSession(sessionToken.getValue(), location, new Date().toString(), userAgent);
			} else {
				code = -2;
			}
		}
		return code;
	}
	
	public static String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}
	
	public static long roundUp(long num, long divisor) {
	    return (num + divisor - 1) / divisor;
	}

	public static String getSessionToken(HttpHeaders hh) {
		String sessionToken = null; 
		try {
			sessionToken = hh.getRequestHeaders().getFirst(Const.SESSION_TOKEN);
		} catch (Exception e) {
			Log.error("", "infosistema.openbaas.utils.Utils", "getSessionToken", "No session token in request header.", e);
		}
		return sessionToken;
	}
	
	public static Date getDate() {
		return new Date();
	}
	
	public static String printDate(Date date) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(date);
	}
	
	public static void printMemoryStats() {
		int mb = 1024*1024;
		 
		//Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();
		 
		//Print used memory
		StringBuffer str = new StringBuffer();
		str.append("Used: " + String.valueOf((runtime.totalMemory() - runtime.freeMemory()) / mb));
		str.append(" - Free: " + String.valueOf(runtime.freeMemory() / mb));
		str.append(" - Total: " + String.valueOf(runtime.totalMemory() / mb));
		str.append(" - Max: " + String.valueOf(runtime.maxMemory() / mb));
		Log.info("", null, "Memory - ",str.toString());
	}
	
	public static String getStringByJSONArray(JSONArray array, String separator) {
		String res = "";
		try {
			String aux="";
			for (int i = 0; i < array.length(); i++) {
				aux += (String)array.getString(i)+separator;
			}
			res = aux.substring(0,aux.length()-1);
		} catch (JSONException e) {
			Log.error("", "", "getNameByArray", "Error parsing the JSON.", e); 
		}
		return res;
	}
	
	public static List<String> getListByString(String list, String separator) {
		List<String> res = null;
		try {
			String[] aux = list.split(separator);
			if(list!=null)
				res = Arrays.asList(aux);
		} catch (Exception e) {
			Log.error("", "", "getStringByJSONArray", "Error occored.", e); 
		}
		return res;
	}	
	
	public static List<String> getListByJsonArray(JSONArray jsonArray) {
		List<String> res = new ArrayList<String>();
		try {
			if(jsonArray.length()>0){
				for(int i=0; i<jsonArray.length(); i++){
					res.add(jsonArray.getString(i));
				}
			}
		} catch (Exception e) {
			Log.error("", "", "getStringByJSONArray", "Error occored.", e); 
		}
		return res;
	}	
}
