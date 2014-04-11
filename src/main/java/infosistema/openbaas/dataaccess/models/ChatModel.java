package infosistema.openbaas.dataaccess.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.data.models.ChatRoom;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class ChatModel {

	// *** CONTRUCTORS *** //

	public ChatModel() {
		pool = new JedisPool(new JedisPoolConfig(), Const.getRedisChatServer(),Const.getRedisChatPort());
	}

	
	// *** PRIVATE *** //

	private JedisPool pool;

	private static final int MAXELEMS = 9999999;


	// *** CONSTANTS *** //

	// *** KEYS *** //
	
	private static final String MESSAGE_KEY_FORMAT = "%s:%s";
	private static final String CHATROOM_KEY_FORMAT = "%s:%s";
	private static final String CHATROOM2_KEY_FORMAT = "%s_%s";
	private static final String UNREAD_MSG_KEY_FORMAT = "%s_UnRead_%s";
	private static final String PARTICIPANTS_KEY_FORMAT = "%s:%s";
	private static final String USER_IN_PARTICIPANTS_LIST_1 = "%s:%s;*";
	private static final String USER_IN_PARTICIPANTS_LIST_2 = "%s:*;%s;*";
	private static final String USER_IN_PARTICIPANTS_LIST_3 = "%s:*;%s";
	
	private String getMessageKey(String appId, String msgId) {
		return String.format(MESSAGE_KEY_FORMAT, appId, msgId);
	}
	
	private String getChatRoomKey(String appId, String roomId) {
		return String.format(CHATROOM_KEY_FORMAT, appId, roomId);
	}
	
	private String getChatRoomKey_2(String appId, String roomId) {
		return String.format(CHATROOM2_KEY_FORMAT, appId, roomId);
	}
	
	private String getUnreadMsgKey(String appId, String userId) {
		return String.format(UNREAD_MSG_KEY_FORMAT, appId, userId);
	}
	
	private String getParticipantsKey(String appId, String participants) {
		return String.format(PARTICIPANTS_KEY_FORMAT, appId, participants);
	}
	
	private List<String> getKeysForUserInParticipantsList(String appId, String userId) {
		List<String> retObj = new ArrayList<String>();
		retObj.add(String.format(USER_IN_PARTICIPANTS_LIST_1, appId, userId));
		retObj.add(String.format(USER_IN_PARTICIPANTS_LIST_2, appId, userId));
		retObj.add(String.format(USER_IN_PARTICIPANTS_LIST_3, appId, userId));
		return retObj;
	}

	public Boolean createMsg(String appId, ChatMessage msg) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		long milliseconds = msg.getDate().getTime();
		try {
			String msgKey = getMessageKey(appId, msg.get_id()); 
			jedis.hset(msgKey, ChatMessage._ID, msg.get_id());
			jedis.hset(msgKey, ChatMessage.SENDER, msg.getSender());
			jedis.hset(msgKey, ChatMessage.DATE, String.valueOf(milliseconds));
			try{jedis.hset(msgKey, ChatMessage.MESSAGE_TEXT, msg.getMessageText());}catch(Exception e){}
			try{jedis.hset(msgKey, ChatMessage.FILE_TEXT, msg.getFileId());}catch(Exception e){}
			try{jedis.hset(msgKey, ChatMessage.AUDIO_TEXT, msg.getAudioId());}catch(Exception e){}
			try{jedis.hset(msgKey, ChatMessage.VIDEO_TEXT, msg.getVideoId());}catch(Exception e){}
			try{jedis.hset(msgKey, ChatMessage.IMAGE_TEXT, msg.getImageId());}catch(Exception e){}
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public ChatMessage getMsg(String appId, String msgId) {
		ChatMessage res = new ChatMessage();
		Jedis jedis = pool.getResource();
		try {
			String msgKey = getMessageKey(appId, msgId);
			String sender = jedis.hget(msgKey, ChatMessage.SENDER);
			String messageText = jedis.hget(msgKey, ChatMessage.MESSAGE_TEXT);
			String fileText = jedis.hget(msgKey, ChatMessage.FILE_TEXT);
			String audioText = jedis.hget(msgKey, ChatMessage.AUDIO_TEXT);
			String videoText = jedis.hget(msgKey, ChatMessage.VIDEO_TEXT);
			String imageText = jedis.hget(msgKey, ChatMessage.IMAGE_TEXT);

			if(sender!=null) res.setSender(sender);
			if(fileText!=null) res.setFileId(fileText);
			if(messageText!=null) res.setMessageText(messageText);
			if(audioText!=null) res.setAudioId(audioText);
			if(videoText!=null) res.setVideoId(videoText);
			if(imageText!=null) res.setImageId(imageText);
			res.setDate(new Date());
			try {
				long l = Long.valueOf(jedis.hget(msgKey, ChatMessage.DATE)).longValue();
				res.setDate(new Date(l));
			} catch (Exception e) {}
			res.set_id(msgId);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public Boolean createChatRoom(String appId, String msgId,String roomId, String roomName, String roomCreator, Boolean flagNotification, String totalParticipants) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		if(flagNotification==null) flagNotification = false;
		Long milliseconds = new Date().getTime();
		String[] participants = totalParticipants.split(Const.SEMICOLON);
		try {
			String roomKey = getChatRoomKey(appId, roomId);
			jedis.hset(roomKey, ChatRoom._ID, roomId);
			jedis.hset(roomKey, ChatRoom.ROOM_NAME, roomName);
			jedis.hset(roomKey, ChatRoom.ROOM_CREATOR, roomCreator);
			jedis.hset(roomKey, ChatRoom.FLAG_NOTIFICATION, flagNotification.toString());
			jedis.hset(roomKey, ChatRoom.PARTICIPANTS, totalParticipants);
			jedis.hset(roomKey, ChatRoom.CREATEDDATE, milliseconds.toString());
			jedis.rpush(getChatRoomKey_2(appId, roomId), msgId);
			if(participants.length==2)
				jedis.set(getParticipantsKey(appId, totalParticipants), roomId);
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public ChatRoom getChatRoom(String appId, String roomId) {
		ChatRoom res = new ChatRoom();
		Jedis jedis = pool.getResource();
		try {
			String roomKey = getChatRoomKey(appId, roomId);
			res.set_id(roomId);
			res.setRoomName(jedis.hget(roomKey, ChatRoom.ROOM_NAME));
			res.setRoomCreator(jedis.hget(roomKey, ChatRoom.ROOM_CREATOR));
			res.setFlagNotification(Boolean.parseBoolean(jedis.hget(roomKey, ChatRoom.FLAG_NOTIFICATION)));
			res.setParticipants(jedis.hget(roomKey, ChatRoom.PARTICIPANTS).split(Const.SEMICOLON));
			res.setCreatedDate(new Date());
			try {
				long l = Long.valueOf(jedis.hget(roomKey, ChatRoom.CREATEDDATE)).longValue();
				res.setCreatedDate(new Date(l));
			} catch (Exception e) {}

		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public Boolean addMsg2Room(String appId, String msgId,String roomId, List<String> unReadUsers) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		Iterator<String> it = unReadUsers.iterator();
		try {
			jedis.rpush(getChatRoomKey_2(appId, roomId), msgId);
			while(it.hasNext()){
				jedis.rpush(getUnreadMsgKey(appId, it.next()), msgId);
			}
			res = true;
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public List<String> getListParticipants(String appId, String roomId) {
		List<String> res = null;
		Jedis jedis = pool.getResource();
		try {
			String strParticipants = jedis.hget(getChatRoomKey(appId, roomId), ChatRoom.PARTICIPANTS);
			res = Utils.getListByString(strParticipants, ChatRoom.SEPARATOR);
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public List<ChatMessage> getMessageList(String appId, String roomId, Date date, Integer numberMessages, String orientation) {
		Jedis jedis = pool.getResource();
		List<ChatMessage> res = new ArrayList<ChatMessage>();
		if (orientation==null) orientation = "";
		if (numberMessages==null) numberMessages = 10;
		try {
			String roomKey2 = getChatRoomKey_2(appId, roomId);
			Long size = jedis.llen(roomKey2);		
			Integer endIndex = (int) (long) size;
			Integer startIndex = 1;
			int i = 0;
			int index = (int) (long)size;
			while(i<size){
				index = ((int)(long) (size))-i-1;
				i++;
				Date dateCurr = new Date();
				try {
					String msgIdCurr = jedis.lindex(roomKey2, index);
					long l = Long.valueOf(jedis.hget(getMessageKey(appId, msgIdCurr), ChatMessage.DATE)).longValue();
					dateCurr = new Date(l);
				} catch (Exception e) {}
				if(dateCurr.compareTo(date)==0 && orientation.equals("front")){
					index -= 1;
					break;
				}
				if(dateCurr.compareTo(date)==0 && !orientation.equals("front")){
					//index += 1;
					break;
				}
				if(dateCurr.compareTo(date)<0)
					break;
			}
			if(orientation.equals("front")){
				startIndex = index+1;
				if(index+numberMessages<endIndex)
					endIndex= index+numberMessages;
			}else{
				endIndex = index;
				startIndex = index-numberMessages+1;
				if(startIndex<1)
					startIndex=1;
			}
			if(startIndex>endIndex){
				return new ArrayList<ChatMessage>();
			}
			for(int o=startIndex;o<=endIndex;o++){
				String msgId = jedis.lindex(roomKey2, o);
				if(msgId!=null){
					ChatMessage msg = new ChatMessage();
					String msgKey = getMessageKey(appId, msgId); 
					String sender = jedis.hget(msgKey, ChatMessage.SENDER);
					String messageText = jedis.hget(msgKey, ChatMessage.MESSAGE_TEXT);
					String fileText = jedis.hget(msgKey, ChatMessage.FILE_TEXT);
					String audioText = jedis.hget(msgKey, ChatMessage.AUDIO_TEXT);
					String videoText = jedis.hget(msgKey, ChatMessage.VIDEO_TEXT);
					String imageText = jedis.hget(msgKey, ChatMessage.IMAGE_TEXT);

					if(sender!=null) msg.setSender(sender);
					if(fileText!=null) msg.setFileId(fileText);
					if(messageText!=null) msg.setMessageText(messageText);
					if(audioText!=null) msg.setAudioId(audioText);
					if(videoText!=null) msg.setVideoId(videoText);
					if(imageText!=null) msg.setImageId(imageText);
					String aux = jedis.hget(msgKey, ChatMessage.DATE);
					msg.setDate(new Date());
					try {
						long l = Long.valueOf(aux).longValue();
						msg.setDate(new Date(l));
					} catch (Exception e) {
					}
					msg.set_id(msgId);
					res.add(msg);
				}
			}
		}catch(Exception e){
			Log.error("", this, "getMessageList", "Error getMessageList redis.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public String existsChat(String participants, String appId) {
		Jedis jedis = pool.getResource();
		String res=null;
		try {
			res = jedis.get(getParticipantsKey(appId, participants));
		}catch(Exception e){
			Log.error("", this, "getMessageList", "Error getMessageList redis.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public int readMessages(String appId, String userId, JSONArray jsonArray) {
		Jedis jedis = pool.getResource();
		int res = 0;
		Long aux = (long) 0;
		try {
			if(jsonArray.length()>0){
				String unreadMsgKey = getUnreadMsgKey(appId, userId);
				for(int i = 0;i<jsonArray.length();i++){
					aux = jedis.lrem(unreadMsgKey, 0, jsonArray.getString(i));
					res += ((int)(long)aux);
				}
			}
		} catch (JSONException e) {
			Log.error("", this, "readMessages", "Error readMessages redis.", e); 
		} finally {
			pool.returnResource(jedis);
		}		
		return res;
	}

	public Boolean existsKey(String appId, String key) {
		Jedis jedis = pool.getResource();
		Boolean res = false;
		try {
			res = jedis.exists(getMessageKey(appId, key));		
		} catch (Exception e) {
			Log.error("", this, "existsKey", "Error existsKey redis: "+ getMessageKey(appId, key), e); 
		}finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public List<String> getTotalUnreadMsg(String appId, String userId) {
		Jedis jedis = pool.getResource();
		List<String> res = new ArrayList<String>();
		try {
			res = jedis.lrange(getUnreadMsgKey(appId, userId), 0, MAXELEMS);		
		} catch (Exception e) {
			Log.error("", this, "getTotalListElements", "Error getTotalListElements redis."+ res.size(), e); 
		}finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public Boolean hasNotification(String appId, String roomId) {
		Boolean res = false;
		Jedis jedis = pool.getResource();
		try {
			res = (Boolean.parseBoolean(jedis.hget(getChatRoomKey(appId, roomId), ChatRoom.FLAG_NOTIFICATION)));
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public List<String> getMessageChatroom(String appId, String roomId) {
		Jedis jedis = pool.getResource();
		List<String> res = new ArrayList<String>();
		try {
			res = jedis.lrange(getChatRoomKey_2(appId, roomId), 0, MAXELEMS);		
		} catch (Exception e) {
			Log.error("", this, "getMessageChatroom", "Error getMessageChatroom redis."+ res.size(), e); 
		}finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public List<String> getAllUserChats(String appId, String userId) {
		List<String> res = new ArrayList<String>();

		List<String> kList = getKeysForUserInParticipantsList(appId, userId); 
		Jedis jedis = pool.getResource();
		try {
			for (String k : kList) {
				Set<String> keys = jedis.keys(k);
				Iterator<String> i = keys.iterator();
				while (i.hasNext()) {
					String s = jedis.get(i.next());
					if (!res.contains(s)) res.add(s);
				}
			}
		}catch(Exception e){
			Log.error("", this, "getMessageList", "Error getMessageList redis.", e); 
		} finally {
			pool.returnResource(jedis);
		}
		return res;
	}

	public void updateMessageWithMedia(String appId, String messageId, ModelEnum type, String mediaId) { 
		Jedis jedis = pool.getResource();
		long milliseconds = new Date().getTime();
		try {
			String msgKey = getMessageKey(appId, messageId); 
			jedis.hset(msgKey, ChatMessage.DATE, String.valueOf(milliseconds));
			if(type.equals(ModelEnum.image)){
				jedis.hset(msgKey, ChatMessage.IMAGE_TEXT, mediaId);
			}
			if(type.equals(ModelEnum.audio)){
				jedis.hset(msgKey, ChatMessage.AUDIO_TEXT, mediaId);
			}
			if(type.equals(ModelEnum.video)){
				jedis.hset(msgKey, ChatMessage.VIDEO_TEXT, mediaId);
			}
			if(type.equals(ModelEnum.storage)){
				jedis.hset(msgKey, ChatMessage.FILE_TEXT, mediaId);
			}			
		}catch(Exception e){
			Log.error("", this, "updateMessageWithMedia", "Error updateMessageWithMedia redis.", e); 
		}finally {
			pool.returnResource(jedis);
		}		
	}

}
