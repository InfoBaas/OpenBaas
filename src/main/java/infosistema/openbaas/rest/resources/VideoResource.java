package infosistema.openbaas.rest.resources;

import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.VideoMiddleLayer;
import infosistema.openbaas.model.IdsResultSet;
import infosistema.openbaas.model.media.video.VideoInterface;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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

	private VideoMiddleLayer videoMid;
	private SessionMiddleLayer sessionMid;
	private static final Utils utils = new Utils();
	
	public VideoResource(String appId) {
		this.appId = appId;
		this.videoMid = MiddleLayerFactory.getVideoMiddleLayer();
	}

	//TODO: PAGINATION
	/**
	 * Gets all Video Identifiers.
	 * 
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllVideoIds(
			@CookieParam(value = "sessionToken") String sessionToken,
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			System.out.println("***********************************");
			System.out.println("********Finding all Video**********");
			ArrayList<String> videoIds = videoMid.getAllVideoIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
			IdsResultSet res = new IdsResultSet(videoIds,pageNumber);
			response = Response.status(Status.OK).entity(res).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
		return response;
	}

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
			@CookieParam(value = "sessionToken") String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("********Finding Video Meta**********");
			if (MiddleLayerFactory.getAppsMiddleLayer().appExists(appId)) {
				if (videoMid.videoExistsInApp(appId, videoId)) {
					VideoInterface video = videoMid.getVideoInApp(appId, videoId);
					response = Response.status(Status.OK).entity(video).build();
				} else
					response = Response.status(Status.NOT_FOUND).entity(videoId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
		return response;
	}

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
			@CookieParam(value = "sessionToken") String sessionToken) {
		Response response = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("***********Deleting Video***********");
			if (videoMid.videoExistsInApp(appId, videoId)) {
				videoMid.deleteVideoInApp(appId, videoId);
				response = Response.status(Status.OK).entity(appId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
		return response;
	}

	/**
	 * Downloads the audio File.
	 * 
	 * @param videoId
	 * @return
	 */
	@Path("{videoId}/{quality}/download")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response downloadAudio(@PathParam("videoId") String videoId,
			@CookieParam(value = "sessionToken") String sessionToken) {
		Response response = null;
		byte[] sucess = null;
		if (sessionMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("*********Downloading Video**********");
			System.out.println("Trying to download.");
			if (videoMid.videoExistsInApp(appId, videoId)) {
				VideoInterface video = videoMid.getVideoInApp(appId, videoId);
				sucess = videoMid.downloadVideoInApp(appId, videoId,video.getType());
				if (sucess!=null)
					return Response.ok(sucess, MediaType.APPLICATION_OCTET_STREAM)
							.header("content-disposition","attachment; filename = "+video.getFileName()+"."+video.getType()).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(videoId).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
		return response;
	}

	/**
	 * Uploads an audio File.
	 * 
	 * @param request
	 * @param headers
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response uploadVideo(@Context HttpServletRequest request,
			@Context HttpHeaders hh,@Context UriInfo ui,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@FormDataParam("location") String location) {
		Response response = null;
		int code = utils.treatParameters(ui, hh);
		String fileNameWithType = null;
		String fileType = new String();
		String fileName = new String();
		if (code == 1) {
			System.out.println("***********************************");
			System.out.println("*******Uploading to Storage********");
			fileNameWithType = fileDetail.getFileName();
			char[] charArray = fileNameWithType.toCharArray();
			boolean pop = false;
			int i = 0;
			while (!pop) {
				fileName += charArray[i];
				if (charArray[i + 1] == '.')
					pop = true;
				i++;
			}
			for (int k = 0; k < charArray.length - 1; k++) {
				if (charArray[k] == '.') {
					for (int j = k + 1; j < charArray.length; j++)
						fileType += charArray[j];
				}
			}
			String dir = "apps/" + appId + "/media/video";
			String videoId = MiddleLayerFactory.getStorageMiddleLayer().createLocalFile(uploadedInputStream,
					fileDetail, appId, fileType, dir);
			/* save it
			 *
			 * :::::::::::::::::MAJOR WARNING::::::::::::::::::::::Handling the
			 * Packet inputstream creates an infinite loop, you should not
			 * attempt it,the ReadMultiStream does not return -1 on EOS, it goes
			 * to the begining.You can read about it here:
			 * http://stackoverflow.com
			 * /questions/17861088/inputstream-infinit-loop
			 * 
			 * Solution: use the IOUtils to handle it, they have it implemented
			 * in a way that this error is handled produces the file
			 * successfully.
			 */
			//String file = dir + videoId + "." + fileType;
			videoMid.uploadVideoFileToServerWithoutGeoLocation(this.appId, videoId, fileType,
					fileName);
			response = Response.status(200).entity(fileNameWithType).build();
		}else if(code == -2)
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
			 .build();
		else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}
}
