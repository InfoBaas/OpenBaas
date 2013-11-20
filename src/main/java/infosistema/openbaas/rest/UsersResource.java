package infosistema.openbaas.rest;

import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.rest.AppResource.PATCH;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;


import org.codehaus.jettison.json.JSONObject;


public class UsersResource {

	private UsersMiddleLayer usersMid;
	private String appId;

	@Context
	UriInfo uriInfo;

	public UsersResource(UriInfo uriInfo, String appId) {
		this.usersMid = MiddleLayerFactory.getUsersMiddleLayer();
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
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String email = null;
			String password = null;
			String alive = null;
			byte[] salt = null;
			byte[] hash = null;
			//User temp = null;
			email = (String) inputJsonObj.opt("email");
			password = (String) inputJsonObj.opt("password");
			alive = (String) inputJsonObj.opt("alive");
			PasswordEncryptionService service = new PasswordEncryptionService();
			if (password != null) {
				try {
					salt = service.generateSalt();
					hash = service.getEncryptedPassword(password, salt);
				} catch (NoSuchAlgorithmException e) {
					Log.error("", this, "updateUser", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
				} catch (InvalidKeySpecException e) {
					Log.error("", this, "updateUser", "Invalid Key.", e); 
				}
			}
			if (usersMid.identifierInUseByUserInApp(this.appId, userId)) {
				if (email != null && password != null && alive != null) {
					usersMid.updateUser(appId, userId, email, hash, salt, alive);
				} else if (email != null && password != null) {
					usersMid.updateUser(appId, userId, email, hash, salt);
				} else if (email != null) {
					usersMid.updateUser(appId, userId, email);
				}
				response = Response.status(Status.OK).entity(usersMid.getUserInApp(appId, userId)).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity("Invalid Session Token.").build();
			}
			Log.debug("", this, "updateUser", "appId: " + appId + "UserId: " + userId);
			Log.debug("", this, "updateUser", "email: " + email + "alive: " + alive);
			Log.debug("", this, "updateUser", "hash: " + hash.toString());
			Log.debug("", this, "updateUser", "salt: " + salt.toString());
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

	
	// *** DELETE *** //
	
	//TODO: LOCATION (remove sessions???)
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
	public Response findAll(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("lat") String latitudeStr,	@QueryParam("long") String longitudeStr, @QueryParam("radius") String radiusStr, 
			@QueryParam(Const.PAGE_NUMBER) Integer pageNumber, @QueryParam(Const.PAGE_SIZE) Integer pageSize, 
			@QueryParam(Const.ORDER_BY) String orderBy, @QueryParam(Const.ORDER_BY) String orderType ) {
		if (pageNumber == null) pageNumber = Const.getPageNumber();
		if (pageSize == null) 	pageSize = Const.getPageSize();
		if (orderBy == null) 	orderBy = Const.getOrderBy();
		if (orderType == null) 	orderType = Const.getOrderType();
		Double latitude = null;
		Double longitude = null;
		Double radius = null;
		if (latitudeStr != null) {
			try {
				latitude = Double.parseDouble(latitudeStr);
			} catch (Exception e) { }
		}
		if (longitudeStr != null) {
			try {
				longitude = Double.parseDouble(longitudeStr);
			} catch (Exception e) { }
		}
		if (radiusStr != null) {
			try {
				radius = Double.parseDouble(radiusStr);
			} catch (Exception e) { }
		}
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if (!MiddleLayerFactory.getAppsMiddleLayer().appExists(appId))
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			else {
				ArrayList<String> temp = usersMid.getAllUserIdsForApp(appId, latitude, longitude, radius, pageNumber, pageSize, orderBy, orderType);
				ListResult res = new ListResult(temp, pageNumber);
				response = Response.status(Status.OK).entity(res).build();
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
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			Log.debug("", this, "findById", "********Finding User info************");
			User temp = null;
			if (MiddleLayerFactory.getAppsMiddleLayer().appExists(appId)) {
				if (usersMid.identifierInUseByUserInApp(appId, userId)) {
					temp = usersMid.getUserInApp(appId, userId);
					Log.debug("", this, "findById", "userId: " + temp.getUserId()+ "email: " + temp.getEmail());
					response = Response.status(Status.OK).entity(temp).build();
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