package infosistema.openbaas.data.models;

import infosistema.openbaas.utils.Log;

import java.io.Serializable;
import java.util.Date;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


public class ChatRoom implements Serializable{

	private String _id;
	private String roomName;
	private Boolean flagNotification;
	private String roomCreator;
	private String[] participants;
	private Date createdDate;
	private Integer unreadMessages = 0;
	
	public final static String CREATEDDATE = "createdDate";
	public final static String FLAG_NOTIFICATION = "flagNotification";
	public final static String PARTICIPANTS = "participants";
	public final static String ROOM_NAME = "roomName";
	public final static String ROOM_CREATOR = "roomCreator";
	public final static String _ID = "_id";
	public final static String SEPARATOR = ";";

	public ChatRoom() {
	}
	
	public ChatRoom(String _id, String roomName, Boolean flagNotification) {
		this._id = _id;
		this.roomName = roomName;
		this.flagNotification = flagNotification;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public Boolean getFlagNotification() {
		return flagNotification;
	}

	public void setFlagNotification(Boolean flagNotification) {
		this.flagNotification = flagNotification;
	}

	public String getRoomCreator() {
		return roomCreator;
	}

	public void setRoomCreator(String roomCreator) {
		this.roomCreator = roomCreator;
	}

	public String[] getParticipants() {
		return participants;
	}

	public void setParticipants(String[] participants) {
		this.participants = participants;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Integer getUnreadMessages() {
		return unreadMessages;
	}

	public void setUnreadMessages(Integer unreadMessages) {
		this.unreadMessages = unreadMessages;
	}
	
	public JSONObject serialize() {
		JSONObject retObj = new JSONObject();
		try {
			if (_id != null) retObj.put(_ID, _id);
			if (roomName != null) retObj.put(ROOM_NAME, roomName);
			if (flagNotification != null) retObj.put(FLAG_NOTIFICATION, flagNotification);
			if (roomCreator != null) retObj.put(ROOM_CREATOR, roomCreator);
			if (participants != null) {
				for (int i = 0; i < participants.length; i++)
					retObj.accumulate(PARTICIPANTS, participants[i]);
			}
			if (createdDate != null) retObj.put(CREATEDDATE, "" + createdDate.getTime());
		} catch (JSONException e) {
			Log.error("", this, "serialize", "Error Serializing Chat", e);
		}
		return retObj;
	}

}
