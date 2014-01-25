package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.DocumentMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.rest.AppResource.PATCH;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.util.Iterator;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

public class UserDataResource {

	@Context
	UriInfo uriInfo;
	private AppsMiddleLayer appsMid;
	private DocumentMiddleLayer docMid;
	private UsersMiddleLayer usersMid;
	private String appId;
	private SessionMiddleLayer sessionMid;

	public UserDataResource(UriInfo uriInfo, String appId, String userId) {
		this.appsMid = AppsMiddleLayer.getInstance();
		this.usersMid = UsersMiddleLayer.getInstance();
		this.docMid = DocumentMiddleLayer.getInstance();
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.appId = appId;
		this.uriInfo = uriInfo;
	}

	// *** CREATE *** //
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createDocumentRoot(JSONObject inputJson, @Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = Const.LOCATION) String location) {
		return createOrReplaceDocument(inputJson, null, ui, hh, location);
	}

	@PUT
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrReplaceDocument(JSONObject inputJson,  @PathParam("pathId") List<PathSegment> path,
			@Context UriInfo ui, @Context HttpHeaders hh, @HeaderParam(value = Const.LOCATION) String location) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String sessionToken = Utils.getSessionToken(hh);
			String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
			if (!sessionMid.checkAppForToken(sessionToken, appId))
				return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
			if (appsMid.appExists(appId) && usersMid.userIdExists(appId, userId)) {
				if (docMid.insertDocumentInPath(appId, userId, path, inputJson, location)) {
					Metadata meta = null;
					Iterator<?> it = inputJson.keys();
					while (it.hasNext()) { 
						String key = it.next().toString();
						meta = docMid.createMetadata(appId, userId, key, userId, location, inputJson);
					}
					Result res = new Result(inputJson.toString(), meta);		
					response = Response.status(Status.OK).entity(res).build();
				} else {
					response = Response.status(Status.BAD_REQUEST).entity(new Error(inputJson.toString())).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response patchDataInRoot(JSONObject inputJson, 
			@Context UriInfo ui, @Context HttpHeaders hh, @HeaderParam(value = Const.LOCATION) String location) {
		return patchDataInElement(inputJson, null, ui, hh, location);
	}
	
	@PATCH
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response patchDataInElement(JSONObject inputJson, @PathParam("pathId") List<PathSegment> path,
			@Context UriInfo ui, @Context HttpHeaders hh, @HeaderParam(value = Const.LOCATION) String location) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String sessionToken = Utils.getSessionToken(hh);
			String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
			if (!sessionMid.checkAppForToken(sessionToken, appId))
				return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
			if (docMid.existsDocumentInPath(appId, null, path)) {
				if (docMid.updateDocumentInPath(appId, userId, path, inputJson, location)){
					Metadata meta = docMid.updateMetadata(appId, userId, docMid.convertPathToString(path), userId, location, inputJson);
					Result res = new Result(inputJson.toString(), meta);		
					response = Response.status(Status.OK).entity(res).build();
				}
				else
					response = Response.status(Status.BAD_REQUEST).entity(new Error(appId)).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}
	
	@DELETE
	@Path("/{pathId:.+}")
	public Response deleteDataInElement(@PathParam("pathId") List<PathSegment> path, @Context UriInfo ui,
			@Context HttpHeaders hh) {
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		if (code == 1) {
			if (docMid.existsDocumentInPath(appId, userId, path)) {
				if (docMid.deleteDocumentInPath(appId, userId, path)){
					Boolean meta = docMid.deleteMetadata(appId, userId, docMid.convertPathToString(path), ModelEnum.data);
					if(meta)
						response = Response.status(Status.OK).entity("").build();
					else
						response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Del Meta")).build();
				}
				else
					response = Response.status(Status.BAD_REQUEST).entity(new Error(path.toString())).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
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
		return findDocument(null, ui, hh, query, radiusStr, latitudeStr, longitudeStr, pageNumberStr, pageSizeStr, orderByStr, orderTypeStr);
	}

	// *** GET *** //
	
	/**
	 * Retrieves the data contained in a key.
	 * 
	 * @param path
	 * @return
	 */
	@GET
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response findDocument(@PathParam("pathId") List<PathSegment> path, 
			@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("query") JSONObject query, @QueryParam(Const.RADIUS) String radiusStr,
			@QueryParam(Const.LAT) String latitudeStr, @QueryParam(Const.LONG) String longitudeStr,
			@QueryParam(Const.PAGE_NUMBER) String pageNumberStr, @QueryParam(Const.PAGE_SIZE) String pageSizeStr, 
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_BY) String orderTypeStr) {
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if ((latitudeStr != null && longitudeStr != null && radiusStr != null) || query != null) {
				String url = docMid.getDocumentPath(userId, path);
				QueryParameters qp = QueryParameters.getQueryParameters(appId, userId, query, radiusStr, latitudeStr, longitudeStr, 
						pageNumberStr, pageSizeStr, orderByStr, orderTypeStr, url, ModelEnum.data);
				try {
					ListResult res = docMid.find(qp);
					response = Response.status(Status.OK).entity(res).build();
				} catch (Exception e) {
					Log.error("", this, "findDocument", "Error ocorred", e);
					response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
				}
				return response;
			} else if (docMid.existsDocumentInPath(appId, userId, path)) {
				Object data = docMid.getDocumentInPath(appId, userId, path);
				if (data == null)
					response = Response.status(Status.BAD_REQUEST).entity(new Error(appId)).build();
				else{
					try {
						if (data instanceof JSONObject) data = (DBObject)JSON.parse(data.toString());
						Metadata meta = docMid.getMetadata(appId, userId, docMid.convertPathToString(path), ModelEnum.data);
						Result res = new Result(data, meta);
						response = Response.status(Status.OK).entity(res).build();
					} catch (Exception e) {
						response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
					}	
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}


	// *** GET *** //
	
	// *** OTHERS *** //
	
	// *** RESOURCES *** //

}
