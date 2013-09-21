package rest_resources;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import modelInterfaces.User;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.PasswordEncryptionService;
import rest_resources.AppsResource.PATCH;

public class UsersResource {

	private AppsMiddleLayer appsMid;
	private String appId;
	private static final int IDLENGTH = 3;

	@Context
	UriInfo uriInfo;

	public UsersResource(UriInfo uriInfo, AppsMiddleLayer appsMidLayer,
			String appId) {
		this.appsMid = appsMidLayer;
		this.appId = appId;
		this.uriInfo = uriInfo;
	}

	/*
	 * Returns a code corresponding to the sucess or failure Codes: -2 ->
	 * Forbidden -1 -> Bad request 1 -> sessionExists
	 */
	private int treatParameters(UriInfo ui, HttpHeaders hh) {
//		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
//		MultivaluedMap<String, String> pathParams = ui.getPathParameters();
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
					appsMid.refreshSession(sessionToken.getValue(),	location.get(0), userAgent.get(0));
				} else
					appsMid.refreshSession(sessionToken.getValue());
			} else {
				code = -2;
			}
		}
		return code;
	}

	/**
	 * Gets all the users in the application.
	 * 
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response findAll(@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("***********************************");
			System.out.println("********Finding all USERS**********");
			if (!appsMid.appExists(appId))
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			else {
				Set<String> temp = appsMid.getAllUserIdsForApp(appId);
				response = Response.status(Status.OK).entity(temp).build();
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
		int code = this.treatParameters(ui, hh);
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
	 * Creates a user in the application, necessary fields: "password";
	 * "userName" or "email" (at least one of these is necessary").
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	/*
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createUser(JSONObject inputJsonObj, @Context UriInfo ui,@Context HttpHeaders hh) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			long start = System.currentTimeMillis();
			System.out.println("************************************");
			System.out.println("***********Creating User************");
			String email = null;
			String password = null;
			String userName = null;
			String userFile = null;
			// User temp = null;
			String userId = null;
			byte[] salt = null;
			byte[] hash = null;
			Boolean readOk = false;
			Boolean sucess = false;
			try {
				userName = (String) inputJsonObj.opt("userName");
				userFile = (String) inputJsonObj.opt("userFile");
				email = (String) inputJsonObj.get("email");
				password = (String) inputJsonObj.get("password");
				readOk = true;
				PasswordEncryptionService service = new PasswordEncryptionService();
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
			} catch (JSONException e) {
				System.out.println("Error parsing the JSON file.");
				e.printStackTrace();
			}
			if (userName == null) {
				userName = email;
			}
			if (readOk && appsMid.appExists(appId)) {
				userId = getRandomString(IDLENGTH);
				while (appsMid.identifierInUseByUserInApp(appId, userId))
					userId = getRandomString(IDLENGTH);

				if (!appsMid.userExistsInApp(appId, userId, email)) {
				//if(true){
					System.out.println("*****************Creating user***************");
					System.out.println("userId: " + userId + " email: " + email);
					System.out.println("********************************************");
					if (!appsMid.confirmUsersEmailOption(this.appId)) {//
						sucess = appsMid.createUser(this.appId, userId,	userName, email, salt, hash, userFile);
						response = Response.status(Status.CREATED).entity(userId).build();
					} else if (appsMid.confirmUsersEmailOption(this.appId)) {
						boolean emailConfirmed = false;
						if(uriInfo==null) 
							uriInfo=ui;
						sucess = appsMid.createUserWithEmailConfirmation(this.appId, userId, 
								userName, email, salt,hash, userFile, emailConfirmed, uriInfo);
						response = Response.status(Status.CREATED).entity(userId).build();
					}
				} else {
					String foundUserId = appsMid.getUserIdUsingUserName(appId,userName);
					// 302 = found
					response = Response.status(302).entity(foundUserId)
							//.header("content-location",uriInfo.getAbsolutePath() + "/"+ foundUserId)
							.build();
				}
			} else {
				response = Response.status(Status.BAD_REQUEST).entity(userName).build();
			}
			System.out.println("TIME TO FULLFILL REQUEST: "	+ (System.currentTimeMillis() - start));
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}*/

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
		int code = this.treatParameters(ui, hh);
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
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			String email = null;
			String password = null;
			String alive = null;
			byte[] salt = null;
			byte[] hash = null;
			User temp = null;
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
			boolean sucess = true;
			if (this.appsMid.identifierInUseByUserInApp(this.appId, userId)) {
				if (email != null && password != null && alive != null) {
					appsMid.updateUser(appId, userId, email, hash, salt, alive);
					sucess = true;
				} else if (email != null && password != null) {
					appsMid.updateUser(appId, userId, email, hash, salt);
					sucess = true;
				} else if (email != null) {
					appsMid.updateUser(appId, userId, email);
					sucess = true;
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
	 * Identifier generator.
	 * 
	 * @param length
	 * @return
	 */
	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
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