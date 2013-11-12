package infosistema.openbaas.rest;

import infosistema.openbaas.data.ErrorSet;
import infosistema.openbaas.data.ListResultSet;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.ResultSet;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.DocumentMiddleLayer;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class UserDataResource {

	@Context
	UriInfo uriInfo;
	private AppsMiddleLayer appsMid;
	private DocumentMiddleLayer docMid;
	private UsersMiddleLayer usersMid;
	private String appId;
	private String userId;
	private SessionMiddleLayer sessionsMid;

	public UserDataResource(UriInfo uriInfo, String appId, String userId) {
		this.appsMid = MiddleLayerFactory.getAppsMiddleLayer();
		this.usersMid = MiddleLayerFactory.getUsersMiddleLayer();
		this.docMid = MiddleLayerFactory.getDocumentMiddleLayer();
		this.sessionsMid = MiddleLayerFactory.getSessionMiddleLayer();
		this.appId = appId;
		this.uriInfo = uriInfo;
		this.userId = userId;
	}

	// *** CREATE *** //
	
	//TODO: LOCATION (documents)
	/**
	 * Creates the document root, this is treated differently than PUT to
	 * 
	 * @Path("/{pathId:.+ ") due to the Rest resources levels.
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createDocumentRoot(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh, @HeaderParam(value = Const.LOCATION) String location) {
		Response response = null;
		Cookie sessionToken=null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.SESSION_TOKEN))
				sessionToken = new Cookie(Const.SESSION_TOKEN, entry.getValue().get(0));
		}
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			JSONObject data = null;
			try {
				data = (JSONObject) inputJsonObj.get("data");
			} catch (JSONException e) {
				Log.error("", this, "createDocumentRoot", "Error parsing the JSON.", e); 
			}
			if (appsMid.appExists(appId) && usersMid.userExistsInApp(appId, userId)) {
				if (docMid.insertDocumentInPath(appId, userId, null, data, location)) {
					
					String metaKey = "apps."+appId+".users."+userId;
					String userId = sessionsMid.getUserIdUsingSessionToken(sessionToken.getValue());
					Metadata meta = docMid.createMetadata(metaKey, userId, location);
					ResultSet res = new ResultSet(inputJsonObj, meta);		
					
					response = Response.status(Status.CREATED).entity(res).build();
				} else {
					response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet(data.toString())).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new ErrorSet(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new ErrorSet("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Error handling the request.")).build();
		return response;
	}

	
	// *** UPDATE *** //
	
	//TODO: LOCATION (location de defeito???)
	/**
	 * Create or replace existing elements.
	 * 
	 * @param inputJsonObj
	 * @param path
	 * @return
	 */
	@PUT
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrReplaceDocument(JSONObject inputJsonObj, @PathParam("pathId") List<PathSegment> path,
			@Context UriInfo ui, @Context HttpHeaders hh, @HeaderParam(value = Const.LOCATION) String location) {
		Response response = null;
		Cookie sessionToken=null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.SESSION_TOKEN))
				sessionToken = new Cookie(Const.SESSION_TOKEN, entry.getValue().get(0));
		}
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			JSONObject data = null;
			try {
				data = (JSONObject) inputJsonObj.get("data");
			} catch (JSONException e) {
				Log.error("", this, "createOrReplaceDocument", "Error parsing the JSON.", e); 
			}
			if (appsMid.appExists(appId)) {
				if (docMid.updateDocumentInPath(appId, userId, path, data, location)){
					String metaKey = "apps."+appId+".users."+userId+path;
					String userId = sessionsMid.getUserIdUsingSessionToken(sessionToken.getValue());
					Metadata meta = docMid.updateMetadata(metaKey, userId, location);
					ResultSet res = new ResultSet(inputJsonObj, meta);		
					
					response = Response.status(Status.CREATED).entity(res).build();
				}
				else
					response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet(data.toString())).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new ErrorSet(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new ErrorSet("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Error handling the request.")).build();
		return response;
	}

	// *** DELETE *** //
	
	/**
	 * Removes an element and the childs of that element if they exist.
	 * 
	 * @param path
	 * @return
	 */
	@DELETE
	@Path("/{pathId:.+}")
	public Response deleteDataInElement(
			@PathParam("pathId") List<PathSegment> path, @Context UriInfo ui,
			@Context HttpHeaders hh) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if (docMid.existsDocumentInPath(appId, userId, path)) {
				if (docMid.deleteDocumentInPath(appId, userId, path)){
					String metaKey = "apps."+appId+".data."+path;
					Boolean meta = docMid.deleteMetadata(metaKey);
					if(meta)
						response = Response.status(Status.OK).entity("").build();
					else
						response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorSet("Del Meta")).build();
				}
				else
					response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet(path.toString())).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new ErrorSet(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new ErrorSet("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Error handling the request.")).build();
		return response;
	}


	// *** GET LIST *** //
	
	//XPTO: Refazer isto tudo
	//TODO: LOCATION (documents)
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllUserData(@Context UriInfo ui, @Context HttpHeaders hh, 
			@QueryParam("latitude") String latitude, @QueryParam("longitude") String longitude, @QueryParam("radius") String radius,
			@QueryParam(Const.PAGE_NUMBER) Integer pageNumber, @QueryParam(Const.PAGE_SIZE) Integer pageSize, 
			@QueryParam(Const.ORDER_BY) String orderBy, @QueryParam(Const.ORDER_BY) String orderType ) {
		if (pageNumber == null) pageNumber = Const.getPageNumber();
		if (pageSize == null) 	pageSize = Const.getPageSize();
		if (orderBy == null) 	orderBy = Const.getOrderBy();
		if (orderType == null) 	orderType = Const.getOrderType();
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			//query parameters are present, only return the elements 
			if (latitude != null && longitude != null && radius != null) {
				ArrayList<String> all = null; 
						//docMid.getAllUserDocsInRadius(appId, userId, Double.parseDouble(latitude), 
						//Double.parseDouble(longitude), Double.parseDouble(radius),pageNumber,pageSize,orderBy,orderType);
				ListResultSet res = new ListResultSet(all,pageNumber);
				response = Response.status(Status.OK).entity(res).build();
			//no query parameters return all docs
			} else {
				String all = null; //docMid.getAllUserDocs(appId, userId);
				response = Response.status(Status.OK).entity(all).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new ErrorSet("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Error handling the request.")).build();
		return response;
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
	@Produces(MediaType.APPLICATION_JSON)
	public Response getElementInDocument(@PathParam("pathId") List<PathSegment> path, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if (docMid.existsDocumentInPath(appId, userId, path)) {
				String data = docMid.getDocumentInPath(appId, userId, path);
				if (data == null)
					response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet(appId)).build();
				else{
					String metaKey = "apps."+appId+".users."+userId+path;
					Metadata meta = docMid.getMetadata(metaKey);
					ResultSet res = new ResultSet(data, meta);
					response = Response.status(Status.OK).entity(res).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new ErrorSet(appId)).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new ErrorSet("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new ErrorSet("Error handling the request.")).build();
		return response;
	}


	// *** RESOURCES *** //

	
	// *** OTHERS *** //
	
}
