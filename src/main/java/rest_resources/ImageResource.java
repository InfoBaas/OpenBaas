package rest_resources;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import modelInterfaces.Image;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import resourceModelLayer.AppsMiddleLayer;

//apps/{appId}/media/images
public class ImageResource {

	private String appId;
	private AppsMiddleLayer appsMid;
	
	public ImageResource(AppsMiddleLayer appsMid, String appId) {
		this.appId = appId;
		this.appsMid = appsMid;
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
	 * Get all image Identifiers in the application.
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllImageIds(@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("***********************************");
			System.out.println("********Finding all Images*********");
			Set<String> imageIds = this.appsMid.getAllImageIdsInApp(this.appId);
			response = Response.status(Status.OK).entity(imageIds).build();
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
		 .build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;		
	}
	/**
	 * Uploads an image to the server and creates in the DB all the required information. Necessary fields: "fileDirectory"
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response uploadImage(@Context UriInfo ui, @Context HttpHeaders hh,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@FormDataParam("location") String location,
			@PathParam("appId") String appId){
		Response response = null;
		int code = this.treatParameters(ui, hh);
		String fileNameWithType = null;
		String extension = new String();
		String fileName = new String();
		boolean uploadOk = false;
		if (code == 1) {
			System.out.println("***********************************");
			System.out.println("*******Uploading Image********");
			fileNameWithType = fileDetail.getFileName();
			String fileDirectory = "apps/"+appId+"/media/images/";
			char[] charArray = fileNameWithType.toCharArray();
			boolean pop = false;
			int i = 0;
			while (!pop) {
				fileName += charArray[i];
				if (charArray[i+1] == '.')
					pop = true;
				i++;
			}
			for (int k = 0; k < charArray.length - 1; k++) {
				if (charArray[k] == '.') {
					for (int j = k + 1; j < charArray.length; j++)
						extension += charArray[j];
				}
			}
			String imageId = appsMid.createLocalFile(uploadedInputStream,
					fileDetail, appId, extension, fileDirectory);
			if(location!= null)
				uploadOk = this.appsMid.uploadImageFileToServerWithGeoLocation(this.appId, 
					location, extension, fileName, imageId);
			else
				uploadOk = this.appsMid.uploadImageFileToServerWithoutGeoLocation(this.appId, 
						extension, fileName, imageId);
			if(imageId != null && uploadOk != false)
				response = Response.status(Status.OK).entity(imageId).build();
			else{
				response = Response.status(Status.BAD_REQUEST).entity(appId).build();
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
	 * Get image metadata.
	 * @param imageId
	 * @return
	 */
	@Path("{imageId}")
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getImageMetadata(@PathParam("imageId") String imageId,
			@Context UriInfo ui, @Context HttpHeaders hh){
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("********Finding Image Meta**********");
			Image temp = null;
			if(appsMid.appExists(this.appId)){
				if(appsMid.imageExistsInApp(this.appId, imageId)){
					temp = appsMid.getImageInApp(this.appId, imageId);
					response = Response.status(Status.OK).entity(temp).build();
				}
				else{
					response = Response.status(Status.NOT_FOUND).entity(temp).build();
				}
			}
			else{
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
	 * Deletes the video (from filesystem and database).
	 * 
	 * @param videoId
	 * @return
	 */
	@Path("{imageId}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteImage(@PathParam("imageId") String imageId,
			@CookieParam(value = "sessionToken") String sessionToken) {
		Response response = null;
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("***********Deleting Image***********");
			if (appsMid.imageExistsInApp(appId, imageId)) {
				this.appsMid.deleteImageInApp(appId, imageId);
				response = Response.status(Status.OK).entity(appId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId)
						.build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken)
					.build();
		return response;
	}
}
