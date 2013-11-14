package infosistema.openbaas.rest;

import infosistema.openbaas.data.ListResultSet;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
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
	private MediaMiddleLayer mediaMid;


	public ImageResource(String appId) {
		this.appId = appId;
		this.mediaMid = MiddleLayerFactory.getMediaMiddleLayer();
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
	public Response uploadImage(@Context HttpHeaders hh, @Context UriInfo ui, @FormDataParam(Const.FILE) InputStream uploadedInputStream,
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail, @HeaderParam(value = Const.LOCATION) String location) {
		Response response = null;
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String imageId = mediaMid.createMedia(uploadedInputStream, fileDetail, appId, ModelEnum.image, location);
			if (imageId == null) { 
				response = Response.status(Status.BAD_REQUEST).entity(appId).build();
			} else {
				response = Response.status(Status.OK).entity(imageId).build();
			}
		} else if(code == -2) {
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
	public Response deleteImage(@PathParam("imageId") String imageId, @CookieParam(value = Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		if (MiddleLayerFactory.getSessionMiddleLayer().sessionTokenExists(sessionToken)) {
			Log.debug("", this, "deleteImage", "***********Deleting Image***********");
			if (mediaMid.mediaExists(appId, ModelEnum.image, imageId)) {
				this.mediaMid.deleteMedia(appId, ModelEnum.image, imageId);
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
			@QueryParam(Const.PAGE_NUMBER) Integer pageNumber, @QueryParam(Const.PAGE_SIZE) Integer pageSize, 
			@QueryParam(Const.ORDER_BY) String orderBy, @QueryParam(Const.ORDER_BY) String orderType ) {
		if (pageNumber == null) pageNumber = Const.getPageNumber();
		if (pageSize == null) 	pageSize = Const.getPageSize();
		if (orderBy == null) 	orderBy = Const.getOrderBy();
		if (orderType == null) 	orderType = Const.getOrderType();
		Response response = null;
		List<String> listRes = new ArrayList<String>();
		int code = Utils.treatParameters(ui, hh);
		Integer totalNumberPages=null;
		Integer iniIndex = (pageNumber-1)*pageSize;
		Integer finIndex = (((pageNumber-1)*pageSize)+pageSize);
		if (code == 1) {
			ArrayList<String> imagesIds = new ArrayList<String>();
			if (latitude != null && longitude != null && radius != null) {
				if(iniIndex>imagesIds.size())
					return Response.status(Status.BAD_REQUEST).entity("Invalid pagination indexes.").build();
				imagesIds = mediaMid.getAllImagesIdsInRadius(appId, Double.parseDouble(latitude),Double.parseDouble(longitude), 
						Double.parseDouble(radius),pageNumber,pageSize,orderBy,orderType);
				if(finIndex>imagesIds.size())
					listRes = imagesIds.subList(iniIndex, imagesIds.size());
				else
					listRes = imagesIds.subList(iniIndex, finIndex);
				totalNumberPages = (int) Utils.roundUp(imagesIds.size(),pageSize);
			}else{
				imagesIds = mediaMid.getAllMediaIds(appId, ModelEnum.image, pageNumber, pageSize, orderBy, orderType);
				listRes = imagesIds;
				totalNumberPages = mediaMid.countAllMedia(appId, ModelEnum.image) / pageSize;
			}

			ListResultSet res = new ListResultSet(listRes,pageNumber,totalNumberPages);

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
			Log.debug("", this, "getImageMetadata", "********Finding Image Meta**********");
			Image temp = null;
			if(MiddleLayerFactory.getAppsMiddleLayer().appExists(this.appId)){
				if(mediaMid.mediaExists(appId, ModelEnum.image, imageId)){
					temp = (Image)(mediaMid.getMedia(appId, ModelEnum.image, imageId));
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
			Log.debug("", this, "downloadImage", "*********Downloading Image**********");
			if (mediaMid.mediaExists(appId, ModelEnum.image, imageId)) {
				Image image = (Image)(mediaMid.getMedia(appId, ModelEnum.image, imageId));
				sucess = mediaMid.download(appId, ModelEnum.image, imageId,image.getFileExtension());
				if (sucess!=null){ 
					return Response.ok(sucess, MediaType.APPLICATION_OCTET_STREAM)
							.header("content-disposition","attachment; filename = "+image.getFileName()+"."+image.getFileExtension()).build();
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
