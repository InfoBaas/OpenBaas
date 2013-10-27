package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.email.EmailInterface;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.sessions.RedisSessions;
import infosistema.openbaas.dataaccess.sessions.SessionInterface;
import infosistema.openbaas.model.media.audio.Audio;
import infosistema.openbaas.model.media.audio.AudioInterface;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

public class AudioMiddleLayer extends MediaMiddleLayer {

	// *** MEMBERS *** //

	SessionInterface sessions;
	EmailInterface emailOp;
	private static final String AUDIOTYPE = "audio";
	private static final String MEDIAFOLDER = "media";
	private static final String AUDIOFOLDER = "/media/audio";

	
	// *** INSTANCE *** //
	
	private static AudioMiddleLayer instance = null;

	protected static AudioMiddleLayer getInstance() {
		if (instance == null) instance = new AudioMiddleLayer();
		return instance;
	}
	
	private AudioMiddleLayer() {
		sessions = new RedisSessions();
		emailOp = new Email();
	}

	// *** CREATE *** //
	
	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	public void deleteAudioInApp(String appId, String audioId) {
		String fileDirectory = getFileDirectory(appId, audioId, MEDIAFOLDER, AUDIO);
		deleteFile(fileDirectory);
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.deleteAudioInApp(appId, audioId);
			if (redisModel.audioExistsInApp(appId, audioId)) {
				redisModel.deleteAudioInApp(appId, audioId);
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	// *** GET LIST *** //

	public ArrayList<String> getAllAudioIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.getAllAudioIds(appId,pageNumber,pageSize,orderBy,orderType);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude, double longitude, double radius) {
		try {
			return docModel.getAllAudioIdsInRadius(appId, latitude, longitude, radius);
		} catch (Exception e) {
			return null;
		}
	}

	// *** GET *** //
	
	public AudioInterface getAudioInApp(String appId, String audioId) {
		Map<String, String> audioFields = redisModel.getAudioInApp(appId, audioId);

		if (audioFields == null || audioFields.size() == 0) {
			String dir = null;
			String type = null;
			String size = null;
			String bitRate = null;
			String fileName = null;
			String creationDate = null;
			String location = null;
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				audioFields = mongoModel.getAudioInApp(appId, audioId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : audioFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("dir"))
							dir = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("type"))
							type = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("size"))
							size = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("bitRate"))
							bitRate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("fileName"))
							fileName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("location"))
							location = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
					}
					redisModel.createAudioInApp(appId, audioId, dir, type,
							size, bitRate, creationDate, fileName, location);
				} else {
					System.out.println("Warning: Cache is full.");
				}
			} else {
				System.out.println("Database not implemented.");
			}
		} else {
			audioFields.put("id", audioId);
			audioFields.put("appId", appId);
		}

		AudioInterface temp = new Audio(audioId);
		for (Map.Entry<String, String> entry : audioFields.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("type"))
				temp.setAudioType(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("size"))
				temp.setSize(Long.parseLong(entry.getValue()));
			else if (entry.getKey().equalsIgnoreCase("defaultBitRate"))
				temp.setDefaultBitRate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("dir"))
				temp.setDir(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("creationDate"))
				temp.setCreationDate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("fileName")) 
				temp.setFileName(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("location"))
				temp.setLocation(entry.getValue());
		}
		return temp;
	}


	// *** UPLOAD *** //

	public boolean uploadAudioFileToServerWithoutGeoLocation(String appId, String audioId, String fileType, String fileName) {
		String dir = "apps/"+appId+AUDIOFOLDER;
		return uploadFileToServer(appId, audioId, MEDIAFOLDER, AUDIOTYPE,dir, null, fileType,fileName);
	}

	public boolean uploadAudioFileToServerWithGeoLocation(String appId, String videoId, String fileType, String fileName,
			String location){
		String dir = "apps/"+appId+AUDIOFOLDER;
		return uploadFileToServer(appId, videoId, MEDIAFOLDER, AUDIOFOLDER, dir, location, fileType, fileName);
	}


	// *** DOWNLOAD *** //
	
	public byte[] downloadAudioInApp(String appId, String audioId, String ext) {
		return download(appId, MEDIAFOLDER, AUDIO, audioId,ext);
	}

	
	// *** EXISTS *** //

	public boolean audioExistsInApp(String appId, String audioId) {
		if (redisModel.audioExistsInApp(appId, audioId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.audioExistsInApp(appId, audioId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	
	// *** OTHERS *** //
	
}
