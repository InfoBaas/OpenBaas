package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FilenameUtils;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

//@Path("/apps/{appId}/storage")
public class StorageResource {
	
	private String appId;
	private MediaMiddleLayer mediaMid;
	private SessionMiddleLayer sessionsMid;

	public StorageResource() {
	}

	public StorageResource(String appId) {
		this.appId = appId;
		this.mediaMid = MiddleLayerFactory.getMediaMiddleLayer();
		this.sessionsMid = MiddleLayerFactory.getSessionMiddleLayer();
	}

	// *** CREATE *** //
	
	// *** UPDATE *** //
	
	//TODO: LOCATION
	/**
	 * Uploads a storage file, storage files are stored in a different folder
	 * than media files due to the simplicity of these files. Media has advanced
	 * options (streaming, ect). Storage has simple options (download, upload).
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response uploadStorageFile(@Context UriInfo ui, @Context HttpHeaders hh, @FormDataParam(Const.FILE) InputStream uploadedInputStream,
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail, @PathParam("appId") String appId, @HeaderParam(value = Const.LOCATION) String location) {
		Response response = null;
		Cookie sessionToken=null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.SESSION_TOKEN))
				sessionToken = new Cookie(Const.SESSION_TOKEN, entry.getValue().get(0));
		}
		String userId = sessionsMid.getUserIdUsingSessionToken(sessionToken.getValue());
		if(Utils.getAppIdFromToken(sessionToken.getValue(), userId)!=appId)
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			String storageId = mediaMid.createMedia(uploadedInputStream, fileDetail, appId, ModelEnum.storage, location);
			if (storageId == null) { 
				response = Response.status(Status.BAD_REQUEST).entity(appId).build();
			} else {
				String metaKey = "apps."+appId+".media.storage."+storageId;
				Metadata meta = mediaMid.createMetadata(metaKey, userId, location);
				Result res = new Result(storageId, meta);
				response = Response.status(Status.OK).entity(res).build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}
	

	// *** DELETE *** //
	
	/**
	 * Deletes the Storage file(from filesystem and database).
	 * 
	 * @param videoId
	 * @return
	 */
	@Path("{storageId}")
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deleteStorageFile(@PathParam("storageId") String storageId,
			@HeaderParam(value = Const.SESSION_TOKEN) String sessionToken) {
		Response response = null;
		String userId = sessionsMid.getUserIdUsingSessionToken(sessionToken);
		if(Utils.getAppIdFromToken(sessionToken, userId)!=appId)
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		if (MiddleLayerFactory.getSessionMiddleLayer().sessionTokenExists(sessionToken)) {
			if (mediaMid.mediaExists(appId, ModelEnum.storage, storageId)) {
				this.mediaMid.deleteMedia(appId, ModelEnum.storage, storageId);
				
				String metaKey = "apps."+appId+".media.storage."+storageId;
				Boolean meta = mediaMid.deleteMetadata(metaKey);
				if(meta)
					response = Response.status(Status.OK).entity("").build();
				else
					response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error("Del Meta")).build();
			} else
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
		} else
			response = Response.status(Status.FORBIDDEN).entity(new Error(sessionToken)).build();
		return response;
	}


	// *** GET LIST *** //
	
	/**
	 * Gets all the storage Identifiers in the application.
	 * 
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllStorageIds(@Context UriInfo ui,
			@Context HttpHeaders hh,
			@QueryParam(Const.PAGE_NUMBER) Integer pageNumber, @QueryParam(Const.PAGE_SIZE) Integer pageSize, 
			@QueryParam(Const.ORDER_BY) String orderBy, @QueryParam(Const.ORDER_BY) String orderType ) {
		if (pageNumber == null) pageNumber = Const.getPageNumber();
		if (pageSize == null) 	pageSize = Const.getPageSize();
		if (orderBy == null) 	orderBy = Const.getOrderBy();
		if (orderType == null) 	orderType = Const.getOrderType();
		Response response = null;
		Cookie sessionToken=null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.SESSION_TOKEN))
				sessionToken = new Cookie(Const.SESSION_TOKEN, entry.getValue().get(0));
		}
		String userId = sessionsMid.getUserIdUsingSessionToken(sessionToken.getValue());
		if(Utils.getAppIdFromToken(sessionToken.getValue(), userId)!=appId)
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			Log.debug("", this, "findAllStorageIds", "********Finding all Storage********");
			ArrayList<String> storageIds = mediaMid.getAllMediaIds(appId, ModelEnum.storage, pageNumber,
					pageSize,orderBy,orderType);
			ListResult res = new ListResult(storageIds,pageNumber);
			response = Response.status(Status.OK).entity(res).build();
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}
	

	// *** GET *** //

	// *** UPLOAD *** //

	// *** DOWNLOAD *** //

	/*Attention! A file octet-stream has no explicit file type, this has to be changed to inform the client of 
	*file type so that he creates it automaticallyor assume that the client finds it out using 
	*the url (this is what I do atm).
	*/
	@GET
	@Path("{storageId}")
	@Produces("application/octet-stream")
	public Response downloadStorageUsingId(@PathParam("storageId") final String storageId,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		ResponseBuilder builder = Response.status(Status.OK);
		Cookie sessionToken=null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.SESSION_TOKEN))
				sessionToken = new Cookie(Const.SESSION_TOKEN, entry.getValue().get(0));
		}
		String userId = sessionsMid.getUserIdUsingSessionToken(sessionToken.getValue());
		if(Utils.getAppIdFromToken(sessionToken.getValue(), userId)!=appId)
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		byte[] found = null;
		String extension = "";
		try {
			/*
			 * pseudo codigo if(!existe em local storage) buscar a aws else
			 * retornar directo
			 */
			
			File dir =new File("apps/" + appId + "/storage/");
			String[] myFiles = dir.list(new FilenameFilter() {
                public boolean accept(File directory, String fileName) {
                    if(fileName.lastIndexOf(".")==-1) return false;
                    if((fileName.substring(0, fileName.lastIndexOf("."))).equals(storageId))
                        return true;
                    return false;
                }
            });
			String url = "apps/" + appId + "/storage/" + myFiles[0];
			File file = new File(url);
			extension = FilenameUtils.getExtension(url);
			if (!file.exists()) {
				found = mediaMid.download(appId, ModelEnum.storage, storageId, null);
			} else if(file.exists()){
				FileInputStream fis = new FileInputStream(file);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				for (int readNum; (readNum = fis.read(buf)) != -1;) {
					bos.write(buf, 0, readNum);
				}
				found = bos.toByteArray();
				fis.close();
			}
			else{
				builder.status(Status.NOT_FOUND);
			}
		} catch (IOException e) {
			Log.error("", this, "downloadStorageUsingId", "An error ocorred.", e); 
		}
		builder.entity(found);
		builder.header("fileType", extension);
		return builder.build();
	}

	// *** RESOURCES *** //
	
	// *** OTHERS *** //
	
}
