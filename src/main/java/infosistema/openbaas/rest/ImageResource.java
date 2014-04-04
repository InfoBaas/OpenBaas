package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import java.util.Date;

import javax.ws.rs.Consumes;
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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;


import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


//apps/{appId}/media/images
public class ImageResource {

	private String appId;
	private MediaMiddleLayer mediaMid;
	private SessionMiddleLayer sessionMid;


	public ImageResource(String appId) {
		this.appId = appId;
		this.mediaMid = MediaMiddleLayer.getInstance();
		this.sessionMid = SessionMiddleLayer.getInstance();
	}

	// *** CREATE *** //

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
		Date startDate = Utils.getDate();
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "upload image", "********upload image ************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			Result res = mediaMid.createMedia(uploadedInputStream, fileDetail, appId, userId, ModelEnum.image, location, Metadata.getNewMetadata(location));
			if (res == null || res.getData() == null)
				response = Response.status(Status.BAD_REQUEST).entity(new Error(appId)).build();
			else
				response = Response.status(Status.OK).entity(res).build();
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		Date endDate = Utils.getDate();
		Log.info(sessionToken, this, "upload image", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
		return response;
	}

	
	// *** UPDATE *** //

	
	// *** DELETE *** //

	/**
	 * Deletes the video (from filesystem and database).
	 * 
	 * @param videoId
	 * @return
	 */
	@Path("{imageId}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteImage(@Context HttpHeaders hh, @PathParam("imageId") String imageId) {
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		Log.debug("", this, "del image", "********del image ************");
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		if (SessionMiddleLayer.getInstance().sessionTokenExists(sessionToken)) {
			Log.debug("", this, "deleteImage", "***********Deleting Image***********");
			if (mediaMid.mediaExists(appId, ModelEnum.image, imageId)) {
				if (this.mediaMid.deleteMedia(appId, ModelEnum.image, imageId))
					response = Response.status(Status.OK).entity("").build();
				else
					response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Del Meta")).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(new Error("Image not found")).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(new Error("sessionToken not found")).build();
		return response;
	}
	
	
	// *** GET LIST *** //

	/**
	 * Gets all the users in the application.
	 * 
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response find(@Context UriInfo ui, @Context HttpHeaders hh, @QueryParam("show") JSONArray arrayShow,
			@QueryParam("query") JSONObject query, @QueryParam(Const.RADIUS) String radiusStr,
			@QueryParam(Const.LAT) String latitudeStr, @QueryParam(Const.LONG) String longitudeStr,
			@QueryParam(Const.ELEM_COUNT) String pageCount, @QueryParam(Const.ELEM_INDEX) String pageIndex,
			@QueryParam(Const.PAGE_NUMBER) String pageNumberStr, @QueryParam(Const.PAGE_SIZE) String pageSizeStr, 
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_TYPE) String orderTypeStr) {
		QueryParameters qp = QueryParameters.getQueryParameters(appId, null, query, radiusStr, latitudeStr, longitudeStr, 
				pageNumberStr, pageSizeStr, orderByStr, orderTypeStr, ModelEnum.image,pageCount,pageIndex);
		Response response = null;
		Log.debug("", this, "get images list", "********get images list ************");
		if(pageNumberStr==null) pageNumberStr = "1";
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();

		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				ListResult res = mediaMid.find(qp,arrayShow);
				if(Integer.parseInt(pageNumberStr) <= res.getTotalnumberpages())
					response = Response.status(Status.OK).entity(res).build();
				else{
					response = Response.status(Status.NOT_FOUND).entity(new Error("Page not found.")).build();
				}
			} catch (Exception e) {
				response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
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
		
		StringBuilder buf = new StringBuilder();
		for(String header:hh.getRequestHeaders().keySet()){
			buf.append(header+" : "+hh.getRequestHeader(header));
			buf.append("\n");
		}
		
		//System. out.println(buf.toString());
		
		Response response = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			Log.debug("", this, "getImageMetadata", "********Finding Image Meta**********");
			if(AppsMiddleLayer.getInstance().appExists(this.appId)){
				if(mediaMid.mediaExists(appId, ModelEnum.image, imageId)){
					Result res = mediaMid.getMedia(appId, ModelEnum.image, imageId, true);
					response = Response.status(Status.OK).entity(res).build();
				}
				else{
					response = Response.status(Status.NOT_FOUND).entity(new Error("")).build();
				}
			}
			else{
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}
		}else if(code == -2){
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		}else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** DOWNLOAD *** //
	
	@Path("{imageId}/{quality}/download")
	@GET
	//@Produces({ MediaType.APPLICATION_OCTET_STREAM })
	public Response downloadImage(@PathParam("imageId") String imageId, @PathParam("quality") String quality,
			@QueryParam("bars") String bars, @Context UriInfo ui, @Context HttpHeaders hh) {
		Date startDate = Utils.getDate();
		Response response = null;
		byte[] sucess = null;
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		//int code=1;
		Log.debug("", this, "download image", "******** download image ************");
		if (code == 1) {
			String sessionToken = Utils.getSessionToken(hh);
			Log.debug("", this, "downloadImage", "*********Downloading Image**********");
			if (mediaMid.mediaExists(appId, ModelEnum.image, imageId)) {
				Image image = (Image)(mediaMid.getMedia(appId, ModelEnum.image, imageId, false).getData());
				sucess = mediaMid.download(appId, ModelEnum.image, imageId, image.getFileExtension(),quality,bars);
				if (sucess!=null){ 
					Date endDate = Utils.getDate();
					Log.info(sessionToken, this, "upload image", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
					return Response.ok(sucess, MediaType.APPLICATION_OCTET_STREAM)
							.header("content-disposition","attachment; filename = "+image.getFileName()+"."+image.getFileExtension()).build();
				}else{
					response = Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(new Error("Error downloading file.")).build();
				}
			} else
				response = Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(imageId).build();
		}else if(code == -2){
			response = Response.status(Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(new Error("Invalid Session Token.")).build();
		}else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(new Error("Error handling the request.")).build();
		return response;
	}

	// *** RESOURCES *** //

	// *** OTHERS *** //

}
