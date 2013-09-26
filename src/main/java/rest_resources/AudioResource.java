package rest_resources;

import simulators.AudioSimulator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.Status;

import modelInterfaces.Audio;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.MP3;

//@Path("/apps/{appId}/media/audio")
public class AudioResource {
	static Map<String, MP3> audio = new HashMap<String, MP3>();
	String appId;
	static final int idGenerator = 3;
	private AppsMiddleLayer appsMid;

	public AudioResource(AppsMiddleLayer appsMid, String appId) {
		this.appId = appId;
		this.appsMid = appsMid;
		// AudioSimulator audioSim = new AudioSimulator(this.audio);
	}

	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}
	/*
	 * Returns a code corresponding to the sucess or failure Codes: 
	 * -2 -> Forbidden
	 * -1 -> Bad request
	 * 1 ->
	 * sessionExists
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
			}else{
				code = -2;
			}
		}
		return code;
	}
	/**
	 * Retrieve all the audio Ids for this application.
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllAudioIds(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("lat") String latitude,
			@QueryParam("long") String longitude, 
			@QueryParam("radius") String radius) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("***********************************");
			System.out.println("********Finding all Audio**********");
			Set<String> audioIds = null;
			if (latitude != null && longitude != null && radius != null) {
				audioIds = appsMid.getAllAudioIdsInRadius(appId, Double.parseDouble(latitude),
						Double.parseDouble(longitude), Double.parseDouble(radius));
			}else
				audioIds = appsMid.getAllAudioIds(appId);
			response = Response.status(Status.OK).entity(audioIds).build();
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
		 .build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}

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
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("********Finding Audio Meta**********");
			Audio temp = null;
			if (appsMid.appExists(this.appId)) {
				if (appsMid.audioExistsInApp(this.appId, audioId)) {
					temp = appsMid.getAudioInApp(appId, audioId);
					response = Response.status(Status.OK).entity(temp).build();
				} else {
					response = Response.status(Status.NOT_FOUND).entity(temp)
							.build();
				}
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			}
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
		 .build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}
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
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("***********Deleting Audio***********");
			if (appsMid.audioExistsInApp(appId, audioId)) {
				this.appsMid.deleteAudioInApp(appId, audioId);
				response = Response.status(Status.OK).entity(appId).build();
			} else {
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
			}
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
		 .build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}
	/**
	 * Downloads the audio in the specified quality.
	 * @param audioId
	 * @return
	 */
	@Path("{audioId}/{quality}/download")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response downloadAudio(@PathParam("audioId") String audioId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("*********Downloading Audio**********");
			if (this.appsMid.audioExistsInApp(appId, audioId)) {
				Audio audio = this.appsMid.getAudioInApp(appId, audioId);
				boolean sucess = appsMid.downloadAudioInApp(appId, audioId);
				if (sucess)
					response = Response.status(Status.OK).entity(audio).build();
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
	public Response uploadAudio(@Context HttpServletRequest request,
			@Context UriInfo ui, @Context HttpHeaders hh,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@PathParam("appId") String appId,
			@FormDataParam("location") String location) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		String fileNameWithType = null;
		String fileType = new String();
		String fileName = new String();
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("*********Uploading Audio************");
			
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
			String dir = "apps/" + appId + "/storage/";
			String audioId = appsMid.createLocalFile(uploadedInputStream,
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
			String file = dir + audioId + "." + fileType;
			this.appsMid.uploadAudioFileToServer(appId,audioId, fileType, location, fileName );
			
//			String fileDirectory = null;
//			String compact = null;
//			String geolocation = null;
//			try {
//				fileDirectory = (String) inputJsonObj.get("dir");
//				compact = (String) inputJsonObj.get("compact");
//				geolocation = (String) inputJsonObj.opt("location"); //geo-location
//			} catch (JSONException e) {
//				System.out.println("Error reading Json input.");
//				e.printStackTrace();
//			}
//			if (appsMid.appExists(appId)) {
//				String audioId = this.appsMid.uploadAudioFileToServer(
//						this.appId, fileDirectory, geolocation, ".mp4");
//				if (audioId != null)
//					if (compact.equalsIgnoreCase("true"))
//						response = Response.status(Status.OK).entity(audioId)
//								.build();
//					else {// ***************************************************************COMPACT
//						Audio created = appsMid.getAudioInApp(appId, audioId);
//						response = Response.status(Status.OK).entity(created)
//								.build();
//					}
//				else {
//					response = Response.status(Status.BAD_REQUEST).entity(appId)
//							.build();
//				}
//			}
//			else{
//				response = Response.status(Status.NOT_FOUND).entity(appId)
//						.build();
//			}
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
		 .build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}

}
