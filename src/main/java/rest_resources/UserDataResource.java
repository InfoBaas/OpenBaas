package rest_resources;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

public class UserDataResource {

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

	/*
	 * Returns a code corresponding to the sucess or failure Codes: -2 ->
	 * Forbidden -1 -> Bad request 1 -> sessionExists
	 */
	private int treatParameters(UriInfo ui, HttpHeaders hh) {
		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
		MultivaluedMap<String, String> pathParams = ui.getPathParameters();
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
					appsMid.refreshSession(sessionToken.getValue(),
							location.get(0), userAgent.get(0));
				} else
					appsMid.refreshSession(sessionToken.getValue());
			} else {
				code = -2;
			}
		}
		return code;
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

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllElementsInDocument(
			@Context UriInfo ui,
			@Context HttpHeaders hh, @QueryParam("latitude") String latitude,
			@QueryParam("longitude") String longitude,
			@QueryParam("radius") String radius) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			//query parameters are present, only return the elements 
			if (latitude != null && longitude != null && radius != null) {
				Set<String> all = appsMid.getAllUserDocsInRadius(appId, userId, Double.parseDouble(latitude), 
						Double.parseDouble(longitude), Double.parseDouble(radius));
				response = Response.status(Status.OK).entity(all).build();
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
		int code = this.treatParameters(ui, hh);
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
		int code = this.treatParameters(ui, hh);
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
		int code = this.treatParameters(ui, hh);
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
