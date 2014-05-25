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
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.data.models.Media;
import infosistema.openbaas.data.models.Storage;
import infosistema.openbaas.dataaccess.models.NotificationsModel;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.middleLayer.NotificationMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import java.util.Map;

import javapns.devices.Device;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONObject;


import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


public class APNSResource {
	
	private String appId;
	private SessionMiddleLayer sessionMid;
	private NotificationMiddleLayer noteMid;
	private AppsMiddleLayer appMid;
	private MediaMiddleLayer mediaMid;

	public APNSResource(String appId) {
		this.appId = appId;
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.noteMid = NotificationMiddleLayer.getInstance();
		this.appMid = AppsMiddleLayer.getInstance();
		this.mediaMid = MediaMiddleLayer.getInstance();

	}
	
	@POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createCertificate(@Context UriInfo ui, @Context HttpHeaders hh,
			@FormDataParam(Application.APNS_PASSWORD) String APNSPassword,
			@FormDataParam(Application.APNS_CLIENT_ID) String clientId,
			@FormDataParam(Const.FILE) InputStream fileInputStream, 
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail) {
		Response response = null;
		String certificatePath;
		String sessionToken = Utils.getSessionToken(hh);
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParametersAdmin(ui, hh);
		if (code == 1) {
			try {
				if(APNSPassword!=null && clientId != null && fileInputStream != null){
					Result res = mediaMid.createMedia(fileInputStream, fileDetail, appId, userId, ModelEnum.storage, null, null,null);
					if(res!=null){
						Media media = (Storage)res.getData();
						certificatePath = media.getDir();
						Certificate certificate = appMid.createCertificate(appId, certificatePath,APNSPassword,clientId);
						response = Response.status(Status.OK).entity(certificate).build();
					}
					else
						throw new Exception("Error creating certificate");
				}
				else
					throw new Exception("Error creating certificate");
			} catch (Exception e) {
				Log.error("", this, "createCertificate", "Error creating certificate.", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error creating certificate!").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		return response;
	}
	
	@POST
	@Path("/register")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response registerDeviceToken(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		String deviceToken = null;
		String client = null;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				deviceToken = inputJsonObj.getString(NotificationsModel.DEVICETOKEN);
				client = inputJsonObj.getString(NotificationsModel.CLIENTID);
			} catch (Exception e) {
				Log.error("", this, "registerDeviceToken", "Error parsing json in registerDeviceToken", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
			try {
				Map<String, Device> res = noteMid.addDeviceToken(appId, userId, client, deviceToken);
				NotificationMiddleLayer.getInstance().pushBadge(appId, userId, null);
				response = Response.status(Status.OK).entity(res).build();				
			} catch (Exception e) {
				Log.error("", this, "registerDeviceToken", "Error registerDeviceToken", e); 
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error in registerDeviceToken.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		return response;
	}
	
	@POST
	@Path("/unregister")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response unRegisterDeviceToken(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		String deviceToken = null;
		String client = null;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		//String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				deviceToken = inputJsonObj.getString(NotificationsModel.DEVICETOKEN);
				client = inputJsonObj.getString(NotificationsModel.CLIENTID);
			} catch (Exception e) {
				Log.error("", this, "unRegisterDeviceToken", "Error parsing json in unRegisterDeviceToken", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
			try {
				Boolean res = noteMid.remDeviceToken(appId,client,deviceToken);
				response = Response.status(Status.OK).entity(res).build();				
			} catch (Exception e) {
				Log.error("", this, "unRegisterDeviceToken", "Error registerDeviceToken", e); 
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error in unRegisterDeviceToken.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		return response;
	}
}
