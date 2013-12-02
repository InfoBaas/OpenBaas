package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


public class AppResource {

	private AppsMiddleLayer appsMid;
	private static final int IDLENGTH = 3;

	public AppResource() {
		appsMid = AppsMiddleLayer.getInstance();
	}

	@Context
	UriInfo uriInfo;

	// *** CREATE *** //

	/**
	 * Create application, fields necessary are: "appName". An unique identifier
	 * is generated and returned in the reply.
	 * 
	 * This is the time to define the optional parameters of the app such as if the developer wants to confirm the users
	 * email before allowing them to perform operations, and other relevant options.
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createApp(JSONObject inputJsonObj,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			long start = System.currentTimeMillis();
			Application temp = null;
			String appName = null;
			boolean confirmUsersEmail;
			boolean AWS;
			boolean FTP;
			boolean FileSystem;
			String appId = null;
			String appKey = null;
			boolean readSucess = false, created = false;
			try {
				appName = (String) inputJsonObj.get("appName");
				confirmUsersEmail = (boolean) inputJsonObj.optBoolean("confirmUsersEmail", false);
				AWS = (boolean) inputJsonObj.optBoolean("aws", false);
				FTP = (boolean) inputJsonObj.optBoolean("ftp", false);
				if(!AWS && !FTP)
					FileSystem = (boolean) inputJsonObj.optBoolean("filesystem", true);
				else 
					FileSystem = (boolean) inputJsonObj.optBoolean("filesystem", false);
				if(!AWS && !FTP && !FileSystem)
					FileSystem = true;
				readSucess = true;
			} catch (JSONException e) {
				Log.error("", this, "createApp", "Error parsing the JSON.", e); 
				return Response.status(Status.BAD_REQUEST).entity(new Error("Error parsing the JSON.")).build();
			}
			if (readSucess) {
				appId = Utils.getRandomString(IDLENGTH);
				appKey = Utils.getRandomString(IDLENGTH);
				created = appsMid.createApp(appId, appKey, appName, confirmUsersEmail, AWS, FTP, FileSystem);
			}
			if (created) {
				temp = this.appsMid.getApp(appId);
				Metadata meta = appsMid.createMetadata(appId, null, null, "admin", null, null);
				Result res = new Result(temp, meta);
				response = Response.status(Status.CREATED).entity(res).build();
			} else {
				// 302 is not implemented in the response status, we can create
				// it
				// using Response.StatusType, or simply send the code to the
				// constructor
				// and let the browser interpret it.
				// No time for details = put 302 in the parameter
				response = Response.status(302).entity(new Error("not created")).build();
			}
			Log.debug("", this, "createApp", "TIME TO FULLFILL REQUEST: " + (System.currentTimeMillis() - start));
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
 	// *** UPDATE *** //
	
	/**
	 * Updates the application, optional fields: "newAppName" (new application
	 * name to set), "alive" (true to reactivate a dead app).
	 * 
	 * @param appId
	 * @param inputJsonObj
	 * @return
	 */
	@Path("{appId}")
	@PATCH
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateApp(@PathParam("appId") String appId,	JSONObject inputJsonObj,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			String newAlive = null;
			String newAppName = null;
			Application temp = null;
			Boolean newAWS;
			Boolean newFTP;
			Boolean newFileSystem;
			Boolean newConfirmUsersEmail;
			newAlive = inputJsonObj.optString("alive"); //mudar para boolean
			newAppName = inputJsonObj.optString("appName");
			newConfirmUsersEmail = (boolean) inputJsonObj.optBoolean("confirmUsersEmail", false);
			newAWS = (Boolean) inputJsonObj.opt("aws");
			newFTP = (Boolean) inputJsonObj.opt("ftp");
			newFileSystem = (Boolean) inputJsonObj.opt("filesystem");
			Application app = this.appsMid.getApp(appId);
			
			
			if(newAlive.equals(""))
				newAlive = app.getAlive();
			if(newAppName.equals(""))
				newAppName = app.getAppName();
			if(newConfirmUsersEmail.equals(""))
				newConfirmUsersEmail = app.getConfirmUsersEmail();
			
			if(newAWS==null)
				newAWS = app.getAWS();
			if(newFTP==null)
				newFTP = app.getFTP();
			if(newFileSystem==null)
				newFileSystem = app.getFileSystem();
			
			if(!newAWS && !newFTP && !newFileSystem)
				newFileSystem = true;
			
			
			if (this.appsMid.appExists(appId)) {
				temp = this.appsMid.updateAllAppFields(appId, newAlive, newAppName,newConfirmUsersEmail,newAWS,newFTP,newFileSystem);
				Metadata meta = appsMid.updateMetadata(appId, null, null, "admin", null, null);
				Result res = new Result(temp, meta);
				response = Response.status(Status.OK).entity(res).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** DELETE *** //
	
	/**
	 * Delete application using its application Identifier.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteApp(@PathParam("appId") String appId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			Log.debug("", this, "deleteApp", "*Deleting App (setting as inactive)*");
			if (this.appsMid.removeApp(appId)) {
				Log.debug("", this, "deleteApp", "******Deletion OK*******");
				Boolean meta = appsMid.deleteMetadata(appId, null, null, null);
				if(meta)
					response = Response.status(Status.OK).entity("").build();
				else
					response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Del Meta")).build();
				
				response = Response.status(Status.OK).entity(appId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** GET LIST *** //

	//XPTO: Não faz sentido isto
	/**
	 * Get all application Identifiers.
	 * 
	 * @param req
	 * @return
	 */
	/*
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllApplicationIds(@Context HttpServletRequest req, @Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam(Const.PAGE_NUMBER) Integer pageNumber, @QueryParam(Const.PAGE_SIZE) Integer pageSize, 
			@QueryParam(Const.ORDER_BY) String orderBy, @QueryParam(Const.ORDER_BY) String orderType ) {
		if (pageNumber == null) pageNumber = Const.getPageNumber();
		if (pageSize == null) 	pageSize = Const.getPageSize();
		if (orderBy == null) 	orderBy = Const.getOrderBy();
		if (orderType == null) 	orderType = Const.getOrderType();
		Response response = null;
		// Parameters treatment
		int code = Utils.treatParametersAdmin(ui, hh);
		if(code == 1){
		JSONObject temp = new JSONObject();
		ArrayList<String> ids = new ArrayList<String>();
		try {
			ids = appsMid.getAllAppIds(pageNumber, pageSize, orderBy, orderType);
			Iterator<String> it = ids.iterator();
			while (it.hasNext()) {
				temp.accumulate("appId", it.next());
			}
		} catch (JSONException e) {
			Log.error("", this, "findAllApplicationIds", "Error parsing the JSON.", e); 
		}
		ListResult res = new ListResult(ids,pageNumber);
		response = Response.status(Status.OK).entity(res).build();
		 } else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}
*/
	
	// *** GET *** //
	
	/**
	 * Get Application Information using its Application Identifier.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findById(@PathParam("appId") String appId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			Log.debug("", this, "findById", "********Finding App info************");
			Application temp = appsMid.getApp(appId);
			if (temp == null)
				return Response.status(Status.NOT_FOUND).entity(new Error("App not exist")).build();
			
			Metadata meta = appsMid.updateMetadata(appId, null, null, "admin", null, null);
			Result res = new Result(temp, meta);
			response = Response.status(Status.OK).entity(res).build();
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** OTHERS *** //
	
	// *** RESOURCES *** //

	// This is needed to use patch methods, don't delete it!
	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@HttpMethod("PATCH")
	public @interface PATCH {
	}

	/**
	 * Launches the resource to handle /users requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/users")
	public UsersResource users(@PathParam("appId") String appId) {
		try {
			return new UsersResource(uriInfo, appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "users", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /media requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/media")
	public MediaResource media(@PathParam("appId") String appId) {
		try {
			return new MediaResource(appId) ;
		} catch (IllegalArgumentException e) {
			Log.error("", this, "media", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /data requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/data")
	public AppDataResource data(@PathParam("appId") String appId) {
		try {
			return new AppDataResource(uriInfo, appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "data", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /media/audio requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/media/audio")
	public AudioResource audio(@PathParam("appId") String appId) {
		try {
			return new AudioResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "audio", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /media/video requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/media/video")
	public VideoResource video(@PathParam("appId") String appId) {
		try {
			return new VideoResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "video", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle /media/images requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/media/images")
	public ImageResource image(@PathParam("appId") String appId) {
		try {
			return new ImageResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "image", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	/**
	 * Launches the resource to handle {appID}/storage requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/storage")
	public StorageResource storage(@PathParam("appId") String appId) {
		try {
			return new StorageResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "storage", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}
	
	/**
	 * Launches the resource to handle {appID}/account requests.
	 * 
	 * @param appId
	 * @return
	 */
	@Path("{appId}/account")
	public AccountResource account(@PathParam("appId") String appId) {
		try {
			return new AccountResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "acount", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

}