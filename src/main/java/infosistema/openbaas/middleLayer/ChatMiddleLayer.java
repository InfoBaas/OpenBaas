package infosistema.openbaas.middleLayer;

import infosistema.openbaas.comunication.bound.Outbound;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.data.models.ChatRoom;
import infosistema.openbaas.dataaccess.models.ChatModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;

public class ChatMiddleLayer extends MiddleLayerAbstract{

	// *** MEMBERS *** //
	private ChatModel chatModel;

	// *** INSTANCE *** //
	private static ChatMiddleLayer instance = null;
	
	private ChatMiddleLayer() {
		super();
		chatModel = new ChatModel();
	}
	
	public static ChatMiddleLayer getInstance() {
		if (instance == null) instance = new ChatMiddleLayer();
		return instance;
	}
	
	@Override
	protected List<DBObject> getAllSearchResults(String appId, String userId,
			String url, Double latitude, Double longitude, Double radius,
			JSONObject query, String orderType, String orderBy, ModelEnum type,
			List<String> toShow) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public ChatRoom createChatRoom(String appId, String roomName, String userIdCriador, Boolean flagNotification, JSONArray participants) {
		ChatRoom res = null;
		String existChatRoom = null;
		if(participants.length()>1){
			//ordenar JsonArray;
			participants = orderJsonArray(participants);
			if(participants.length()==2){
				existChatRoom = chatModel.existsChat(Utils.getStringByJSONArray(participants, ";"),appId);
			}
			if(existChatRoom != null){
				res = chatModel.getChatRoom(appId, existChatRoom);
				List<ChatMessage> lstMsg = getUnreadMsgs(appId, userIdCriador, existChatRoom);
				res.setUnreadMessages(lstMsg.size());
			}else{
				try {
					String strParticipants = Utils.getStringByJSONArray(participants,";");
					String messageId = "Msg_EMPTY";
					String roomId = "Chat_"+Utils.getRandomString(Const.getIdLength());
					
					ChatMessage msg = new ChatMessage(messageId, new Date(), userIdCriador, roomId, "", "", "", "", "", "", "", "", "");
					Boolean msgStorage = chatModel.createMessage(appId, msg);
					Boolean msgRoomStorage = chatModel.createChatRoom(appId, messageId, roomId, roomName, userIdCriador, flagNotification,strParticipants);
					
					if(msgRoomStorage && msgStorage)
						res = chatModel.getChatRoom(appId, roomId);;
				} catch (Exception e) {
					Log.error("", this, "createChatRoom", "Error ocorred.", e); 
				}
			}
		}else{
			return null;
		}
		return res;
	}

	public ChatRoom getChatRoom(String appId, String roomId) {
		return chatModel.getChatRoom(appId, roomId);
	}

	private JSONArray orderJsonArray(JSONArray participants) {
		JSONArray res = null;
		try {
			String[] array = new String[participants.length()];
			for(int i =0; i<participants.length();i++){
					array[i] = participants.getString(i);	
			}
			Arrays.sort(array);
			res = new JSONArray();
			for(int i =0; i<array.length;i++){
				res.put(array[i]);
			}
		} catch (Exception e) {
			Log.error("", this, "orderJsonArray", "Error ocorred.", e); 
		}
		return res;
	}
	
	public ChatMessage sendMessage(String appId, String messageId) {
		ChatMessage message = chatModel.getMessage(appId, messageId);
		String roomId = message.getRoomId(); 
		String sender = message.getSender();
		List<String> participants = chatModel.getListParticipants(appId, roomId);
		try {
			List<String> listUsers = new ArrayList<String>();
			Iterator<String> it = participants.iterator();
			while(it.hasNext()){
				String curr = it.next();
				if(!curr.equals(sender)){
					listUsers.add(curr);
				}
			}
			List<String> unReadUsers = new ArrayList<String>();
			for (String userId: listUsers) {
				Outbound outbound = Outbound.getUserOutbound(userId);
				if (outbound == null || !outbound.sendRecvMessage(appId, roomId, message, userId))
					unReadUsers.add(userId);
			}
			//Boolean addMsgRoom = chatModel.addMessage2Room(appId, message.get_id(), roomId, unReadUsers);
		} catch (Exception e) {
			Log.error("", this, "createChatRoom", "Error parsing the JSON.", e); 
		}
		return message;
	}
			
	public ChatMessage sendMessage(String appId, String sender, String roomId, String messageText, String fileId,
			String imageId, String audioId, String videoId, String hasFile, String hasImage, String hasAudio,
			String hasVideo) {
		ChatMessage res = null;
		List<String> participants = new ArrayList<String>();
		participants = chatModel.getListParticipants(appId, roomId);
		if(participants.size()>0 && participants!=null){
			try {
				String messageId = "Msg_"+Utils.getRandomString(Const.getIdLength());
				ChatMessage msg = new ChatMessage(messageId, new Date(), sender, roomId, messageText, fileId, imageId, audioId, videoId, hasFile, hasImage, hasAudio, hasVideo);
				Boolean msgStorage = chatModel.createMessage(appId, msg);
				Boolean addMsgRoom = chatModel.addMessage2Room(appId, messageId, roomId, sender, participants);
				for (String userId: participants) {
					if (sender == null || !sender.equals(userId)) {
						Outbound outbound = Outbound.getUserOutbound(userId);
						if (outbound != null) outbound.sendRecvMessage(appId, roomId, msg, userId);
					}
				}
				if (addMsgRoom && msgStorage)
					res = msg;
			} catch (Exception e) {
				Log.error("", this, "sendMessage", "Error parsing the JSON.", e); 
			}
		}else{
			return null;
		}
		return res;
	}

	public List<ChatMessage> getMessages(String appId, String userId, String roomId, Date date, String orientation, Integer numberMessages) {
		
		List<ChatMessage> res = new ArrayList<ChatMessage>();
		List<String> unreadMsg = new ArrayList<String>();
		try {
			unreadMsg = chatModel.getTotalUnreadMsg(appId, userId);
			res = chatModel.getMessageList(appId, roomId, date, numberMessages, orientation);
			Iterator<ChatMessage> it = res.iterator();
			while(it.hasNext()){
				ChatMessage msg = it.next();
				if(unreadMsg.contains(msg.get_id()))
					msg.setRead(false);
				else
					msg.setRead(true);
			}
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		return res;
	}

	public Boolean readMsgsFromUser(String appId, String userId, JSONArray jsonArray) {
		Boolean res = false;
		try {
			int num = chatModel.readMessages(appId,userId, jsonArray);
			if(num==jsonArray.length()){
				res =true;
			}
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		return res;
	}

	public Boolean existsChatRoom(String appId, String roomId) {
		Boolean res = false;		
		try {
			res = chatModel.existsKey(appId,roomId);
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		return res;
	}
	
	public List<ChatMessage> getUnreadMsgs(String appId, String userId, String roomId) {
		List<ChatMessage> res = new ArrayList<ChatMessage>();		
		int i=0;
		try {
			List<String> msgList = chatModel.getTotalUnreadMsg(appId, userId);
			Log.error("", this, "getUnreadMsgs", "getUnreadMsgs ###1 totalUnRead:"+msgList.size()); 
			List<String> list = chatModel.getMessageChatroom(appId, roomId);
			Log.error("", this, "getUnreadMsgs", "getUnreadMsgs ###1 totalChatRoom:"+msgList.size());
			Iterator<String> it = msgList.iterator();
			while(it.hasNext()){
				String messageId = it.next();
				if(list.contains(messageId)) {
					ChatMessage msg = chatModel.getMessage(appId, messageId);
					Log.error("", this, "getUnreadMsgs", "getUnreadMsgs ###1 addMsg:"+(i++));
					res.add(msg);
				}
			}
		} catch (Exception e) {
			Log.error("", this, "getMessages", "Error parsing the JSON.", e); 
		}
		Log.error("", this, "getUnreadMsgs", "getUnreadMsgs ###1 returnMsgSize:"+res.size());
		return res;
	}

	public void updateMessageWithMedia(String appId, String messageId, ModelEnum type, String mediaId) { 
		chatModel.updateMessageWithMedia(appId, messageId, type, mediaId);
		sendMessage(appId, messageId);
	}
	
}
