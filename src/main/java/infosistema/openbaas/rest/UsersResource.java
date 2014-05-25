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
package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.rest.AppResource.PATCH;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.util.Map;

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

import org.codehaus.jettison.json.JSONArray;
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
	 * Updates the user, optional fields: email, password, alive.
	 * 
	 * @param userId
	 * @param inputJsonObj
	 * @return
	 */
	@Path("{userId}")
	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateUser(@PathParam(Const.USER_ID) String userId, JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		String appKey = null;
		String location = null;
		String newUserName = null;
		String userAgent = null;
		String newUserFile = null;
		String newEmail = null;
		Boolean newBaseLocationOption = null;
		String newBaseLocation = null;
		//Cookie sessionToken=null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		try {
			location = headerParams.getFirst(Const.LOCATION);
		} catch (Exception e) { }
		try {
			appKey = headerParams.getFirst(Application.APP_KEY);
		} catch (Exception e) { }
		try {
			userAgent = headerParams.getFirst(Const.USER_AGENT);
		} catch (Exception e) { }
		
		String sessionToken = Utils.getSessionToken(hh);
		
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
					Map<String, String> metadata = Metadata.getNewMetadata(location);
					Result res = usersMid.updateUser(appId, userId, newUserName, newEmail, newUserFile, newBaseLocationOption, newBaseLocation, location, metadata);
					
					if (newBaseLocationOption == null) {
						User user = (User)usersMid.getUserInApp(appId, userId).getData();
						newBaseLocationOption = user.getBaseLocationOption().equals("true");
					}

					if (newBaseLocationOption == true)
						usersMid.updateUserLocation(userId, appId, newBaseLocation, metadata);
					if (res != null){
						sessionMid.refreshSession(sessionToken, location, userAgent);
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
	public Response deleteUser(@PathParam(Const.USER_ID) String userId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			boolean sucess = usersMid.deleteUserInApp(appId, userId);
			if (sucess){
				response = Response.status(Status.OK).entity(userId).build();
			}
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
	public Response find(@Context UriInfo ui, @Context HttpHeaders hh, @QueryParam("show") JSONArray arrayShow,
			@QueryParam("query") JSONObject query, @QueryParam(Const.RADIUS) String radiusStr,
			@QueryParam(Const.LAT) String latitudeStr, @QueryParam(Const.LONG) String longitudeStr,
			@QueryParam(Const.PAGE_NUMBER) String pageNumberStr, @QueryParam(Const.PAGE_SIZE) String pageSizeStr,
			@QueryParam(Const.ELEM_COUNT) String pageCount, @QueryParam(Const.ELEM_INDEX) String pageIndex,
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_TYPE) String orderTypeStr) {
		QueryParameters qp = QueryParameters.getQueryParameters(appId, null, query, radiusStr, latitudeStr, longitudeStr, 
				pageNumberStr, pageSizeStr, orderByStr, orderTypeStr, ModelEnum.users,pageCount,pageIndex);
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId)) {
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		}
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				ListResult res = usersMid.find(qp,arrayShow);
				response = Response.status(Status.OK).entity(res).build();
			} catch (Exception e) {
				Log.error("", this, "find", "********Find Users info************", e);
				response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1) {
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		}
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
	public Response findById(@PathParam(Const.USER_ID) String userId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if (AppsMiddleLayer.getInstance().appExists(appId)) {
				Result res = usersMid.getUserInApp(appId, userId);
				if (res != null) {
					response = Response.status(Status.OK).entity(res).build();
				} else {
					response = Response.status(Status.NOT_FOUND).entity(userId).build();
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
	public SessionsResource sessions(@PathParam(Const.USER_ID) String userId) {
		try {
			return new SessionsResource(appId, userId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "sessions", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	@Path("{userId}/recovery")
	public UserRecoveryResource userRecovery(@PathParam(Const.USER_ID) String userId) {
		try {
			return new UserRecoveryResource(uriInfo, appId, userId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "userRecovery", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	@Path("{userId}/data")
	public UserDataResource userData(@PathParam(Const.USER_ID) String userId) {
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
	public UserConfirmationResource userConfirmation(@PathParam(Const.USER_ID) String userId) {
		try {
			return new UserConfirmationResource(uriInfo, appId, userId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "userConfirmation", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

}