package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Audio;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

//@Path("/apps/{appId}/media/audio")
public class AudioResource {
	static Map<String, Audio> audio = new HashMap<String, Audio>();
	String appId;
	private MediaMiddleLayer mediaMid;
	private AppsMiddleLayer appsMid;
	private SessionMiddleLayer sessionMid;

	public AudioResource(String appId) {
		this.appId = appId;
		this.mediaMid = MediaMiddleLayer.getInstance();
		this.sessionMid = SessionMiddleLayer.getInstance();
	}

	
	// *** CREATE *** //

	/**
	 * To upload a file simply send a Json object with the directory of the file
	 * you want to send and send the "compact" flag (it takes the value of true
	 * or false). If the compactInfo is true then only the id of the audio is
	 * returned, otherwise the entire information of the object is sent.
	 * 
	 * @param request
	 * @param headers
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response uploadAudio(@Context HttpServletRequest request, @Context UriInfo ui, @Context HttpHeaders hh,
			@FormDataParam(Const.FILE) InputStream uploadedInputStream, @FormDataParam(Const.FILE) FormDataContentDisposition fileDetail,
			@PathParam(Const.APP_ID) String appId, @HeaderParam(value = Const.LOCATION) String location,
			@FormDataParam(Const.MESSAGEID) String messageId) {
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		if(!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			Result res = mediaMid.createMedia(uploadedInputStream, fileDetail, appId, userId, ModelEnum.audio, location, Metadata.getNewMetadata(location),messageId);
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
	 * Deletes the audio File (from the DB and FileSystem).
	 * @param audioId
	 * @return
	 */
	@Path("{audioId}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteAudio(@PathParam("audioId") String audioId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if (mediaMid.mediaExists(appId, ModelEnum.audio, audioId)) {
				if(mediaMid.deleteMedia(appId, ModelEnum.audio, audioId))
					response = Response.status(Status.OK).entity("").build();
				else
					response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Del Meta")).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		}else if(code == -2){
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		}else if(code == -1)
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
	public Response find(@Context UriInfo ui, @Context HttpHeaders hh, @QueryParam("show") JSONArray arrayShow,
			@QueryParam("query") JSONObject query, @QueryParam(Const.RADIUS) String radiusStr,
			@QueryParam(Const.LAT) String latitudeStr, @QueryParam(Const.LONG) String longitudeStr,
			@QueryParam(Const.ELEM_COUNT) String pageCount, @QueryParam(Const.ELEM_INDEX) String pageIndex,
			@QueryParam(Const.PAGE_NUMBER) String pageNumberStr, @QueryParam(Const.PAGE_SIZE) String pageSizeStr, 
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_BY) String orderTypeStr) {
		QueryParameters qp = QueryParameters.getQueryParameters(appId, null, query, radiusStr, latitudeStr, longitudeStr, 
				pageNumberStr, pageSizeStr, orderByStr, orderTypeStr, ModelEnum.audio,pageCount,pageIndex);
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
	 * Retrieve the audio Metadata using its ID.
	 * @param audioId
	 * @return
	 */
	@Path("{audioId}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAudioById(@PathParam("audioId") String audioId, 
			@Context UriInfo ui, @Context HttpHeaders hh
			) {
		Response response = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if (appsMid.appExists(this.appId)) {
				if (mediaMid.mediaExists(this.appId, ModelEnum.audio, audioId)) {
					Result res = mediaMid.getMedia(appId, ModelEnum.audio, audioId, true);
					response = Response.status(Status.OK).entity(res).build();
				} else {
					response = Response.status(Status.NOT_FOUND).entity(new Error("")).build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		}else if(code == -2){
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		}else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	// *** DOWNLOAD *** //

	/**
	 * Downloads the audio in the specified quality.
	 * @param audioId
	 * @return
	 */
	@Path("{audioId}/{quality}/download")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response downloadAudio(@PathParam("audioId") String audioId, @PathParam("quality") String quality,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		byte[] sucess = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if (this.mediaMid.mediaExists(appId, ModelEnum.audio, audioId)) {
				Audio audio = (Audio)(mediaMid.getMedia(appId, ModelEnum.audio, audioId, false).getData());
				sucess = mediaMid.download(appId, ModelEnum.audio, audioId,audio.getFileExtension(),quality,null);
				if (sucess!=null)
					return Response.ok(sucess, MediaType.APPLICATION_OCTET_STREAM)
							.header("content-disposition","attachment; filename = "+audio.getFileName()+"."+audio.getFileExtension()).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(audioId)
				.build();
		}else if(code == -2){
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
					.build();
		}else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			.build();
		return response;
	}

	// *** OTHERS *** //

	// *** RESOURCES *** //

}
