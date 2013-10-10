package infosistema.openbaas.rest;

import infosistema.openbaas.middleLayer.ImageMiddleLayer;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.model.IdsResultSet;
import infosistema.openbaas.model.media.image.ImageInterface;
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
	private ImageMiddleLayer imageMid;
	
	
	public ImageResource(String appId) {
		this.appId = appId;
		this.imageMid = MiddleLayerFactory.getImageMiddleLayer();
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
		List<String> listRes = new ArrayList<String>();
		int code = utils.treatParameters(ui, hh);
		Integer totalNumberPages=null;
		Integer iniIndex = (pageNumber-1)*pageSize;
		Integer finIndex = (((pageNumber-1)*pageSize)+pageSize);
		if (code == 1) {
			System.out.println("******************************************");
			System.out.println("********Finding all Images - GEO**********");
			ArrayList<String> imagesIds = new ArrayList<String>();
			if (latitude != null && longitude != null && radius != null) {
				imagesIds = imageMid.getAllImagesIdsInRadius(appId, Double.parseDouble(latitude),Double.parseDouble(longitude), 
						Double.parseDouble(radius),pageNumber,pageSize,orderBy,orderType);
				if(iniIndex>imagesIds.size())
					return Response.status(Status.BAD_REQUEST).entity("Invalid pagination indexes.").build();
				if(finIndex>imagesIds.size())
					listRes = imagesIds.subList(iniIndex, imagesIds.size());
				else
					listRes = imagesIds.subList(iniIndex, finIndex);
				totalNumberPages = (int) utils.roundUp(imagesIds.size(),pageSize);
			}else{
				imagesIds = imageMid.getAllImageIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
				listRes = imagesIds;
				totalNumberPages = imageMid.countAllImages(appId)/pageSize;
			}
			
			IdsResultSet res = new IdsResultSet(listRes,pageNumber,totalNumberPages);
			
			response = Response.status(Status.OK).entity(res).build();
		} else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		 }else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
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
			String imageId = MiddleLayerFactory.getStorageMiddleLayer().createLocalFile(uploadedInputStream,fileDetail, appId, extension, fileDirectory);
			if(location!= null)
				uploadOk = imageMid.uploadImageFileToServerWithGeoLocation(this.appId, 
					location.get(0), extension, fileName, imageId);
			else
				uploadOk = imageMid.uploadImageFileToServerWithoutGeoLocation(this.appId,extension, fileName, imageId);
			if(imageId != null && uploadOk != false)
				response = Response.status(Status.OK).entity(imageId).build();
			else{
				response = Response.status(Status.BAD_REQUEST).entity(appId).build();
			}
		}else if(code == -2){
			 response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
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
			ImageInterface temp = null;
			if(MiddleLayerFactory.getAppsMiddleLayer().appExists(this.appId)){
				if(imageMid.imageExistsInApp(this.appId, imageId)){
					temp = imageMid.getImageInApp(this.appId, imageId);
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
			if (imageMid.imageExistsInApp(appId, imageId)) {
				ImageInterface image = imageMid.getImageInApp(appId, imageId);
				sucess = imageMid.downloadImageInApp(appId, imageId,image.getType());
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
		if (MiddleLayerFactory.getSessionMiddleLayer().sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("***********Deleting Image***********");
			if (imageMid.imageExistsInApp(appId, imageId)) {
				this.imageMid.deleteImageInApp(appId, imageId);
				response = Response.status(Status.OK).entity(appId).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(appId).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(sessionToken).build();
		return response;
	}
}
