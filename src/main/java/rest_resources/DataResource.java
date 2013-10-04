package rest_resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import resourceModelLayer.AppsMiddleLayer;
import rest_Models.IdsResultSet;
import rest_resources.AppsResource.PATCH;
import utils.Const;

public class DataResource {

	private AppsMiddleLayer appsMid;
	private String appId;
	@Context
	UriInfo uriInfo;

	public DataResource(@Context UriInfo uriInfo, AppsMiddleLayer appsMid, String appId) {
		this.appsMid = appsMid;
		this.appId = appId;
		this.uriInfo = uriInfo;
	}
	/*
	 * Returns a code corresponding to the sucess or failure Codes: 
	 * -2 -> Forbidden 
	 * -1 -> Bad request 
	 * 1 -> sessionExists
	 */
	private int treatParameters(UriInfo ui, HttpHeaders hh) {
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
					if(!appsMid.refreshSession(sessionToken.getValue(),	location.get(0), userAgent.get(0)))
						code = -1;
				} else
					appsMid.refreshSession(sessionToken.getValue());
			} else {
				code = -2;
			}
		}
		return code;
	}

	@POST
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createNonPublishableDocument(JSONObject inputJsonObj,
			@PathParam("pathId") List<PathSegment> path, @Context UriInfo ui,
			@Context HttpHeaders hh, @HeaderParam(value = "location") String location) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			JSONObject data = null;
			try {
				data = (JSONObject) inputJsonObj.get("data");
			} catch (JSONException e) {
				System.out.println("Error parsing the JSON file.");
				e.printStackTrace();
			}
			if (appsMid.appExists(appId)) {
				if (appsMid.createNonPublishableAppDocument(appId, data, path, location))
					response = Response.status(Status.OK).entity(appId).build();
				else
					response = Response.status(Status.BAD_REQUEST).entity(appId).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

	//TODO: PAGINATION
	/*DEV NOTES:
	 * Geolocation between points was done in a rude manner, we iterate all the points and calculate
	 * the distance between the two using haversine.
	 * 
	 * THIS IS NOT the right way to do it, if you have the time look into k-d trees ,
	 * http://en.wikipedia.org/wiki/K-d_tree
	 * 
	 * http://zaemis.blogspot.pt/2011/01/geolocation-search.html
	 * 
	 * or 
	 * http://wiki.apache.org/solr/SpatialSearch
	 *
	 */
	/**
	 * Retrieves all the data contained in this application.
	 * 
	 * 
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllData(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("latitude") String latitude,
			@QueryParam("longitude") String longitude,
			@QueryParam("radius") String radius,
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			//query parameters are present, only return the elements 
			if (latitude != null && longitude != null && radius != null) {
				ArrayList<String> all = appsMid.getAllDocsInRadius(appId, Double.parseDouble(latitude), 
						Double.parseDouble(longitude), Double.parseDouble(radius));
				IdsResultSet res = new IdsResultSet(all,pageNumber);
				
				response = Response.status(Status.OK).entity(res).build();
			//no query parameters return all docs
			} else {
				String all = appsMid.getAllDocInApp(appId);
				response = Response.status(Status.OK).entity(all).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
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
	public Response createDocumentRoot(JSONObject inputJsonObj,@Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = "location") String location) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			JSONObject data = null;
			try {
				data = (JSONObject) inputJsonObj.get("data");
			} catch (JSONException e) {
				System.out.println("Error parsing the JSON file.");
				e.printStackTrace();
			}
			if (appsMid.appExists(appId)) {
				if (appsMid.insertAppDocumentRoot(appId, data, location)) {
					response = Response.status(Status.CREATED).entity(appId).build();
				} else {
					response = Response.status(Status.BAD_REQUEST).entity(data).build();
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
	public Response createOrReplaceDocument(JSONObject inputJsonObj,@PathParam("pathId") List<PathSegment> path, 
			@Context UriInfo ui,@Context HttpHeaders hh,@HeaderParam(value = "location")String location) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			if (appsMid.appExists(appId)) {
				String url = appsMid.createAppDocPathFromListWithSlashes(appId,
						path);
				if (appsMid.insertIntoAppDocument(appId, url, inputJsonObj, location))
					response = Response.status(Status.CREATED).entity(appId).build();
				else
					response = Response.status(Status.BAD_REQUEST).entity(inputJsonObj).build();
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
			@Context HttpHeaders hh,
			@QueryParam("lat") String latitude,
			@QueryParam("long") String longitude,
			@QueryParam("radius") String radius) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			if (latitude != null && longitude != null && radius != null) {
				/*Set<String> all = appsMid.getElementInAppInRadius(appId, path,Double.parseDouble(latitude), 
						Double.parseDouble(longitude), Double.parseDouble(radius));*/
			//no query parameters return all docs
			} else {
				if (appsMid.dataExistsForElement(appId, path)) {
					String data = appsMid.getElementInAppDocument(appId, path);
					if (data == null)
						response = Response.status(Status.BAD_REQUEST).entity(appId).build();
					else
						response = Response.status(Status.OK).entity(data).build();
				} else {
					response = Response.status(Status.NOT_FOUND).entity(appId).build();
				}
			}
			
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
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
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			if (appsMid.dataExistsForElement(appId, path)) {
				if (appsMid.deleteDataInElement(appId, path))
					response = Response.status(Status.OK).entity("").build();
				else
					response = Response.status(Status.BAD_REQUEST).entity(path).build();
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
	 * Partial updates, adds non existing fields and edits existing ones.
	 * 
	 * @param path
	 * @param inputJson
	 * @return
	 */
	@PATCH
	@Path("/{pathId:.+}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response patchDataInElement(
			@PathParam("pathId") List<PathSegment> path, JSONObject inputJson,
			@Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = "location") String location) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			if (appsMid.dataExistsForElement(appId, path)) {
				String data = appsMid.patchDataInElement(appId, path, inputJson, location);
				if (data != null)
					response = Response.status(Status.OK).entity(data).build();
				else
					response = Response.status(Status.BAD_REQUEST).entity(appId).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}
}