package infosistema.openbaas.rest;

import infosistema.openbaas.data.IdsResultSet;
import infosistema.openbaas.data.ModelEnum;
import infosistema.openbaas.data.models.Video;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import java.util.ArrayList;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
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



import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


public class VideoResource {

	private String appId;

	private MediaMiddleLayer mediaMid;
	private SessionMiddleLayer sessionMid;
	
	public VideoResource(String appId) {
		this.appId = appId;
		this.mediaMid = MiddleLayerFactory.getMediaMiddleLayer();
	}

	// *** CREATE *** //

	//TODO: LOCATION
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
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail, @HeaderParam(value = Const.LOCATION) String location) {

		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String videoId = mediaMid.uploadMedia(uploadedInputStream, fileDetail, appId, ModelEnum.video, location);
			if (videoId == null) { 
				response = Response.status(Status.BAD_REQUEST).entity(appId).build();
			} else {
				response = Response.status(Status.OK).entity(videoId).build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.") .build();
		return response;
	}
	
	
	// *** UPDATE *** //
	
	
	// *** DELETE *** //
	
	//TODO: LOCATION
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
			@CookieParam(value = Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			Log.debug("", this, "deleteVideo", "***********Deleting Video***********");
			if (mediaMid.mediaExists(appId, ModelEnum.video, videoId)) {
				mediaMid.deleteMedia(appId, ModelEnum.video, videoId);
				response = Response.status(Status.OK).entity(appId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
		return response;
	}

	
	// *** GET LIST *** //
	
	//TODO: LOCATION
	/**
	 * Gets all Video Identifiers.
	 * 
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllVideoIds(
			@CookieParam(value = Const.SESSION_TOKEN) String sessionToken,
			@QueryParam(Const.PAGE_NUMBER) Integer pageNumber, @QueryParam(Const.PAGE_SIZE) Integer pageSize, 
			@QueryParam(Const.ORDER_BY) String orderBy, @QueryParam(Const.ORDER_BY) String orderType ) {
		if (pageNumber == null) pageNumber = Const.getPageNumber();
		if (pageSize == null) 	pageSize = Const.getPageSize();
		if (orderBy == null) 	orderBy = Const.getOrderBy();
		if (orderType == null) 	orderType = Const.getOrderType();
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			Log.debug("", this, "findAllvideos", "********Finding all Video**********");
			ArrayList<String> videoIds = mediaMid.getAllMediaIds(appId, ModelEnum.video, pageNumber, pageSize,
					orderBy, orderType);
			IdsResultSet res = new IdsResultSet(videoIds,pageNumber);
			response = Response.status(Status.OK).entity(res).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
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
			@CookieParam(value = Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			Log.debug("", this, "findById", "********Finding Video Meta**********");
			if (MiddleLayerFactory.getAppsMiddleLayer().appExists(appId)) {
				if (mediaMid.mediaExists(appId, ModelEnum.video, videoId)) {
					Video video = (Video)(mediaMid.getMedia(appId, ModelEnum.video, videoId));
					response = Response.status(Status.OK).entity(video).build();
				} else
					response = Response.status(Status.NOT_FOUND).entity(videoId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
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
	public Response downloadVideo(@PathParam("videoId") String videoId,
			@CookieParam(value = Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		byte[] sucess = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			Log.debug("", this, "updateUser", "*********Downloading Video**********");
			if (mediaMid.mediaExists(appId, ModelEnum.video, videoId)) {
				Video video = (Video)(mediaMid.getMedia(appId, ModelEnum.video, videoId));
				sucess = mediaMid.download(appId, ModelEnum.video, videoId,video.getType());
				if (sucess!=null)
					return Response.ok(sucess, MediaType.APPLICATION_OCTET_STREAM)
							.header("content-disposition","attachment; filename = "+video.getFileName()+"."+video.getType()).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(videoId).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
		return response;
	}

	// *** RESOURCES *** //

	
	// *** OTHERS *** //

}
