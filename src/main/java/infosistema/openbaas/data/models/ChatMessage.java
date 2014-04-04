package infosistema.openbaas.data.models;

import infosistema.openbaas.utils.Log;

import java.util.Date;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ChatMessage {

	private String _id;
	private Date date;
	private String sender;
	private String messageText;
	private String fileId;
	private String imageId;
	private String audioId;
	private String videoId;
	private Boolean read;
	
	public final static String ORIENTATION = "orientation";
	public final static String MSGSLIST = "msgsList";
	public final static String MESSAGE_TEXT = "messageText";
	public final static String IMAGE_TEXT = "imageId";
	public final static String AUDIO_TEXT = "audioId";
	public final static String VIDEO_TEXT = "videoId";
	public final static String FILE_TEXT = "fileId";
	public final static String ROOM_ID = "roomId";
	public final static String PARTICIPANTS = "participants";
	public final static String DATE = "date";
	public final static String SENDER = "sender";
	public final static String _ID = "_id";
	public final static String NUM_MSG = "numberMessages";
	
	
	public ChatMessage(){
		
	}
	
	public ChatMessage(String _id, Date date, String sender,
			String messageText, String fileId, String imageId, String audioId,
			String videoId) {
		super();
		this._id = _id;
		this.date = date;
		this.sender = sender;
		this.messageText = messageText;
		this.fileId = fileId;
		this.imageId = imageId;
		this.audioId = audioId;
		this.videoId = videoId;
	}

	public String get_id() {
		return _id;
	}
	
	public void set_id(String _id) {
		this._id = _id;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public String getSender() {
		return sender;
	}
	
	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getAudioId() {
		return audioId;
	}

	public void setAudioId(String audioId) {
		this.audioId = audioId;
	}

	public String getVideoId() {
		return videoId;
	}

	public void setVideoId(String videoId) {
		this.videoId = videoId;
	}

	public Boolean getRead() {
		return read;
	}

	public void setRead(Boolean read) {
		this.read = read;
	}

	public JSONObject serialize() {
		JSONObject retObj = new JSONObject();
		try {
			if (_id != null) retObj.put(_ID, _id);
			if (date != null) retObj.put(DATE, "" + date.getTime());
			if (sender != null) retObj.put(SENDER, sender);
			if (messageText != null) retObj.put(MESSAGE_TEXT, messageText);
			if (fileId != null) retObj.put(FILE_TEXT, fileId);
			if (imageId != null) retObj.put(IMAGE_TEXT, imageId);
			if (audioId != null) retObj.put(AUDIO_TEXT, audioId);
			if (videoId != null) retObj.put(VIDEO_TEXT, videoId);
		} catch (JSONException e) {
			Log.error("", this, "serialize", "Error Serializing Chat", e);
		}
		return retObj;
	}
	
}
