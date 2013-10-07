package infosistema.openbaas.rest_resources;

import infosistema.openbaas.modelInterfaces.Image;
import infosistema.openbaas.resourceModelLayer.AppsMiddleLayer;
import infosistema.openbaas.rest_Models.IdsResultSet;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;


import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


//apps/{appId}/media/images
public class ImageResource {

	private static final Utils utils = new Utils();
	private String appId;
	private AppsMiddleLayer appsMid;
	
	
	public ImageResource(AppsMiddleLayer appsMid, String appId) {
		this.appId = appId;
		this.appsMid = appsMid;
	}
	
	//TODO: PAGINATION
	/**
	 * Retrieve all the image Ids for this application.
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllImagesIds(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("lat") String latitude,	@QueryParam("long") String longitude,@QueryParam("radius") String radius, 
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		int code = utils.treatParameters(ui, hh);
		Integer totalNumberPages=null;
		if (code == 1) {
			System.out.println("******************************************");
			System.out.println("********Finding all Images - GEO**********");
			ArrayList<String> imagesIds = new ArrayList<String>();
			if (latitude != null && longitude != null && radius != null) {
				imagesIds = appsMid.getAllImagesIdsInRadius(appId, Double.parseDouble(latitude),Double.parseDouble(longitude), 
						Double.parseDouble(radius),pageNumber,pageSize,orderBy,orderType);
			}else{
				imagesIds = appsMid.getAllImageIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
				totalNumberPages = appsMid.countAllImages(appId)/pageSize;
			}
			
			IdsResultSet res = new IdsResultSet(imagesIds,pageNumber,totalNumberPages);
			
			response = Response.status(Status.OK).entity(res).build();
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}
	
	/**
	 * Get all image Identifiers in the application.
	 * @return
	 */
	/*
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllImageIds(@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		int code = utils.treatParameters(ui, hh);
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
	*/
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
			@PathParam("appId") String appId){
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		int code = utils.treatParameters(ui, hh);
		String fileNameWithType = null;
		String extension = new String();
		String fileName = new String();
		List<String> location = null;
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("location"))
				location = entry.getValue();	
		}
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
			String imageId = appsMid.createLocalFile(uploadedInputStream,fileDetail, appId, extension, fileDirectory);
			if(location!= null)
				uploadOk = this.appsMid.uploadImageFileToServerWithGeoLocation(this.appId, 
					location.get(0), extension, fileName, imageId);
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
	public Response getImageMetadata(@PathParam("imageId") String imageId,@Context UriInfo ui, @Context HttpHeaders hh){
		Response response = null;
		int code = utils.treatParameters(ui, hh);
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
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}
	
	@Path("{imageId}/{quality}/download")
	@GET
	@Produces({ MediaType.APPLICATION_OCTET_STREAM })
	public Response downloadImage(@PathParam("imageId") String imageId,	@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		byte[] sucess = null;
		int code = utils.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("************************************");
			System.out.println("*********Downloading Image**********");
			if (this.appsMid.imageExistsInApp(appId, imageId)) {
				Image image = this.appsMid.getImageInApp(appId, imageId);
				sucess = appsMid.downloadImageInApp(appId, imageId,image.getType());
				if (sucess!=null){ 
					return Response.ok(sucess, MediaType.APPLICATION_OCTET_STREAM)
					.header("content-disposition","attachment; filename = "+image.getFileName()+"."+image.getType()).build();
					//response = Response.status(Status.OK).entity(image).build();
				}
			} else
				response = Response.status(Status.NOT_FOUND).entity(imageId).build();
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
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
	public Response deleteImage(@PathParam("imageId") String imageId, @CookieParam(value = "sessionToken") String sessionToken) {
		Response response = null;
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("***********Deleting Image***********");
			if (appsMid.imageExistsInApp(appId, imageId)) {
				this.appsMid.deleteImageInApp(appId, imageId);
				response = Response.status(Status.OK).entity(appId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
		return response;
	}
}
