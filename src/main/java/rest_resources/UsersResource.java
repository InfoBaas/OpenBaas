package rest_resources;

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

import modelInterfaces.User;

import org.codehaus.jettison.json.JSONObject;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.IdsResultSet;
import rest_Models.PasswordEncryptionService;
import rest_resources.AppsResource.PATCH;
import utils.Const;
import utils.Utils;

public class UsersResource {

	private static final Utils utils = new Utils();
	private AppsMiddleLayer appsMid;
	private String appId;

	@Context
	UriInfo uriInfo;

	public UsersResource(UriInfo uriInfo, AppsMiddleLayer appsMidLayer,
			String appId) {
		this.appsMid = appsMidLayer;
		this.appId = appId;
		this.uriInfo = uriInfo;
	}

	
	//TODO: PAGINATION
	/**
	 * Gets all the users in the application.
	 * 
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response findAll(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("***********************************");
			System.out.println("********Finding all USERS**********");
			if (!appsMid.appExists(appId))
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			else {
				ArrayList<String> temp = appsMid.getAllUserIdsForApp(appId,pageNumber,pageSize,orderBy,orderType);
				IdsResultSet res = new IdsResultSet(temp,pageNumber);
				response = Response.status(Status.OK).entity(res).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

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
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("********Finding User info************");
			User temp = null;
			if (appsMid.appExists(appId)) {
				if (appsMid.identifierInUseByUserInApp(appId, userId)) {
					temp = appsMid.getUserInApp(appId, userId);
					System.out.println("userId: " + temp.getUserId()+ "email: " + temp.getEmail());
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
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("*Deleting User(setting as inactive)*");
			boolean sucess = this.appsMid.deleteUserInApp(appId, userId);
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
	public Response updateUser(@PathParam("userId") String userId,
			JSONObject inputJsonObj, @Context UriInfo ui,
			@Context HttpHeaders hh) {
		Response response = null;
		int code = utils.treatParameters(ui, hh);
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
					System.out.println("Hashing Algorithm failed, please review the PasswordEncryptionService.");
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					System.out.println("Invalid Key.");
					e.printStackTrace();
				}
			}
			//boolean sucess;
			if (this.appsMid.identifierInUseByUserInApp(this.appId, userId)) {
				if (email != null && password != null && alive != null) {
					appsMid.updateUser(appId, userId, email, hash, salt, alive);
					//sucess = true;
				} else if (email != null && password != null) {
					appsMid.updateUser(appId, userId, email, hash, salt);
					//sucess = true;
				} else if (email != null) {
					appsMid.updateUser(appId, userId, email);
					//sucess = true;
				}
				response = Response.status(Status.OK).entity(this.appsMid.getUserInApp(appId, userId)).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity("Invalid Session Token.").build();
			}
			System.out.println("appId: " + appId + "UserId: " + userId);
			System.out.println("email: " + email + "alive: " + alive);
			System.out.println("hash: " + hash.toString());
			System.out.println("salt: " + salt.toString());
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

	/**
	 * Launches the sessions resource.
	 * 
	 * @param userId
	 * @return
	 */
	@Path("{userId}/sessions")
	public SessionsResource sessions(@PathParam("userId") String userId) {
		try {
			return new SessionsResource(appsMid, appId, userId);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	@Path("{userId}/recovery")
	public UserRecoveryResource userRecovery(@PathParam("userId") String userId) {
		try {
			return new UserRecoveryResource(uriInfo, appsMid, appId, userId);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	@Path("{userId}/data")
	public UserDataResource userData(@PathParam("userId") String userId) {
		try {
			return new UserDataResource(uriInfo, appsMid, appId, userId);
		} catch (IllegalArgumentException e) {
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
	public UserConfirmationResource userConfirmation(
			@PathParam("userId") String userId) {
		try {
			return new UserConfirmationResource(uriInfo, appsMid, appId, userId);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}
}