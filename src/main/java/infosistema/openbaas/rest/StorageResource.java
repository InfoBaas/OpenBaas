/*****************************************************************************************
Infosistema - OpenBaas
Copyright(C) 2002-2014 Infosistema, S.A.
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
www.infosistema.com
info@openbaas.com
Av. José Gomes Ferreira, 11 3rd floor, s.34
Miraflores
1495-139 Algés Portugal
****************************************************************************************/
package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

//@Path("/apps/{appId}/storage")
public class StorageResource {
	
	private String appId;
	private MediaMiddleLayer mediaMid;
	private SessionMiddleLayer sessionMid;

	public StorageResource() {
	}

	public StorageResource(String appId) {
		this.appId = appId;
		this.mediaMid = MediaMiddleLayer.getInstance();
		this.sessionMid = SessionMiddleLayer.getInstance();
	}

	// *** CREATE *** //
	
	// *** UPDATE *** //
	
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
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail, @PathParam(Const.APP_ID) String appId, @HeaderParam(value = Const.LOCATION) String location,
			@FormDataParam(Const.MESSAGEID) String messageId) {
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			Result res = mediaMid.createMedia(uploadedInputStream, fileDetail, appId, userId, ModelEnum.storage,location, Metadata.getNewMetadata(location),messageId);
			if (res == null || res.getData() == null) 
				response = Response.status(Status.BAD_REQUEST).entity(appId).build();
			else
				response = Response.status(Status.OK).entity(res).build();
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
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		if (SessionMiddleLayer.getInstance().sessionTokenExists(sessionToken)) {
			if (mediaMid.mediaExists(appId, ModelEnum.storage, storageId)) {
				if(mediaMid.deleteMedia(appId, ModelEnum.storage, storageId))
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
			@QueryParam(Const.ORDER_BY) String orderByStr, @QueryParam(Const.ORDER_BY) String orderTypeStr) {
		QueryParameters qp = QueryParameters.getQueryParameters(appId, null, query, radiusStr, latitudeStr, longitudeStr, 
				pageNumberStr, pageSizeStr, orderByStr, orderTypeStr, ModelEnum.storage,pageCount,pageIndex);
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();

		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			try {
				ListResult res = mediaMid.find(qp,arrayShow);
				response = Response.status(Status.OK).entity(res).build();
			} catch (Exception e) {
				response = Response.status(Status.FORBIDDEN).entity(e.getMessage()).build();
			}
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
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
	public Response downloadStorageUsingId(@PathParam("storageId") final String storageId, @PathParam("quality") String quality,
			@Context UriInfo ui, @Context HttpHeaders hh) {
		ResponseBuilder builder = Response.status(Status.OK);
		if (!sessionMid.checkAppForToken(Utils.getSessionToken(hh), appId))
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
				found = mediaMid.download(appId, ModelEnum.storage, storageId, null,quality,null);
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
