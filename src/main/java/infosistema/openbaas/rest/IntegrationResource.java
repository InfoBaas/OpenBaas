package infosistema.openbaas.rest;

import java.util.List;
import java.util.Map.Entry;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.enums.OperatorEnum;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class IntegrationResource {

	
	private UsersMiddleLayer usersMid;
	private String appId;
	private SessionMiddleLayer sessionMid;
	private AppsMiddleLayer appsMid;

	@Context
	UriInfo uriInfo;
	
	public IntegrationResource(String appId) {
		this.usersMid = UsersMiddleLayer.getInstance();
		this.appId = appId;
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.appsMid = AppsMiddleLayer.getInstance();
	}
	

	@Path("/test")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response test(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		
		DocumentModel m = new DocumentModel();
		List<String> contains = m.getOperation(null, OperatorEnum.contains, null, "restaurante.nome", null, "es");
		List<String> notContains = m.getOperation(null, OperatorEnum.notContains, null, "restaurante.idade", null, "11");
		List<String> equals = m.getOperation(null, OperatorEnum.equals, null, "restaurante.nome", null, "rest2");
		List<String> diferent = m.getOperation(null, OperatorEnum.diferent, null, "restaurante.nome", null, "rest1");
		List<String> greater = m.getOperation(null, OperatorEnum.greater, null, "restaurante.idade", null, "16");
		List<String> greaterOrEqual = m.getOperation(null, OperatorEnum.greaterOrEqual, null, "restaurante.idade", null, "18");
		List<String> lesser = m.getOperation(null, OperatorEnum.lesser, null, "restaurante.idade", null, "19");
		List<String> lesserOrEqual = m.getOperation(null, OperatorEnum.lesserOrEqual, null, "restaurante.idade", null, "11");
		return Response.status(Status.OK).entity(lesserOrEqual).build();
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
		String appKey = null;
		String fbToken = null;
		User outUser = new User();
		String userId =null;
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.LOCATION))
				locationList = entry.getValue();
			if (entry.getKey().equalsIgnoreCase("user-agent")){
				userAgentList = entry.getValue();
			}	
			if (entry.getKey().equalsIgnoreCase(Const.APP_KEY))
				appKey = entry.getValue().get(0);
		}
		if(appKey==null)
			return Response.status(Status.BAD_REQUEST).entity("App Key not found").build();
		if(!appsMid.authenticateApp(appId,appKey))
			return Response.status(Status.UNAUTHORIZED).entity("Wrong App Key").build();
		if (locationList != null)
			location = locationList.get(0);
		if (userAgentList != null)
			userAgent = userAgentList.get(0);
		try {
			fbToken = (String) inputJsonObj.get("fbToken");
			JSONObject jsonReqFB = getFBInfo(fbToken);
			if(jsonReqFB == null)
				return Response.status(Status.BAD_REQUEST).entity("Bad FB Token!!!").build();
			email = (String) jsonReqFB.opt("email");
			socialNetwork = "Facebook";
			socialId = (String) jsonReqFB.get("id"); 
			userName = (String) jsonReqFB.opt("username");
		} catch (JSONException e) {
			Log.error("", this, "createOrLoginFacebookUser", "Error parsing the JSON.", e); 
			return Response.status(Status.BAD_REQUEST).entity(new Error("Error reading JSON")).build();
		}		
				
		if(email == null && userName != null){
			email = userName+"@facebook.com";
		}
		userId = usersMid.getUserIdUsingEmail(appId, email);
		userSocialId = usersMid.socialUserExistsInApp(appId, socialId, socialNetwork);
		
		if(userId!=null && userSocialId==null)
			response =  Response.status(302).entity(new Error("User "+userId+" with email: "+email+" already exists in app.")).build();
		if (userId==null) {
			if (uriInfo == null) uriInfo=ui;
			outUser = usersMid.createSocialUserAndLogin(headerParams, appId, userName,email, socialId, socialNetwork);
			Metadata meta = usersMid.createMetadata(appId, userId, null, userId, ModelEnum.users, location);
			Result res = new Result(outUser, meta);
			
			response = Response.status(Status.CREATED).entity(res).build();
		} else {
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
			if(validation){
				sessionMid.refreshSession(sessionToken, location, userAgent);
				outUser.setUserID(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setUserEmail(email);
				outUser.setUserName(userName);
				Metadata meta = usersMid.createMetadata(appId, userId, null, userId, ModelEnum.users, location);
				Result res = new Result(outUser, meta);
				response = Response.status(Status.OK).entity(res).build();
			}
		}
		return response;
	}
	
	private JSONObject getFBInfo(String fbToken) {
		JSONObject res = null;
		try{
			ClientConfig config = new DefaultClientConfig();
			Client client = Client.create(config);
			WebResource service = client.resource(UriBuilder.fromUri("https://graph.facebook.com/me?access_token="+fbToken).build());
			res = new JSONObject(service.accept(MediaType.APPLICATION_JSON).get(String.class));
		}
		catch (Exception e) {
			Log.error("", this, "FB Conn", "FB Conn", e);
		}
		return res;
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
		User outUser = new User();
		String appKey = null;
		String userId =null;
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.LOCATION))
				locationList = entry.getValue();
			if (entry.getKey().equalsIgnoreCase("user-agent"))
				userAgentList = entry.getValue();
			if (entry.getKey().equalsIgnoreCase(Const.APP_KEY))
				appKey = entry.getValue().get(0);
		}
		if(appKey==null)
			return Response.status(Status.BAD_REQUEST).entity("App Key not found").build();
		if(!appsMid.authenticateApp(appId,appKey))
			return Response.status(Status.UNAUTHORIZED).entity("Wrong App Key").build();
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
			Log.error("", this, "createOrLoginLinkedInUser", "Error parsing the JSON.", e); 
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
			Metadata meta = usersMid.createMetadata(appId, userId, null, userId, ModelEnum.users, location);
			Result res = new Result(outUser, meta);
			response = Response.status(Status.CREATED).entity(res).build();
		} else {
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
			if(validation){
				sessionMid.refreshSession(sessionToken, location, userAgent);
				outUser.setUserID(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setUserEmail(email);
				outUser.setUserName(userName);
				Metadata meta = usersMid.createMetadata(appId, userId, null, userId, ModelEnum.users, location);
				Result res = new Result(outUser, meta);
				response = Response.status(Status.OK).entity(res).build();
			}
		}
		return response;
	}
}
