package rest_resources;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import modelInterfaces.Application;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.IdsResultSet;
import utils.Const;
import utils.Utils;


public class AppsResource {

	private AppsMiddleLayer appsMid;
	private static final int IDLENGTH = 3;
	private static final Utils utils = new Utils();

	public AppsResource() {
		appsMid = new AppsMiddleLayer();
	}

	@Context
	UriInfo uriInfo;

	//TODO: PAGINATION
	/**
	 * Get all application Identifiers.
	 * 
	 * @param req
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllApplicationIds(@Context HttpServletRequest req,
			@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		// Parameters treatment
		int code = utils.treatParameters(ui, hh);
		if(code == 1){
		System.out.println("***********************************");
		System.out.println("********Finding all apps***********");
		JSONObject temp = new JSONObject();
		ArrayList<String> ids = new ArrayList<String>();
		try {
			ids = appsMid.getAllAppIds(pageNumber, pageSize, orderBy, orderType);
			Iterator<String> it = ids.iterator();
			while (it.hasNext()) {
				temp.accumulate("appId", it.next());
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IdsResultSet res = new IdsResultSet(ids,pageNumber);
		response = Response.status(Status.OK).entity(res).build();
		 } else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

	

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
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("********Finding App info************");
			Application temp = appsMid.getApp(appId);
			if (temp == null)
				return Response.status(Status.NOT_FOUND).entity(temp).build();
			response = Response.status(Status.OK).entity(temp).build();
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
		 .build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}

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
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("*Deleting App (setting as inactive)*");
			if (this.appsMid.removeApp(appId)) {
				System.out.println("******Deletion OK*******");
				response = Response.status(Status.OK).entity(appId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

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
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			long start = System.currentTimeMillis();
			System.out.println("************************************");
			System.out.println("***********Creating App*************");
			Application temp = null;
			String appName = null;
			boolean confirmUsersEmail;
			String appId = null;
			boolean readSucess = false, created = false;
			try {
				appName = (String) inputJsonObj.get("appName");
				confirmUsersEmail = (boolean) inputJsonObj.optBoolean("confirmUsersEmail", false);
				readSucess = true;
			} catch (JSONException e) {
				System.out.println("Error reading json input.");
				e.printStackTrace();
				return Response.status(Status.BAD_REQUEST).entity(temp).build();
			}
			if (readSucess) {
				appId = utils.getRandomString(IDLENGTH);
				created = appsMid.createApp(appId, appName, confirmUsersEmail);
			}
			if (created) {
				temp = this.appsMid.getApp(appId);
				boolean awsOkay = this.appsMid.createAppAWS(appId);
				if (awsOkay) {
					// System.out.println("entrei");
					appsMid.createDocumentForApplication(appId);
					response = Response.status(Status.CREATED).entity(temp)
							.build();
					System.out.println("***********App Created**************");
					System.out.println("************************************");
				}
			} else {
				// 302 is not implemented in the response status, we can create
				// it
				// using Response.StatusType, or simply send the code to the
				// constructor
				// and let the browser interpret it.
				// No time for details = put 302 in the parameter
				response = Response.status(302).entity(temp).build();
			}
			System.out.println("TIME TO FULLFILL REQUEST: "
					+ (System.currentTimeMillis() - start));
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
		 .build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}

	// This is needed to use patch methods, don't delete it!
	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@HttpMethod("PATCH")
	public @interface PATCH {
	}

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
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			String alive = null;
			String newAppName = null;
			Boolean confirmUsersEmail;
			alive = inputJsonObj.optString("alive"); //mudar para boolean
			newAppName = inputJsonObj.optString("newAppName");
			confirmUsersEmail = (boolean) inputJsonObj.optBoolean("confirmUsersEmail", false);
			if (this.appsMid.appExists(appId)) {
				if (alive != null && newAppName != null && confirmUsersEmail != null) {
					this.appsMid.updateAllAppFields(appId, alive, newAppName,confirmUsersEmail);
					response = Response.status(Status.OK).entity(appId).build();
				} else if (alive != null)
					this.appsMid.reviveApp(appId);
				else if (newAppName != null)
					this.appsMid.updateAppName(appId, newAppName);
				else if(confirmUsersEmail != null)
					this.appsMid.updateConfirmUsersEmailOption(appId, confirmUsersEmail);
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			}
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
		 .build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
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
			return new UsersResource(uriInfo, appsMid, appId);
		} catch (IllegalArgumentException e) {
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
			return new MediaResource(appsMid, appId);
		} catch (IllegalArgumentException e) {
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
	public DataResource data(@PathParam("appId") String appId) {
		try {
			return new DataResource(uriInfo, appsMid, appId);
		} catch (IllegalArgumentException e) {
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
			return new AudioResource(appsMid, appId);
		} catch (IllegalArgumentException e) {
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
			return new VideoResource(appsMid, appId);
		} catch (IllegalArgumentException e) {
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
			return new ImageResource(appsMid, appId);
		} catch (IllegalArgumentException e) {
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
			return new StorageResource(appsMid, appId);
		} catch (IllegalArgumentException e) {
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
			return new AccountResource(appsMid, appId);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}
}