package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Video;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

public class VideoResource {

	private String appId;

	private MediaMiddleLayer mediaMid;
	private SessionMiddleLayer sessionMid;
	
	public VideoResource(String appId) {
		this.appId = appId;
		this.mediaMid = MediaMiddleLayer.getInstance();
		this.sessionMid = SessionMiddleLayer.getInstance();
	}

	// *** CREATE *** //

	/**
	 * Uploads an video File.
	 * 
	 * @param request
	 * @param headers
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response uploadVideo(@Context HttpHeaders hh, @Context UriInfo ui, @FormDataParam(Const.FILE) InputStream uploadedInputStream,
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail, @HeaderParam(value = Const.LOCATION) String location,
			@FormDataParam(Const.MESSAGEID) String messageId) {

		Response response = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String userId = sessionMid.getUserIdUsingSessionToken(Utils.getSessionToken(hh));
			Result res = mediaMid.createMedia(uploadedInputStream, fileDetail, appId, userId, ModelEnum.video, location, Metadata.getNewMetadata(location),messageId);
			if (res == null || res.getData() == null)
				response = Response.status(Status.BAD_REQUEST).entity(new Error(appId)).build();
			else
				response = Response.status(Status.OK).entity(res).build();
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}
	
	
	// *** UPDATE *** //
	
	
	// *** DELETE *** //
	
	/**
	 * Deletes the video (from filesystem and database).
	 * 
	 * @param videoId
	 * @return
	 */
	@Path("{videoId}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteVideo(@PathParam("videoId") String videoId,
			@HeaderParam(value = Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		if (sessionMid.sessionTokenExists(sessionToken)) {
			if (mediaMid.mediaExists(appId, ModelEnum.video, videoId)) {
				if (mediaMid.deleteMedia(appId, ModelEnum.video, videoId))
					response = Response.status(Status.OK).entity("").build();
				else
					response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Del Meta")).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(new Error(sessionToken)).build();
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
	public Response find(@Context UriInfo ui, @Context HttpHeaders hh, @QueryParam("show") JSONArray arrayShow,
			@QueryParam("query") JSONObject query, @QueryParam(Const.RADIUS) String radiusStr,
			@QueryParam(Const.LAT) String latitudeStr, @QueryParam(Const.LONG) String longitudeStr,
			@QueryParam(Const.ELEM_COUNT) String elemCount, @QueryParam(Const.ELEM_INDEX) String elemIndex,
			@QueryParam(Const.PAGE_NUMBER) String pageNumberStr, @QueryParam(Const.PAGE_SIZE) String pageSizeStr, 
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_BY) String orderTypeStr) {
		QueryParameters qp = QueryParameters.getQueryParameters(appId, null, query, radiusStr, latitudeStr, longitudeStr, 
				pageNumberStr, pageSizeStr, orderByStr, orderTypeStr, ModelEnum.video,elemCount,elemIndex);
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();

		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			try {
				ListResult res = mediaMid.find(qp,arrayShow);
				response = Response.status(Status.OK).entity(res).build();
			} catch (Exception e) {
				response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}

	
	// *** GET *** //
	
	/**
	 * Gets the video metadata.
	 * 
	 * @param videoId
	 * @return
	 */
	@Path("{videoId}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findById(@PathParam("videoId") String videoId,
			@HeaderParam(value = Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		if (sessionMid.sessionTokenExists(sessionToken)) {
			if (AppsMiddleLayer.getInstance().appExists(appId)) {
				if (mediaMid.mediaExists(appId, ModelEnum.video, videoId)) {
					Result res = mediaMid.getMedia(appId, ModelEnum.video, videoId, true);
					response = Response.status(Status.OK).entity(res).build();
				} else
					response = Response.status(Status.NOT_FOUND).entity(new Error(videoId)).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(new Error(sessionToken)).build();
		return response;
	}

	
	// *** DOWNLOAD *** //
	
	/**
	 * Downloads the video File.
	 * 
	 * @param videoId
	 * @return
	 */
	@Path("{videoId}/{quality}/download")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response downloadVideo(@PathParam("videoId") String videoId, @PathParam("quality") String quality,
			@HeaderParam(value = Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		byte[] sucess = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			if (mediaMid.mediaExists(appId, ModelEnum.video, videoId)) {
				Video video = (Video)(mediaMid.getMedia(appId, ModelEnum.video, videoId, false).getData());
				sucess = mediaMid.download(appId, ModelEnum.video, videoId, video.getFileExtension(),quality,null);
				if (sucess!=null)
					return Response.ok(sucess, MediaType.APPLICATION_OCTET_STREAM)
							.header("content-disposition","attachment; filename = "+video.getFileName()+"."+video.getFileExtension()).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(new Error(videoId)).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(new Error(sessionToken)).build();
		return response;
	}

	// *** RESOURCES *** //

	
	// *** OTHERS *** //

}
