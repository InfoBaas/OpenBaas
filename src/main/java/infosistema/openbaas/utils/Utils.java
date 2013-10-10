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

import infosistema.openbaas.dataaccess.sessions.RedisSessions;
import infosistema.openbaas.dataaccess.sessions.SessionInterface;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;

public class Utils {

	
	/*
	 * Returns a code corresponding to the sucess or failure Codes: 
	 * -2 -> Forbidden
	 * -1 -> Bad request
	 * 1 ->
	 * sessionExists
	 */
	public int treatParameters(UriInfo ui, HttpHeaders hh) {
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		Map<String, Cookie> cookiesParams = hh.getCookies();
		int code = -1;
		List<String> location = null;
		Cookie sessionToken = null;
		List<String> userAgent = null;
		// iterate cookies
		for (Entry<String, Cookie> entry : cookiesParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("sessionToken"))
				sessionToken = entry.getValue();
		}
		// iterate headers
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("sessionToken"))
				sessionToken = new Cookie("sessionToken", entry.getValue().get(0));
			if (entry.getKey().equalsIgnoreCase("location"))
				location = entry.getValue();
			else if (entry.getKey().equalsIgnoreCase("user-agent"))
				userAgent = entry.getValue();
		}
		if (sessionToken != null) {
			SessionInterface sessions = new RedisSessions();
			if (sessions.sessionTokenExists(sessionToken.getValue())) {
				code = 1;
				if (location != null) {
					sessions.refreshSession(sessionToken.getValue(), location.get(0), new Date().toString(), userAgent.get(0));
				} else
					sessions.refreshSession(sessionToken.getValue(), new Date().toString());
			} else {
				code = -2;
			}
		}
		return code;
	}
	
	public String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}
	
	public long roundUp(long num, long divisor) {
	    return (num + divisor - 1) / divisor;
	}
	
}
