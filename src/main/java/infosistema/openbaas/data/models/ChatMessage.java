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
	private String hasFile;
	private String hasImage;
	private String hasAudio;
	private String hasVideo;
	private String roomId;
	private Boolean read;
	
	public final static String ORIENTATION = "orientation";
	public final static String MSGSLIST = "msgsList";
	public final static String MESSAGE_TEXT = "messageText";
	public final static String IMAGE_ID = "imageId";
	public final static String AUDIO_ID = "audioId";
	public final static String VIDEO_ID = "videoId";
	public final static String FILE_ID = "fileId";
	public static final String HAS_IMAGE = "hasImage";
	public static final String HAS_VIDEO = "hasVideo";
	public static final String HAS_FILE = "hasFile";
	public static final String HAS_AUDIO = "hasAudio";
	public final static String ROOM_ID = "roomId";
	public final static String PARTICIPANTS = "participants";
	public final static String DATE = "date";
	public final static String SENDER = "sender";
	public final static String _ID = "_id";
	public final static String NUM_MSG = "numberMessages";
	
	
	public ChatMessage(){
		
	}
	
	public ChatMessage(String _id, Date date, String sender, String roomId, String messageText, String fileId, String imageId, 
			String audioId, String videoId, String hasFile, String hasImage, String hasAudio, String hasVideo) {
		super();
		this._id = _id;
		this.date = date;
		this.sender = sender;
		this.messageText = messageText;
		this.fileId = fileId;
		this.imageId = imageId;
		this.audioId = audioId;
		this.videoId = videoId;
		this.hasFile = hasFile;
		this.hasImage = hasImage;
		this.hasAudio = hasAudio;
		this.hasVideo = hasVideo;
		this.roomId = roomId;
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

	public String getHasFile() {
		return hasFile;
	}

	public void setHasFile(String hasFile) {
		this.hasFile = hasFile;
	}

	public String getHasImage() {
		return hasImage;
	}

	public void setHasImage(String hasImage) {
		this.hasImage = hasImage;
	}

	public String getHasAudio() {
		return hasAudio;
	}

	public void setHasAudio(String hasAudio) {
		this.hasAudio = hasAudio;
	}

	public String getHasVideo() {
		return hasVideo;
	}

	public void setHasVideoId(String hasVideo) {
		this.hasVideo = hasVideo;
	}

	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomId) {
		this.roomId = roomId;
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
			if (fileId != null) retObj.put(FILE_ID, fileId);
			if (imageId != null) retObj.put(IMAGE_ID, imageId);
			if (audioId != null) retObj.put(AUDIO_ID, audioId);
			if (videoId != null) retObj.put(VIDEO_ID, videoId);
			if (hasFile != null) retObj.put(HAS_FILE, hasFile);
			if (hasImage != null) retObj.put(HAS_IMAGE, hasImage);
			if (hasAudio != null) retObj.put(HAS_AUDIO, hasAudio);
			if (hasVideo != null) retObj.put(HAS_VIDEO, hasVideo);
		} catch (JSONException e) {
			Log.error("", this, "serialize", "Error Serializing Chat", e);
		}
		return retObj;
	}
	
}
