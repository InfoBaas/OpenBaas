package rest_resources;

import java.util.ArrayList;
import java.util.List;

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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.IdsResultSet;
import utils.Const;
import utils.Utils;

public class UserDataResource {

	private static final Utils utils = new Utils();
	private AppsMiddleLayer appsMid;
	private String appId;
	private String userId;
	@Context
	UriInfo uriInfo;

	public UserDataResource(UriInfo uriInfo, AppsMiddleLayer appsMidLayer,
			String appId, String userId) {
		this.appsMid = appsMidLayer;
		this.appId = appId;
		this.uriInfo = uriInfo;
		this.userId = userId;
	}

	

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
	public Response createOrReplaceDocument(JSONObject inputJsonObj,
			@PathParam("pathId") List<PathSegment> path, @Context UriInfo ui,
			@Context HttpHeaders hh,
			@HeaderParam(value = "location") String location) {
		Response response = null;
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			JSONObject data = null;
			try {
				data = (JSONObject) inputJsonObj.get("data");
			} catch (JSONException e) {
				System.out.println("Error parsing the JSON file.");
				e.printStackTrace();
			}
			if (appsMid.appExists(appId)) {
				String url = appsMid.createUserDocPathFromListWithSlashes(
						appId, userId, path);
				if (appsMid.insertIntoUserDocument(appId, userId, url, data,
						location))
					response = Response.status(Status.CREATED).entity(appId)
							.build();
				else
					response = Response.status(Status.BAD_REQUEST).entity(data)
							.build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId)
						.build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN)
					.entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST)
					.entity("Error handling the request.").build();
		return response;
	}

	//TODO: PAGINATION
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllElementsInDocument(
			@Context UriInfo ui,
			@Context HttpHeaders hh, @QueryParam("latitude") String latitude,
			@QueryParam("longitude") String longitude,
			@QueryParam("radius") String radius,
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			//query parameters are present, only return the elements 
			if (latitude != null && longitude != null && radius != null) {
				ArrayList<String> all = appsMid.getAllUserDocsInRadius(appId, userId, Double.parseDouble(latitude), 
						Double.parseDouble(longitude), Double.parseDouble(radius),pageNumber,pageSize,orderBy,orderType);
				IdsResultSet res = new IdsResultSet(all,pageNumber);
				response = Response.status(Status.OK).entity(res).build();
			//no query parameters return all docs
			} else {
				String all = appsMid.getAllUserDocs(appId, userId);
				response = Response.status(Status.OK).entity(all).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN)
					.entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST)
					.entity("Error handling the request.").build();
		return response;
	}

	/**
	 * Retrieves the data contained in a key.
	 * 
	 * @param path
	 * @return
	 */
	@GET
	@Path("/{pathId:.+}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getElementInDocument(
			@PathParam("pathId") List<PathSegment> path, @Context UriInfo ui,
			@Context HttpHeaders hh) {
		Response response = null;
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			if (appsMid.dataExistsForElement(appId, path)) {
				String data = appsMid.getElementInUserDocument(appId, userId,
						path);
				if (data == null)
					response = Response.status(Status.BAD_REQUEST)
							.entity(appId).build();
				else
					response = Response.status(Status.OK).entity(data).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId)
						.build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN)
					.entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST)
					.entity("Error handling the request.").build();
		return response;
	}

	/**
	 * Creates the document root, this is treated differently than PUT to
	 * 
	 * @Path("/{pathId:.+ ") due to the Rest resources levels.
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createDocumentRoot(JSONObject inputJsonObj,
			@Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = "location") String location) {
		Response response = null;
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			JSONObject data = null;
			try {
				data = (JSONObject) inputJsonObj.get("data");
			} catch (JSONException e) {
				System.out.println("Error parsing the JSON file.");
				e.printStackTrace();
			}
			if (appsMid.appExists(appId) && appsMid.userExistsInApp(appId, userId)) {
				if (appsMid.insertUserDocumentRoot(appId, userId, data,
						location)) {
					response = Response.status(Status.CREATED).entity(appId)
							.build();
				} else {
					response = Response.status(Status.BAD_REQUEST).entity(data)
							.build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId)
						.build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN)
					.entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST)
					.entity("Error handling the request.").build();
		return response;
	}

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
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			if (appsMid.dataExistsForUserElement(appId, userId, path)) {
				if (appsMid.deleteUserDataInElement(appId, userId, path))
					response = Response.status(Status.OK).entity("").build();
				else
					response = Response.status(Status.BAD_REQUEST).entity(path)
							.build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId)
						.build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN)
					.entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST)
					.entity("Error handling the request.").build();
		return response;
	}

	@POST
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createNonPublishableUserDocument(JSONObject inputJsonObj,
			@PathParam("pathId") List<PathSegment> path, @Context UriInfo ui,
			@Context HttpHeaders hh,
			@HeaderParam(value = "location") String location) {
		Response response = null;
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			JSONObject data = null;
			try {
				data = (JSONObject) inputJsonObj.get("data");
			} catch (JSONException e) {
				System.out.println("Error parsing the JSON file.");
				e.printStackTrace();
			}
			if (appsMid.appExists(appId)) {
				String url = appsMid.createUserDocPathFromListWithSlashes(
						appId, userId, path);
				if (appsMid.createNonPublishableUserDocument(appId, userId,
						data, url, location))
					response = Response.status(Status.OK).entity(appId).build();
				else
					response = Response.status(Status.BAD_REQUEST)
							.entity(appId).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId)
						.build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN)
					.entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST)
					.entity("Error handling the request.").build();
		return response;
	}
}
