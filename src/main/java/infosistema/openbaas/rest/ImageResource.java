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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;


import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


//apps/{appId}/media/images
public class ImageResource {

	private String appId;
	private ImageMiddleLayer imageMid;


	public ImageResource(String appId) {
		this.appId = appId;
		this.imageMid = MiddleLayerFactory.getImageMiddleLayer();
	}

	// *** CREATE *** //

	//TODO: LOCATION
	/**
	 * Uploads an image to the server and creates in the DB all the required information. Necessary fields: "fileDirectory"
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response uploadImage(@Context HttpHeaders hh, @Context UriInfo ui,
			@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@HeaderParam(value = "location") String location) {
		
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String imageId = imageMid.uploadImage(uploadedInputStream, fileDetail, appId, location);
			if (imageId == null) { 
				response = Response.status(Status.BAD_REQUEST).entity(appId).build();
			} else {
				response = Response.status(Status.OK).entity(imageId).build();
			}
		} else if(code == -2){
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
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
	
	
	// *** GET LIST *** //

	//TODO: LOCATION
	/**
	 * Retrieve all the image Ids for this application.
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllImagesIds(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam("lat") String latitude,	@QueryParam("long") String longitude, @QueryParam("radius") String radius, 
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		List<String> listRes = new ArrayList<String>();
		int code = Utils.treatParameters(ui, hh);
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
				totalNumberPages = (int) Utils.roundUp(imagesIds.size(),pageSize);
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


	// *** GET *** //

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
		int code = Utils.treatParameters(ui, hh);
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

	
	// *** DOWNLOAD *** //
	
	@Path("{imageId}/{quality}/download")
	@GET
	@Produces({ MediaType.APPLICATION_OCTET_STREAM })
	public Response downloadImage(@PathParam("imageId") String imageId,	@Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		byte[] sucess = null;
		int code = Utils.treatParameters(ui, hh);
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

	// *** RESOURCES *** //

	// *** OTHERS *** //

}
