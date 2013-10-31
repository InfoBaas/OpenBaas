package infosistema.openbaas.rest;

import java.util.List;
import java.util.Map.Entry;

import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.model.user.User;
import infosistema.openbaas.model.user.UserInterface;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class IntegrationResource {

	
	private UsersMiddleLayer usersMid;
	private String appId;
	private SessionMiddleLayer sessionMid;

	@Context
	UriInfo uriInfo;
	
	public IntegrationResource(String appId) {
		this.usersMid = MiddleLayerFactory.getUsersMiddleLayer();
		this.appId = appId;
		this.sessionMid = MiddleLayerFactory.getSessionMiddleLayer();
	}
	

	/**
	 * Creates a user in the application. Necessary fields: "facebook id"
	 * and "email". if the user already register only signin. if not signup
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@Path("/facebook")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrLoginFacebookUser(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		String email = null;
		String socialNetwork = null;
		String socialId = null;
		String userSocialId = null;
		String userName = null;
		List<String> locationList = null;
		List<String> userAgentList = null;
		String userAgent = null;
		String location = null;
		UserInterface outUser = new User();
		String userId =null;
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("location"))
				locationList = entry.getValue();
			else if (entry.getKey().equalsIgnoreCase("user-agent")){
				userAgentList = entry.getValue();
			}	
		}
		if (locationList != null)
			location = locationList.get(0);
		if (userAgentList != null)
			userAgent = userAgentList.get(0);
		try {
			email = (String) inputJsonObj.get("email");
			socialNetwork = "Facebook";
			socialId = ((Integer) inputJsonObj.get("socialId")).toString(); 
			userName = (String) inputJsonObj.opt("userName");
			
		} catch (JSONException e) {
			System.out.println("Error Reading the jsonFile");
			return Response.status(Status.BAD_REQUEST).entity("Error reading JSON").build();
		}
		if (userName == null) {
			userName = email;
		}
		userId = usersMid.getUserIdUsingEmail(appId, email);
		userSocialId = usersMid.socialUserExistsInApp(appId, socialId, socialNetwork);
		
		if(userId!=null && userSocialId==null)
			response =  Response.status(302).entity("User "+userId+" with email: "+email+" already exists in app.").build();
		
		
		if (userId==null) {
			if (uriInfo == null) uriInfo=ui;
			outUser = usersMid.createSocialUserAndLogin(headerParams, appId, userName,email, socialId, socialNetwork);
			response = Response.status(Status.CREATED).entity(outUser).build();
		} else {
			String sessionToken = Utils.getRandomString(Const.IDLENGTH);
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
			if(validation){
				sessionMid.refreshSession(sessionToken, location, userAgent);
				outUser.setUserID2(userId);
				outUser.setReturnToken(sessionToken);
				response = Response.status(Status.OK).entity(outUser).build();
			}
		}
		return response;
	}
	
	/**
	 * Creates a user in the application. Necessary fields: "linkedin id".
	 * if the user already register only signin. if not signup
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@Path("/linkedin")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrLoginLinkedInUser(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		String email = null;
		String socialNetwork = null;
		String socialId = null;
		String userSocialId = null;
		String userName = null;
		List<String> locationList = null;
		List<String> userAgentList = null;
		String userAgent = null;
		String location = null;
		UserInterface outUser = new User();
		String userId =null;
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("location"))
				locationList = entry.getValue();
			else if (entry.getKey().equalsIgnoreCase("user-agent")){
				userAgentList = entry.getValue();
			}	
		}
		if (locationList != null)
			location = locationList.get(0);
		if (userAgentList != null)
			userAgent = userAgentList.get(0);
		try {
			email = (String) inputJsonObj.get("email");
			socialNetwork = "LinkedIn";
			socialId = ((Integer) inputJsonObj.get("socialId")).toString(); 
			userName = (String) inputJsonObj.opt("userName");
			
		} catch (JSONException e) {
			System.out.println("Error Reading the jsonFile");
			return Response.status(Status.BAD_REQUEST).entity("Error reading JSON").build();
		}
		if (userName == null) {
			userName = email;
		}
		userId = usersMid.getUserIdUsingEmail(appId, email);
		userSocialId = usersMid.socialUserExistsInApp(appId, socialId, socialNetwork);
		
		if(userId!=null && userSocialId==null)
			response =  Response.status(302).entity("User "+userId+" with email: "+email+" already exists in app.").build();
		
		
		if (userId==null) {
			if (uriInfo == null) uriInfo=ui;
			outUser = usersMid.createSocialUserAndLogin(headerParams, appId, userName,email, socialId, socialNetwork);
			response = Response.status(Status.CREATED).entity(outUser).build();
		} else {
			String sessionToken = Utils.getRandomString(Const.IDLENGTH);
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
			if(validation){
				sessionMid.refreshSession(sessionToken, location, userAgent);
				outUser.setUserID2(userId);
				outUser.setReturnToken(sessionToken);
				response = Response.status(Status.OK).entity(outUser).build();
			}
		}
		return response;
	}
}
