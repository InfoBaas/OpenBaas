package rest_resources;

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

import modelInterfaces.Video;


import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.IdsResultSet;
import utils.Const;
import utils.Utils;

public class VideoResource {

	private String appId;

	private AppsMiddleLayer appsMid;
	private static final Utils utils = new Utils();
	
	public VideoResource(AppsMiddleLayer appsMid, String appId) {
		this.appId = appId;
		this.appsMid = appsMid;
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
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("***********************************");
			System.out.println("********Finding all Video**********");
			ArrayList<String> videoIds = appsMid.getAllVideoIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
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
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("********Finding Video Meta**********");
			if (appsMid.appExists(appId)) {
				if (appsMid.videoExistsInApp(appId, videoId)) {
					Video video = this.appsMid.getVideoInApp(appId, videoId);
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
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("***********Deleting Video***********");
			if (appsMid.videoExistsInApp(appId, videoId)) {
				this.appsMid.deleteVideoInApp(appId, videoId);
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
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("*********Downloading Video**********");
			System.out.println("Trying to download.");
			if (this.appsMid.videoExistsInApp(appId, videoId)) {
				Video video = this.appsMid.getVideoInApp(appId, videoId);
				sucess = appsMid.downloadVideoInApp(appId, videoId,video.getType());
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
			String videoId = appsMid.createLocalFile(uploadedInputStream,
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
			this.appsMid.uploadVideoFileToServerWithoutGeoLocation(this.appId, videoId, fileType,
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
