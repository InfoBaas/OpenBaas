package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.rest.AppResource.PATCH;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONObject;


public class UsersResource {

	private UsersMiddleLayer usersMid;
	private SessionMiddleLayer sessionMid;
	private String appId;

	@Context
	UriInfo uriInfo;

	public UsersResource(UriInfo uriInfo, String appId) {
		this.usersMid = UsersMiddleLayer.getInstance();
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.appId = appId;
		this.uriInfo = uriInfo;
	}

	// *** CREATE *** //

	
	// *** UPDATE *** //
	
	/**
	 * Updates the user, optional fields: "email", "password", "alive".
	 * 
	 * @param userId
	 * @param inputJsonObj
	 * @return
	 */
	@Path("{userId}")
	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateUser(@PathParam("userId") String userId, JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		String appKey = null;
		String location = null;
		
		String newUserName = null;
		String userAgent = null;
		List<String> userAgentList = null;
		String newUserFile = null;
		String newEmail = null;
		Boolean newBaseLocationOption = null;
		String newBaseLocation = null;
		Boolean userUpdateFields = false;
		
		List<String> locationList = null;
		Cookie sessionToken=null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.LOCATION))
				locationList = entry.getValue();
			if (entry.getKey().equalsIgnoreCase(Const.APP_KEY))
				appKey = entry.getValue().get(0);
			if (entry.getKey().equalsIgnoreCase("user-agent"))
				userAgentList = entry.getValue();
			if (entry.getKey().equalsIgnoreCase(Const.SESSION_TOKEN))
				sessionToken = new Cookie(Const.SESSION_TOKEN, entry.getValue().get(0));
		}
		
		if(appKey==null)
			return Response.status(Status.BAD_REQUEST).entity("App Key not found").build();
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
				try {
				if(usersMid.userIdExists(appId, userId)){
					newUserName = (String) inputJsonObj.opt(User.USER_NAME);
					newEmail = (String) inputJsonObj.opt(User.EMAIL);
					newUserFile = (String) inputJsonObj.opt("userFile");
					newBaseLocationOption = (Boolean) inputJsonObj.opt(User.BASE_LOCATION_OPTION);
					newBaseLocation = (String) inputJsonObj.opt(User.BASE_LOCATION);
					userUpdateFields = usersMid.updateUser(appId, userId, newUserName, newEmail,
							newUserFile, newBaseLocationOption, newBaseLocation, location);
					if(newBaseLocationOption==true)
						usersMid.updateUserLocation(userId, appId, newBaseLocation);
					if(userUpdateFields){
						sessionMid.refreshSession(sessionToken.getValue(), location, userAgent);
						Metadata meta = usersMid.updateMetadata(appId, null, userId, userId, ModelEnum.users, location);
						Result res = new Result(usersMid.getUserInApp(appId, userId), meta);		
						return Response.status(Status.OK).entity(res).build();
					}
				}
			}catch(Exception e){
				Log.error("", this, "Internal Error", "Internal Error", e); 
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Internal Error.")).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** DELETE *** //
	
	/**
	 * Deletes the user.
	 * 
	 * @param userId
	 * @return
	 */
	@Path("{userId}")
	@DELETE
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteUser(@PathParam("userId") String userId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			Log.debug("", this, "deleteUser", "*Deleting User(setting as inactive)*");
			boolean sucess = usersMid.deleteUserInApp(appId, userId);
			if (sucess)
				response = Response.status(Status.OK).entity(userId).build();
			else
				response = Response.status(Status.NOT_FOUND).entity(userId).build();
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

	
	// *** GET LIST *** //
	
	/**
	 * Gets all the users in the application.
	 * 
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response find(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("query") JSONObject query, @QueryParam(Const.RADIUS) String radiusStr,
			@QueryParam(Const.LAT) String latitudeStr, @QueryParam(Const.LONG) String longitudeStr,
			@QueryParam(Const.PAGE_NUMBER) String pageNumberStr, @QueryParam(Const.PAGE_SIZE) String pageSizeStr, 
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_BY) String orderTypeStr) {
		QueryParameters qp = QueryParameters.getQueryParameters(appId, query, radiusStr, latitudeStr, longitudeStr, 
				pageNumberStr, pageSizeStr, orderByStr, orderTypeStr, ModelEnum.users);
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();

		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				ListResult res = usersMid.find(qp);
				response = Response.status(Status.OK).entity(res).build();
			} catch (Exception e) {
				response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

	
	// *** GET *** //

	/**
	 * Gets the user fields.
	 * 
	 * @param userId
	 * @return
	 */
	@Path("{userId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response findById(@PathParam("userId") String userId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			Log.debug("", this, "findById", "********Finding User info************");
			User temp = null;
			if (AppsMiddleLayer.getInstance().appExists(appId)) {
				temp = usersMid.getUserInApp(appId, userId);
				if (temp != null) {
					Metadata meta = usersMid.getMetadata(appId, null, userId, ModelEnum.users);
					Result res = new Result(temp, meta);
					Log.debug("", this, "findById", "userId: " + temp.getUserId()+ "email: " + temp.getEmail());
					response = Response.status(Status.OK).entity(res).build();
				} else {
					response = Response.status(Status.NOT_FOUND).entity(temp).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

	
	// *** OTHERS *** //

	// *** RESOURCES *** //

	/**
	 * Launches the sessions resource.
	 * 
	 * @param userId
	 * @return
	 */
	@Path("{userId}/sessions")
	public SessionsResource sessions(@PathParam("userId") String userId) {
		try {
			return new SessionsResource(appId, userId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "sessions", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	@Path("{userId}/recovery")
	public UserRecoveryResource userRecovery(@PathParam("userId") String userId) {
		try {
			return new UserRecoveryResource(uriInfo, appId, userId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "userRecovery", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	@Path("{userId}/data")
	public UserDataResource userData(@PathParam("userId") String userId) {
		try {
			return new UserDataResource(uriInfo, appId, userId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "userData", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle the user confirmation
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{userId}/confirmation")
	public UserConfirmationResource userConfirmation(@PathParam("userId") String userId) {
		try {
			return new UserConfirmationResource(uriInfo, appId, userId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "userConfirmation", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

}