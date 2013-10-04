package rest_resources;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FilenameUtils;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.IdsResultSet;
import utils.Const;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

//@Path("/apps/{appId}/storage")
public class StorageResource {
	
	private String appId;
	private AppsMiddleLayer appsMid;
//	private static final int IDLENGTH = 3;

//	private final String storageUrl = "apps/" + appId + "/storage";

	public StorageResource() {
	}

	public StorageResource(AppsMiddleLayer appsMid, String appId) {
		this.appId = appId;
		this.appsMid = appsMid;
	}

	/*
	 * Returns a code corresponding to the sucess or failure Codes: -2 ->
	 * Forbidden -1 -> Bad request 1 -> sessionExists
	 */
	private int treatParameters(UriInfo ui, HttpHeaders hh) {
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
			} else {
				code = -2;
			}
		}
		return code;
	}

	//TODO: PAGINATION
	/**
	 * Gets all the storage Identifiers in the application.
	 * 
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAllStorageIds(@Context UriInfo ui,
			@Context HttpHeaders hh,
			@QueryParam("pageNumber") Integer pageNumber, @QueryParam("pageSize") Integer pageSize, 
			@QueryParam("orderBy") String orderBy, @QueryParam("orderType") String orderType ) {
		if (pageNumber == null) pageNumber = Const.PAGE_NUMBER;
		if (pageSize == null) 	pageSize = Const.PAGE_SIZE;
		if (orderBy == null) 	orderBy = Const.ORDER_BY;
		if (orderType == null) 	orderType = Const.ORDER_TYPE;
		Response response = null;
		int code = this.treatParameters(ui, hh);
		if (code == 1) {
			System.out.println("***********************************");
			System.out.println("********Finding all Storage********");
			ArrayList<String> storageIds = appsMid.getAllStorageIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
			IdsResultSet res = new IdsResultSet(storageIds,pageNumber);
			response = Response.status(Status.OK).entity(res).build();
		} else if (code == -2) {
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.").build();
		} else if (code == -1)
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		return response;
	}
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
				found = appsMid.downloadStorageInApp(appId, storageId,null);
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
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		builder.entity(found);
		builder.header("fileType", extension);
		return builder.build();
	}
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
	public Response uploadStorageFile(@Context UriInfo ui,
			@Context HttpHeaders hh,
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
			System.out.println("***********************************");
			System.out.println("*******Uploading to Storage********");
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
			String storageId = appsMid.createLocalFile(uploadedInputStream,
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
			//String file = dir + storageId + "." + fileType;
			this.appsMid.uploadStorageFileToServer(this.appId, storageId, fileType,
					fileName);
			response = Response.status(200).entity(fileNameWithType).build();
		}else if(code == -2)
			response = Response.status(Status.FORBIDDEN).entity("Invalid Session Token.")
			 .build();
		else if(code == -1)
			 response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.")
			 .build();
		return response;
	}
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
			@CookieParam(value = "sessionToken") String sessionToken) {
		Response response = null;
		if (appsMid.sessionTokenExists(sessionToken)) {
			System.out.println("************************************");
			System.out.println("***********Deleting Storage***********");
			if (appsMid.storageExistsInApp(appId, storageId)) {
				this.appsMid.deleteStorageInApp(appId, storageId);
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
