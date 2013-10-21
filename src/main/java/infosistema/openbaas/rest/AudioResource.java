package infosistema.openbaas.rest;

import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.AudioMiddleLayer;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.model.IdsResultSet;
import infosistema.openbaas.model.media.audio.Audio;
import infosistema.openbaas.model.media.audio.AudioInterface;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;

//@Path("/apps/{appId}/media/audio")
public class AudioResource {
	static Map<String, Audio> audio = new HashMap<String, Audio>();
	String appId;
	static final int idGenerator = 3;
	private AudioMiddleLayer audioMid;
	private AppsMiddleLayer appsMid;

	public AudioResource(String appId) {
		this.appId = appId;
		this.audioMid = MiddleLayerFactory.getAudioMiddleLayer();
	}

	
	// *** CREATE *** //

	//TODO: LOCATION
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
			@HeaderParam(value = "location") String location) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
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
			String audioId = MiddleLayerFactory.getStorageMiddleLayer().createLocalFile(uploadedInputStream, fileDetail, appId, fileType, dir);
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
			this.audioMid.uploadAudioFileToServerWithGeoLocation(appId, location, fileType, fileName, audioId);

		}else if(code == -2){
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
					.build();
		}else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			.build();
		return response;
	}

	
	// *** UPDATE *** //

	
	// *** DELETE *** //

	//TODO: LOCATION
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
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("***********Deleting Audio***********");
			if (audioMid.audioExistsInApp(appId, audioId)) {
				this.audioMid.deleteAudioInApp(appId, audioId);
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

	
	// *** GET LIST *** //

	//TODO: LOCATION
	/**
	 * Retrieve all the audio Ids for this application.
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllAudioIds(@Context UriInfo ui, @Context HttpHeaders hh,@QueryParam("lat") 
	String latitude,@QueryParam("long") String longitude,@QueryParam("radius") String radius,
	@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
	@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("***********************************");
			System.out.println("********Finding all Audio**********");
			ArrayList<String> audioIds = null;
			if (latitude != null && longitude != null && radius != null) {
				audioIds = audioMid.getAllAudioIdsInRadius(appId, Double.parseDouble(latitude),
						Double.parseDouble(longitude), Double.parseDouble(radius));
			}else
				audioIds = audioMid.getAllAudioIds(appId,pageNumber,pageSize,orderBy,orderType);
			IdsResultSet res = new IdsResultSet(audioIds,pageNumber);
			response = Response.status(Status.OK).entity(res).build();
		} else if(code == -2){
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
					.build();
		}else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			.build();
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
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("********Finding Audio Meta**********");
			AudioInterface temp = null;
			if (appsMid.appExists(this.appId)) {
				if (audioMid.audioExistsInApp(this.appId, audioId)) {
					temp = audioMid.getAudioInApp(appId, audioId);
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

	// *** DOWNLOAD *** //

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
		byte[] sucess = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("*********Downloading Audio**********");
			if (this.audioMid.audioExistsInApp(appId, audioId)) {
				AudioInterface audio = this.audioMid.getAudioInApp(appId, audioId);
				sucess = audioMid.downloadAudioInApp(appId, audioId,audio.getType());
				if (sucess!=null)
					return Response.ok(sucess, MediaType.APPLICATION_OCTET_STREAM)
							.header("content-disposition","attachment; filename = "+audio.getFileName()+"."+audio.getType()).build();
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

}
