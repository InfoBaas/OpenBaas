package rest_resources;


import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import resourceModelLayer.AppsMiddleLayer;
import rest_resources.AppsResource.PATCH;

public class UserRecoveryResource {

	private AppsMiddleLayer appsMid;
	private String appId;
	private String userId;
	@Context
	UriInfo uriInfo;
	
	public UserRecoveryResource(UriInfo uriInfo, AppsMiddleLayer appsMid,
			String appId, String userId) {
		this.appsMid = appsMid;
		this.appId = appId;
		this.uriInfo = uriInfo;
		this.userId = userId;
	}
	/*
	 * Returns a code corresponding to the sucess or failure Codes: -2 ->
	 * Forbidden -1 -> Bad request 1 -> sessionExists
	 * CHECK FILTERS
	 *//*
	private int treatParameters(UriInfo ui, HttpHeaders hh) {
		/*MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		MultivaluedMap<String, String> pathParams = ui.getPathParameters();
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
			if (appsMid.sessionTokenExists(sessionToken.getValue())) {
				code = 1;
				if (location != null) {
					appsMid.refreshSession(sessionToken.getValue(),
							location.get(0), userAgent.get(0));
				} else
					appsMid.refreshSession(sessionToken.getValue());
			} else {
				code = -2;
			}
		}
		return code;
	}*/
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response makeRecoveryRequest(JSONObject inputJson, @Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = "location") String location){
		Response response = null;
		String email = null;
			try {
				email = (String) inputJson.get("email");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			String newPass="aaa";
			boolean opOk = appsMid.recoverUser(appId, userId, email, ui,newPass,null,null);
			if(opOk)
				response = Response.status(Status.OK)
				.entity("Email sent with recovery details.").build();
			else
				response = Response.status(Status.BAD_REQUEST)
				.entity("Wrong email.").build();
		return response;
		
	}
	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response changePassword(JSONObject inputJson, @QueryParam("recoveryCode") String recoveryCode){
		String password = null;
		Response response = null;
		if(recoveryCode == null){
			return Response.status(Status.BAD_REQUEST)
					.entity("Error handling the request.").build();
		}
		try {
			password = (String) inputJson.get("password");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String dbRecoveryCode = appsMid.getRecoveryCode(appId, userId);
	
		if(dbRecoveryCode.equalsIgnoreCase(recoveryCode)){
			this.appsMid.updateUserPassword(appId, userId, password);
			response = Response.status(Status.OK)
					.entity("Your password has been changed.").build();
		}else{
			Response.status(Status.BAD_REQUEST)
			.entity("Error handling the request.").build();
		}
		return response;		
	}
}
