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
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.data.models.ChatRoom;
import infosistema.openbaas.data.models.Media;
import infosistema.openbaas.middleLayer.ChatMiddleLayer;
import infosistema.openbaas.middleLayer.MediaMiddleLayer;
import infosistema.openbaas.middleLayer.NotificationMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;


public class ChatResource {
	
	private String appId;
	private SessionMiddleLayer sessionMid;
	private MediaMiddleLayer mediaMid;
	private ChatMiddleLayer chatMid;
	private NotificationMiddleLayer noteMid;

	public ChatResource(String appId) {
		this.appId = appId;
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.mediaMid = MediaMiddleLayer.getInstance();
		this.chatMid = ChatMiddleLayer.getInstance();
		this.noteMid = NotificationMiddleLayer.getInstance();
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response createChatRoom(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		String roomName;		
		boolean flag=false;
		JSONArray participants=null;
		Boolean flagNotification=false;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				roomName = (String) inputJsonObj.opt(ChatRoom.ROOM_NAME);
				participants = (JSONArray) inputJsonObj.get(ChatRoom.PARTICIPANTS);
				flagNotification =  inputJsonObj.optBoolean(ChatRoom.FLAG_NOTIFICATION);
				for(int i = 0; i < participants.length(); i++){
					String userCurr = participants.getString(i);
					if(userCurr.equals(userId))
						flag = true;
				}
				if(!flag) participants.put(userId);
				if(roomName==null){
					roomName = Utils.getStringByJSONArray(participants,";");
				}
				ChatRoom chatRoom = chatMid.createChatRoom(appId, roomName, userId, flagNotification, participants);
				response = Response.status(Status.OK).entity(chatRoom).build();				
			} catch (Exception e) {
				Log.error("", this, "createChatRoom", "Error creating chat.", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		return response;
	}
	
	@POST
	@Path("/{roomid}/getmessages")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getMessages(JSONObject inputJsonObj, @Context UriInfo ui, 
			@Context HttpHeaders hh, @PathParam("roomid") String roomId) {
		Response response = null;
		Date date;
		String orientation;
		Integer numberMessages;
		List<ChatMessage> res = new ArrayList<ChatMessage>();
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				Long l = (Long)inputJsonObj.get(ChatMessage.DATE);
				date = new Date(l);
				orientation = inputJsonObj.optString(ChatMessage.ORIENTATION);
				numberMessages =  inputJsonObj.optInt(ChatMessage.NUM_MSG);	
				
				res = chatMid.getMessages(appId, userId, roomId, date, orientation, numberMessages);
				response = Response.status(Status.OK).entity(res).build();
			} catch (JSONException e) {
				Log.error("", this, "createUserAndLogin", "Error parsing the JSON.", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		return response;
	}

	@POST
	@Path("/{roomid}/sendmessage")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response sendMessage(@PathParam("roomid") String roomId, @Context UriInfo ui, @Context HttpHeaders hh,
			@FormDataParam(Const.MESSAGE) String message,
			@FormDataParam(Const.IMAGE) InputStream imageInputStream, 
			@FormDataParam(Const.IMAGE) FormDataContentDisposition imageDetail,
			@FormDataParam(Const.VIDEO) InputStream videoInputStream, 
			@FormDataParam(Const.VIDEO) FormDataContentDisposition videoDetail,
			@FormDataParam(Const.AUDIO) InputStream audioInputStream, 
			@FormDataParam(Const.AUDIO) FormDataContentDisposition audioDetail,
			@FormDataParam(Const.FILE) InputStream fileInputStream, 
			@FormDataParam(Const.FILE) FormDataContentDisposition fileDetail,
			@HeaderParam(value = Const.LOCATION) String location) {
		JSONObject inputJsonObj= new JSONObject();
		if(message!=null){
			try {
				inputJsonObj.put(ChatMessage.MESSAGE_TEXT,URLDecoder.decode(message,"UTF-8"));
			} catch (JSONException e) {
				Log.error("", this, "sendMessage", "Error in message.", e);
				return Response.status(Status.BAD_REQUEST).entity(new Error("Error in message")).build();
			} catch (UnsupportedEncodingException e) {
				Log.error("", this, "sendMessage", "Error in decoding message.", e);
				return Response.status(Status.BAD_REQUEST).entity(new Error("Error in decoding message")).build();
			}
		}
		Response response = null;
		String messageText;
		String fileId;
		String imageId;
		String audioId;
		String videoId;
		String hasFile = null;
		String hasImage = null;
		String hasAudio = null;
		String hasVideo = null;
		ModelEnum flag = null;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if(chatMid.existsChatRoom(appId,roomId)){
				try {
					Result res = null;
					if (imageInputStream!=null && imageDetail!=null) {
						res = mediaMid.createMedia(imageInputStream, imageDetail, appId, userId, ModelEnum.image, location, Metadata.getNewMetadata(location),null);
						flag = ModelEnum.image;
						hasImage = "true";
					} else if (videoInputStream!=null && videoDetail!=null) {
						res = mediaMid.createMedia(videoInputStream, videoDetail, appId, userId, ModelEnum.video, location, Metadata.getNewMetadata(location),null);
						flag = ModelEnum.video;
						hasVideo = "true";
					} else if (audioInputStream!=null && audioDetail!=null) {
						res = mediaMid.createMedia(audioInputStream, audioDetail, appId, userId, ModelEnum.audio, location, Metadata.getNewMetadata(location),null);
						flag = ModelEnum.audio;
						hasAudio = "true";
					} else if (fileInputStream!=null && fileDetail!=null) {
						res = mediaMid.createMedia(fileInputStream, fileDetail, appId, userId, ModelEnum.storage, location, Metadata.getNewMetadata(location),null);
						flag = ModelEnum.storage;
						hasFile = "true";
					}
					if (res!=null && flag!=null) {
						String fId = ((Media)res.getData()).get_id();
						if(flag.equals(ModelEnum.image)) inputJsonObj.put(ChatMessage.IMAGE_ID, fId);
						if(flag.equals(ModelEnum.storage))inputJsonObj.put(ChatMessage.FILE_ID, fId);
						if(flag.equals(ModelEnum.audio))inputJsonObj.put(ChatMessage.AUDIO_ID, fId);
						if(flag.equals(ModelEnum.video))inputJsonObj.put(ChatMessage.VIDEO_ID, fId);
					}
					imageId = inputJsonObj.optString(ChatMessage.IMAGE_ID);
					audioId = inputJsonObj.optString(ChatMessage.AUDIO_ID);
					videoId = inputJsonObj.optString(ChatMessage.VIDEO_ID);
					fileId = inputJsonObj.optString(ChatMessage.FILE_ID);
					messageText = inputJsonObj.optString(ChatMessage.MESSAGE_TEXT);
					ChatMessage msg = chatMid.sendMessage(appId, userId, roomId, messageText, fileId, imageId, audioId, videoId, hasFile, hasImage, hasAudio, hasVideo);
					if(msg!=null){
						response = Response.status(Status.OK).entity(msg).build();
						noteMid.setPushNotificationsTODO(appId, userId, roomId);
					}else{
						throw new Exception("Error sendMessage");
					}
				} catch (Exception e) {
					Log.error("", this, "sendMessage", "Error sendMessage.", e); 
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error sendMessage").build();
				}
			}else{
				return Response.status(Status.NOT_FOUND).entity("Chat Room not found").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		return response;
	}
	
	@POST
	@Path("/{roomid}/readmessages")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	public Response readMessages(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh, 
			@PathParam("roomid") String roomId) {
		Response response = null;
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				JSONArray jsonArray = inputJsonObj.getJSONArray(ChatMessage.MSGSLIST);
				if(jsonArray.length()>0){
					chatMid.readMsgsFromUser(appId,userId,jsonArray);
				}
				response = Response.status(Status.OK).build();
				noteMid.pushBadge(appId, userId, roomId);
			} catch (Exception e) {
				Log.error("", this, "readMessages", "Error in readMessages.", e);
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		return response;
	}
	
	@GET
	@Path("/{roomid}/unreadmessages")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response unReadMessages(@Context UriInfo ui, @Context HttpHeaders hh, @PathParam("roomid") String roomId) {
		Response response = null;
		List<ChatMessage> lisRes = new ArrayList<ChatMessage>();
		String sessionToken = Utils.getSessionToken(hh);
		if (!sessionMid.checkAppForToken(sessionToken, appId))
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		String userId = sessionMid.getUserIdUsingSessionToken(sessionToken);
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			try {
				lisRes = chatMid.getUnreadMsgs(appId, userId, roomId);
				response = Response.status(Status.OK).entity(lisRes).build();
			} catch (Exception e) {
				Log.error("", this, "createUserAndLogin", "Error parsing the JSON.", e); 
				return Response.status(Status.BAD_REQUEST).entity("Error parsing the JSON.").build();
			}
		} else if(code == -2) {
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		} else if(code == -1){
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		}
		return response;
	}
	
}
