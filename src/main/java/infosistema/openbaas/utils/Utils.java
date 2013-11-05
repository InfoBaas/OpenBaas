package infosistema.openbaas.utils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

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
		/*List<String> userAgentList = null;
		List<String> locationList = null;*/
		String userAgent = null;
		String location = null;
		Cookie sessionToken = null;
		// iterate cookies
		for (Entry<String, Cookie> entry : cookiesParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("sessionToken"))
				sessionToken = entry.getValue();
		}
		// iterate headers
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("sessionToken"))
				sessionToken = new Cookie("sessionToken", entry.getValue().get(0));
			/*else if (entry.getKey().equalsIgnoreCase("location"))
				locationList = entry.getValue();
			else if (entry.getKey().equalsIgnoreCase("user-agent"))
				userAgentList = entry.getValue();*/
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
	
	public static String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}
	
	public static long roundUp(long num, long divisor) {
	    return (num + divisor - 1) / divisor;
	}
	
}
