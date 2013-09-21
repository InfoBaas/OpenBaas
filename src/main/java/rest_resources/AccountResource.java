package rest_resources;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
import rest_Models.DefaultUser;
import rest_Models.PasswordEncryptionService;
import rest_resources.AppsResource.PATCH;

public class AccountResource {

	
	private static final int IDGENERATOR = 3;
	private AppsMiddleLayer appsMid;
	private String appId;
	private String userId;
	private static final String EMAILCONFIRMATIONERROR = "Please confirm your email first.";
	private static final int IDLENGTH = 3;

	@Context
	UriInfo uriInfo;
	
	public AccountResource(AppsMiddleLayer appsMid, String appId, String userId) {
		this.appsMid = appsMid;
		this.appId = appId;
		this.userId = userId;
	}
	
	public AccountResource(AppsMiddleLayer appsMid, String appId) {
		this.appsMid = appsMid;
		this.appId = appId;
	}
	
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
	public Response createUserAndLogin(JSONObject inputJsonObj, @Context UriInfo ui,@Context HttpHeaders hh) {
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
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
			User outUser = new DefaultUser();
			List<String> userAgent = null;
			List<String> location = null;
			Boolean readOk = false;
			Boolean refreshCode = false;
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
					if (!appsMid.confirmUsersEmailOption(this.appId)) {
						appsMid.createUser(this.appId, userId,	userName, email, salt, hash, userFile);
						String sessionToken = getRandomString(IDGENERATOR);
						boolean validation = appsMid.createSession(sessionToken, appId, userId, password);
						for (Entry<String, List<String>> entry : headerParams.entrySet()) {
							if (entry.getKey().equalsIgnoreCase("location"))
								location = entry.getValue();
							else if (entry.getKey().equalsIgnoreCase("user-agent")){
								userAgent = entry.getValue();
							}	
						}
						if (location != null) {
							appsMid.refreshSession(sessionToken, location.get(0), userAgent.get(0));
							refreshCode = true;
						} else{
							appsMid.refreshSession(sessionToken);
							refreshCode = true;
						}
						if (validation && refreshCode) {
							outUser.setUserID2(userId);
							outUser.setReturnToken(sessionToken);
							response = Response.status(Status.CREATED).entity(outUser).build();
						}
					} else if (appsMid.confirmUsersEmailOption(this.appId)) {
						boolean emailConfirmed = false;
						if(uriInfo==null) 
							uriInfo=ui;
						appsMid.createUserWithEmailConfirmation(this.appId, userId, 
								userName, email, salt,hash, userFile, emailConfirmed, uriInfo);
						outUser.setUserID2(userId);
						response = Response.status(Status.CREATED).entity(outUser).build();
					}
				} else {
					String foundUserId = appsMid.getUserIdUsingUserName(appId,userName);
					// 302 = found
					response = Response.status(302).entity(foundUserId)
							/*.header("content-location",uriInfo.getAbsolutePath() + "/"+ foundUserId)*/.build();
				}
			} else {
				response = Response.status(Status.BAD_REQUEST).entity(userName).build();
			}
			System.out.println("TIME TO FULLFILL REQUEST: "	+ (System.currentTimeMillis() - start));
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
	public Response createSession(@Context HttpServletRequest req,
			JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		String email = null; // user inserted fields
		String attemptedPassword = null; // user inserted fields
		Response response = null;
		User outUser = new DefaultUser();
		List<String> userAgent = null;
		List<String> location = null;
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
				location = entry.getValue();
			else if (entry.getKey().equalsIgnoreCase("user-agent")){
				userAgent = entry.getValue();
			}	
		}
		//// String email = appsMid.getEmailUsingUserName(appId, userName);
		if(email == null && attemptedPassword == null)
			return Response.status(Status.BAD_REQUEST).entity("Error reading JSON").build();
		String userId = appsMid.getUserIdUsingEmail(appId, email);
		if (userId != null) {
			boolean usersConfirmedOption = appsMid.confirmUsersEmailOption(appId);
			// Remember the order of evaluation in java
			if (usersConfirmedOption) {
				if (appsMid.userEmailIsConfirmed(appId, userId)) {
					System.out.println("userId of email: " + email + " is: " + userId);
					String sessionToken = getRandomString(IDGENERATOR);
					boolean validation = appsMid.createSession(sessionToken, appId, userId, attemptedPassword);
					if (location != null) {
						appsMid.refreshSession(sessionToken, location.get(0), userAgent.get(0));
						refreshCode = true;
					} else{
						appsMid.refreshSession(sessionToken);
						refreshCode = true;
					}
					if (validation && refreshCode) {
						outUser.setUserID2(userId);
						outUser.setReturnToken(sessionToken);
						response = Response.status(Status.OK).entity(outUser).build();
					}
				} else {
					response = Response.status(Status.FORBIDDEN).entity(EMAILCONFIRMATIONERROR).build();
				}
			} else
				response = Response.status(Status.UNAUTHORIZED).entity("").build();
		} else
			response = Response.status(Status.NOT_FOUND).entity("").build();
		return response;

	}
	
	
	
	

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
		String userId = appsMid.getUserUsingSessionToken(sessionToken);
		if(userId!=null){
			if (appsMid.sessionTokenExists(sessionToken)) {
				if(!flagAll){
					//deletes the sessions user with the token = sessionToken
					if (appsMid.deleteUserSession(sessionToken, userId))
						response = Response.status(Status.OK).entity(sessionToken).build();
					else
						response = Response.status(Status.NOT_FOUND).entity(sessionToken).build();
				}else{
					//deletes all sessions user
					System.out.println("************************************");
					System.out.println("********DELETING ALL SESSIONS FOR THIS USER");
					boolean sucess = appsMid.deleteAllUserSessions(userId);
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
		if (appsMid.sessionTokenExists(sessionToken)) {
			String userId = appsMid.getUserUsingSessionToken(sessionToken);
			response = Response.status(Status.OK).entity(userId).build();
		} else
			response = Response.status(Status.NOT_FOUND).entity(sessionToken).build();
		return response;
	}

	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}

	
	@PATCH
	@Path("/sessions/{sessionToken}")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response patchSession(
			@HeaderParam("user-agent") String userAgent,
			@HeaderParam("location") String location,
			@PathParam("sessionToken") String sessionToken) {
		Response response = null;
		if (appsMid.sessionTokenExists(sessionToken)) {
			if (appsMid.sessionExistsForUser(userId)) {
				if (location != null) {
					appsMid.refreshSession(sessionToken, location, userAgent);
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
