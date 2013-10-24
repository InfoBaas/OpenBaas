package infosistema.openbaas.rest;

import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.model.user.User;
import infosistema.openbaas.model.user.UserInterface;
import infosistema.openbaas.rest.AppsResource.PATCH;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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


public class AccountResource {
	
	private UsersMiddleLayer usersMid;
	private SessionMiddleLayer sessionMid;
	private String appId;

	@Context
	UriInfo uriInfo;

	
	public AccountResource(String appId) {
		this.usersMid = MiddleLayerFactory.getUsersMiddleLayer();
		this.appId = appId;
		this.sessionMid = MiddleLayerFactory.getSessionMiddleLayer();
	}
	
	// *** CREATE *** ///

	/**
	 * Creates a user in the application, necessary fields: "password";
	 * and "email". signin the user creating a session
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@Path("/signup")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createUserAndLogin(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
			String email = null;
			String userName = null;
			String password = null;
			String userFile = null;
			String userId = null;
			Boolean readOk = false;
			try {
				userName = (String) inputJsonObj.opt("userName");
				userFile = (String) inputJsonObj.opt("userFile");
				email = (String) inputJsonObj.get("email");
				password = (String) inputJsonObj.get("password");
				readOk = true;
			} catch (JSONException e) {
				System.out.println("Error parsing the JSON file.");
				e.printStackTrace();
			}
			if (userName == null) {
				userName = email;
			}
			if (readOk && MiddleLayerFactory.getAppsMiddleLayer().appExists(appId)) {
				if (!usersMid.userExistsInApp(appId, userId, email)) {
					if (uriInfo == null) uriInfo=ui;
					UserInterface outUser = usersMid.createUserAndLogin(headerParams, ui, userId, userName, email, password, userFile);
					
					response = Response.status(Status.CREATED).entity(outUser).build();
				} else {
					String foundUserId = usersMid.getUserIdUsingUserName(appId,userName);
					// 302 = found
					response = Response.status(302).entity(foundUserId).build();
				}
			} else {
				response = Response.status(Status.BAD_REQUEST).entity(userName).build();
			}
		return response;
	}

	/**
	 * Creates a user session and returns de session Identifier (generated by
	 * the server). Required fields: "email", "password".
	 * 
	 * @param req
	 * @param inputJsonObj
	 * @return
	 */
	@Path("/signin")
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces(MediaType.APPLICATION_JSON)
	public Response createSession(@Context HttpServletRequest req, JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		String email = null; // user inserted fields
		String attemptedPassword = null; // user inserted fields
		Response response = null;
		UserInterface outUser = new User();
		List<String> locationList = null;
		List<String> userAgentList = null;
		String userAgent = null;
		String location = null;
		Boolean refreshCode = false;
		try {
			email = (String) inputJsonObj.get("email");
			attemptedPassword = (String) inputJsonObj.get("password");
		} catch (JSONException e) {
			System.out.println("Error Reading the jsonFile");
			return Response.status(Status.BAD_REQUEST).entity("Error reading JSON").build();
		}
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
		//// String email = usersMid.getEmailUsingUserName(appId, userName);
		if(email == null && attemptedPassword == null)
			return Response.status(Status.BAD_REQUEST).entity("Error reading JSON").build();
		String userId = usersMid.getUserIdUsingEmail(appId, email);
		if (userId != null) {
			boolean usersConfirmedOption = usersMid.confirmUsersEmailOption(appId);
			// Remember the order of evaluation in java
			if (usersConfirmedOption) {
				if (usersMid.userEmailIsConfirmed(appId, userId)) {
					String sessionToken = Utils.getRandomString(Const.IDLENGTH);
					boolean validation = sessionMid.createSession(sessionToken, appId, userId, attemptedPassword);
					sessionMid.refreshSession(sessionToken, location, userAgent);
					refreshCode = true;
					if (validation && refreshCode) {
						outUser.setUserID2(userId);
						outUser.setReturnToken(sessionToken);
						response = Response.status(Status.OK).entity(outUser).build();
					}
				} else {
					response = Response.status(Status.FORBIDDEN).entity(Const.EMAIL_CONFIRMATION_ERROR).build();
				}
			} else{
				System.out.println("userId of email: " + email + " is: " + userId);
				String sessionToken = Utils.getRandomString(Const.IDLENGTH);
				boolean validation = sessionMid.createSession(sessionToken, appId, userId, attemptedPassword);
				if(validation){
					sessionMid.refreshSession(sessionToken, location, userAgent);
					refreshCode = true;
					if (validation && refreshCode) {
						outUser.setUserID2(userId);
						outUser.setReturnToken(sessionToken);
						response = Response.status(Status.OK).entity(outUser).build();
					}
				}else
					response = Response.status(Status.UNAUTHORIZED).entity("").build();				
			}
		} else
			response = Response.status(Status.NOT_FOUND).entity("").build();
		return response;

	}

	
 	// *** UPDATE *** ///
	
	
	// *** DELETE *** ///
	
	
	// *** GET *** ///
	
	
	// *** OTHERS *** ///

	/**
	 * Deletes a session (signout).
	 * 
	 * @param sessionToken
	 * @return
	 */
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	@POST
	@Path("/signout/{sessionToken}")
	public Response deleteSession(JSONObject inputJsonObj, @PathParam("sessionToken") String sessionToken) {
		Response response = null;
		Boolean flagAll = (Boolean) inputJsonObj.opt("all");
		String userId = sessionMid.getUserUsingSessionToken(sessionToken);
		if(userId!=null){
			if (sessionMid.sessionTokenExists(sessionToken)) {
				if(!flagAll){
					//deletes the sessions user with the token = sessionToken
					if (sessionMid.deleteUserSession(sessionToken, userId))
						response = Response.status(Status.OK).entity(sessionToken).build();
					else
						response = Response.status(Status.NOT_FOUND).entity(sessionToken).build();
				}else{
					//deletes all sessions user
					System.out.println("************************************");
					System.out.println("********DELETING ALL SESSIONS FOR THIS USER");
					boolean sucess = sessionMid.deleteAllUserSessions(userId);
					if (sucess)
						response = Response.status(Status.OK).entity(userId).build();
					else
						response = Response.status(Status.NOT_FOUND).entity("No sessions exist").build();
					} 
				}
			}
			else 
				response = Response.status(Status.FORBIDDEN).entity("").build();		
		return response;
	}
	
	@POST
	@Path("/recovery")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response makeRecoveryRequest(JSONObject inputJson, @Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = "location") String location){
		Response response = null;
		String email = null;
		String newPass = Utils.getRandomString(Const.PASSWORD_LENGTH);
		byte[] salt = null;
		byte[] hash = null;
			try {
				email = (String) inputJson.get("email");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			PasswordEncryptionService service = new PasswordEncryptionService();
			try {
				salt = service.generateSalt();
				hash = service.getEncryptedPassword(newPass, salt);
			} catch (NoSuchAlgorithmException e) {
				System.out.println("Hashing Algorithm failed, please review the PasswordEncryptionService.");
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				System.out.println("Invalid Key.");
				e.printStackTrace();
			}
			String userId = usersMid.getUserIdUsingEmail(appId, email);
			boolean opOk = usersMid.recoverUser(appId, userId, email, ui, newPass,hash,salt);
			
			if(opOk)
				response = Response.status(Status.OK)
				.entity("Email sent with recovery details.").build();
			else
				response = Response.status(Status.BAD_REQUEST)
				.entity("Wrong email.").build();
		return response;
		
	}

	/**
	 * Gets the session fields associated with the token.
	 * 
	 * @param sessionToken
	 * @return
	 */
	@GET
	@Path("/sessions/{sessionToken}")
	public Response getSessionFields(
			@PathParam("sessionToken") String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			String userId = sessionMid.getUserUsingSessionToken(sessionToken);
			response = Response.status(Status.OK).entity(userId).build();
		} else
			response = Response.status(Status.NOT_FOUND).entity(sessionToken).build();
		return response;
	}

	@PATCH
	@Path("/sessions/{sessionToken}")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response patchSession(
			@HeaderParam("user-agent") String userAgent,
			@HeaderParam("location") String location,
			@PathParam("sessionToken") String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			String userId = sessionMid.getUserUsingSessionToken(sessionToken);
			if (sessionMid.sessionExistsForUser(userId)) {
				if (location != null) {
					sessionMid.refreshSession(sessionToken, location, userAgent);
					response = Response.status(Status.OK).entity("").build();
				} // if the device does not have the gps turned on we should not
					// refresh the session.
					// only refresh it when an action is performed.
			}
			Response.status(Status.NOT_FOUND).entity(sessionToken).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity("You do not have permission to access.").build();
		return response;
	}

}
