package infosistema.openbaas.rest;

import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.data.ErrorSet;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.ResultSet;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.rest.AppResource.PATCH;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
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
import javax.ws.rs.WebApplicationException;
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
	
	// *** CREATE *** //

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
			String location = null;
			List<String> locationList = null;
			for (Entry<String, List<String>> entry : headerParams.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(Const.LOCATION))
					locationList = entry.getValue();
			}
			if (locationList != null)
				location = locationList.get(0);
			try {
				userName = (String) inputJsonObj.opt("userName");
				userFile = (String) inputJsonObj.opt("userFile");
				email = (String) inputJsonObj.get("email");
				password = (String) inputJsonObj.get("password");
				readOk = true;
			} catch (JSONException e) {
				Log.error("", this, "createUserAndLogin", "Error parsing the JSON.", e); 
			}
			if (userName == null) {
				userName = email;
			}
			if(!MiddleLayerFactory.getAppsMiddleLayer().appExists(appId))
				return Response.status(Status.NOT_FOUND).entity("{\"App\": "+appId+"}").build();
			if (readOk) {
				if (!usersMid.userExistsInApp(appId, userId, email)) {
					if (uriInfo == null) 
						uriInfo=ui;
					String metaKey = "apps."+appId+".users."+userId;
					Metadata meta = usersMid.createMetadata(metaKey, userId, location);
					User outUser = usersMid.createUserAndLogin(headerParams, ui,appId, userName, email, password, userFile);
					ResultSet res = new ResultSet(outUser, meta);
					response = Response.status(Status.CREATED).entity(res).build();
				} else {
					response = Response.status(Status.FORBIDDEN).entity(new ErrorSet("{\"email exists\": "+email+"}")).build();
				}
			} else {
				response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet("")).build();
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
		User outUser = new User();
		List<String> locationList = null;
		List<String> userAgentList = null;
		String userAgent = null;
		String location = null;
		Boolean refreshCode = false;
		try {
			email = (String) inputJsonObj.get("email");
			attemptedPassword = (String) inputJsonObj.get("password");
		} catch (JSONException e) {
			Log.error("", this, "createSession", "Error parsing the JSON.", e); 
			return Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Error reading JSON")).build();
		}
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.LOCATION))
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
		outUser = usersMid.getUserUsingEmail(appId, email);
		if (outUser.getUserId() != null) {
			boolean usersConfirmedOption = usersMid.getConfirmUsersEmailOption(appId);
			// Remember the order of evaluation in java
			if (usersConfirmedOption) {
				if (usersMid.userEmailIsConfirmed(appId, outUser.getUserId())) {
					String sessionToken = Utils.getRandomString(Const.getIdLength());
					boolean validation = sessionMid.createSession(sessionToken, appId, outUser.getUserId(), attemptedPassword);
					sessionMid.refreshSession(sessionToken, location, userAgent);
					refreshCode = true;
					if (validation && refreshCode) {
						outUser.setUserID(outUser.getUserId());
						outUser.setReturnToken(sessionToken);
						outUser.setUserEmail(email);
						outUser.setUserName(outUser.getUserName());
						outUser.setUserFile(outUser.getUserFile());
						String metaKey = "apps."+appId+".users."+outUser.getUserId();
						Metadata meta = usersMid.createMetadata(metaKey, outUser.getUserId(), location);
						ResultSet res = new ResultSet(outUser, meta);
						response = Response.status(Status.OK).entity(res).build();
					}
				} else {
					response = Response.status(Status.FORBIDDEN).entity(new ErrorSet(Const.getEmailConfirmationError())).build();
				}
			} else{
				Log.debug("", this, "createSession", "userId of email: " + email + " is: " + outUser.getUserId());
				String sessionToken = Utils.getRandomString(Const.getIdLength());
				boolean validation = sessionMid.createSession(sessionToken, appId, outUser.getUserId(), attemptedPassword);
				if(validation){
					sessionMid.refreshSession(sessionToken, location, userAgent);
					refreshCode = true;
					if (validation && refreshCode) {
						outUser.setUserID(outUser.getUserId());
						outUser.setReturnToken(sessionToken);
						outUser.setUserEmail(email);
						outUser.setUserName(outUser.getUserName());
						outUser.setUserFile(outUser.getUserFile());
						String metaKey = "apps."+appId+".users."+outUser.getUserId();
						Metadata meta = usersMid.createMetadata(metaKey, outUser.getUserId(), location);
						ResultSet res = new ResultSet(outUser, meta);
						response = Response.status(Status.OK).entity(res).build();
					}
				}else
					response = Response.status(Status.UNAUTHORIZED).entity(new ErrorSet("")).build();				
			}
		} else
			response = Response.status(Status.NOT_FOUND).entity(new ErrorSet("")).build();
		return response;

	}

	
 	// *** UPDATE *** //
	
	@PATCH
	@Path("/sessions/{sessionToken}")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response patchSession( @HeaderParam("user-agent") String userAgent, @HeaderParam(Const.LOCATION) String location,
			@PathParam(Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
			if (sessionMid.sessionExistsForUser(userId)) {
				if (location != null) {
					String metaKey = "apps."+appId+".users."+userId;
					Metadata meta = usersMid.updateMetadata(metaKey, userId, location);
					ResultSet res = new ResultSet("Refresh OK", meta);
					sessionMid.refreshSession(sessionToken, location, userAgent);					
					response = Response.status(Status.OK).entity(res).build();
				} // if the device does not have the gps turned on we should not
					// refresh the session.
					// only refresh it when an action is performed.
			}
			Response.status(Status.NOT_FOUND).entity(new ErrorSet("SessionToken: "+sessionToken)).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(new ErrorSet("You do not have permission to access.")).build();
		return response;
	}

	
	// *** DELETE *** //
	
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
	public Response deleteSession(JSONObject inputJsonObj, @PathParam(Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		Boolean flagAll = (Boolean) inputJsonObj.opt("all");
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		if(userId!=null){
			if (sessionMid.sessionTokenExists(sessionToken)) {
				if(!flagAll){
					//deletes the sessions user with the token = sessionToken
					if (sessionMid.deleteUserSession(sessionToken, userId)){
						String metaKey = "apps"+appId+"users"+userId;
						Metadata meta = usersMid.updateMetadata(metaKey, userId, null);
						ResultSet res = new ResultSet("Signout OK", meta);
						response = Response.status(Status.OK).entity(res).build();
					}
					else{
						response = Response.status(Status.NOT_FOUND).entity(new ErrorSet("Not found")).build();
					}
				}else{
					//deletes all sessions user
					Log.debug("", this, "deleteSession", "********DELETING ALL SESSIONS FOR THIS USER");
					boolean sucess = sessionMid.deleteAllUserSessions(userId);
					if (sucess){
						String metaKey = "apps."+appId+".users."+userId;
						Metadata meta = usersMid.updateMetadata(metaKey, userId, null);
						ResultSet res = new ResultSet("Signout OK", meta);
						response = Response.status(Status.OK).entity(res).build();
					}
					else
						response = Response.status(Status.NOT_FOUND).entity(new ErrorSet("No sessions exist")).build();
					} 
				}
			}
			else 
				response = Response.status(Status.FORBIDDEN).entity(new ErrorSet("FORBIDDEN")).build();		
		return response;
	}
	
	
	// *** GET LIST *** //
	
	
	// *** GET *** //
	
	/**
	 * Gets the session fields associated with the token.
	 * 
	 * @param sessionToken
	 * @return
	 */
	@GET
	@Path("/sessions/{sessionToken}")
	public Response getUserIdWithSession(
			@PathParam(Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
			
			String metaKey = "apps."+appId+".users."+userId;
			Metadata meta = usersMid.getMetadata(metaKey);
			ResultSet res = new ResultSet("OK", meta);
			response = Response.status(Status.OK).entity(res).build();
		} else
			response = Response.status(Status.NOT_FOUND).entity(new ErrorSet(sessionToken)).build();
		return response;
	}
	
	/**
	 * Gets the session fields associated with the token.
	 * 
	 * @param sessionToken
	 * @return
	 */
	@GET
	@Path("/sessions")
	public Response getSessionFields(
			@PathParam(Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
			User outUser = usersMid.getUserInApp(appId, userId);
			String metaKey = "apps."+appId+".users."+userId;
			Metadata meta = usersMid.getMetadata(metaKey);
			ResultSet res = new ResultSet(outUser, meta);
			response = Response.status(Status.OK).entity(res).build();
		} else
			response = Response.status(Status.NOT_FOUND).entity(new ErrorSet("Token NOT_FOUND")).build();
		return response;
	}

	
	// *** OTHERS *** //

	@POST
	@Path("/recovery")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response makeRecoveryRequest(JSONObject inputJson, @Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = Const.LOCATION) String location){
		Response response = null;
		String email = null;
		String newPass = Utils.getRandomString(Const.getPasswordLength());
		byte[] salt = null;
		byte[] hash = null;
			try {
				email = (String) inputJson.get("email");
			} catch (JSONException e) {
				Log.error("", this, "makeRecoveryRequest", "Error parsing the JSON.", e); 
			}
			PasswordEncryptionService service = new PasswordEncryptionService();
			try {
				salt = service.generateSalt();
				hash = service.getEncryptedPassword(newPass, salt);
			} catch (NoSuchAlgorithmException e) {
				Log.error("", this, "makeRecoveryRequest", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
			} catch (InvalidKeySpecException e) {
				Log.error("", this, "makeRecoveryRequest", "Invalid Key.", e); 
			}
			String userId = usersMid.getUserIdUsingEmail(appId, email);
			if(userId==null)
				return Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Wrong email.")).build();
			boolean opOk = usersMid.recoverUser(appId, userId, email, ui, newPass,hash,salt);
			if(opOk){
				ResultSet res = new ResultSet("Email sent with recovery details.", null);
				response = Response.status(Status.OK).entity(res).build();
			}
			else
				response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Wrong email.")).build();
		return response;
		
	}

	
	// *** RESOURCES *** //

	/**
	 * Launches the resource integration requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("integration")
	public IntegrationResource integration() {
		try {
			return new IntegrationResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "integration", "Illegal Argument.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Parse error")).build());
		}
	}
}
