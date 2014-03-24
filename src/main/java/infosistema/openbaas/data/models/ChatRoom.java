package infosistema.openbaas.data.models;

import java.util.Date;


public class ChatRoom {

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
	
}
