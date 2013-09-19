package rest_resources;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import modelInterfaces.Audio;
import modelInterfaces.Video;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.MP3;
import rest_Models.MPEG;

public class VideoResource {

	private String appId;
	static final int idGenerator = 3;
	private AppsMiddleLayer appsMid;

	public VideoResource(AppsMiddleLayer appsMid, String appId) {
		this.appId = appId;
		this.appsMid = appsMid;
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
	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}

	/**
	 * Gets all Video Identifiers.
	 * 
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllVideoIds(
			@CookieParam(value = "sessionToken") String sessionToken) {
		Response response = null;
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("***********************************");
			System.out.println("********Finding all Video**********");
			Set<String> videoIds = appsMid.getAllVideoIdsInApp(appId);
			response = Response.status(Status.OK).entity(videoIds).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken)
					.build();
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
					response = Response.status(Status.NOT_FOUND)
							.entity(videoId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId)
						.build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken)
					.build();
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
				response = Response.status(Status.NOT_FOUND).entity(appId)
						.build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken)
					.build();
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
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("*********Downloading Video**********");
			System.out.println("Trying to download.");
			if (this.appsMid.videoExistsInApp(appId, videoId)) {
				Video video = this.appsMid.getVideoInApp(appId, videoId);
				boolean sucess = appsMid.downloadVideoInApp(appId, videoId);
				if (sucess)
					response = Response.status(Status.OK).entity(video).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(videoId)
						.build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken)
					.build();
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
		int code = this.treatParameters(ui, hh);
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
			String file = dir + videoId + "." + fileType;
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
