package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.email.EmailInterface;
import infosistema.openbaas.dataaccess.email.Email;
import infosistema.openbaas.dataaccess.models.Model;
import infosistema.openbaas.dataaccess.sessions.RedisSessions;
import infosistema.openbaas.dataaccess.sessions.SessionInterface;
import infosistema.openbaas.model.media.audio.Audio;
import infosistema.openbaas.model.media.audio.AudioInterface;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;
import java.util.ArrayList;
import java.util.Map;

public class AudioMiddleLayer {

	// *** MEMBERS *** ///

	Model model;
	SessionInterface sessions;
	EmailInterface emailOp;
	private static final String AUDIOTYPE = "audio";
	private static final String MEDIAFOLDER = "media";
	private static final String AUDIOFOLDER = "/media/audio";
	private static final Utils utils = new Utils();

	// *** INSTANCE *** ///
	
	private static AudioMiddleLayer instance = null;

	protected static AudioMiddleLayer getInstance() {
		if (instance == null) instance = new AudioMiddleLayer();
		return instance;
	}
	
	private AudioMiddleLayer() {
		model = Model.getModel();
		sessions = new RedisSessions();
		emailOp = new Email();
	}

	// *** CREATE *** ///
	
	// *** UPDATE *** ///
	
	// *** DELETE *** ///
	
	// *** GET *** ///
	
	// *** OTHERS *** ///
	
	public String uploadAudioFileToServer(String appId, String fileDirectory, String location, String fileType,
			String fileName) {
		String audioId = utils.getRandomString(Const.IDLENGTH);
		if (this.model.uploadFileToServer(appId, audioId, MEDIAFOLDER, AUDIOTYPE, fileDirectory, location, fileType, fileName))
			return audioId;
		else {
			return null;
		}
	}

	public void deleteAudioInApp(String appId, String audioId) {
		this.model.deleteAudio(appId, audioId);
	}

	public ArrayList<String> getAllAudioIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllAudioIds(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public AudioInterface getAudioInApp(String appId, String audioId) {
		Map<String, String> audioFields = this.model.getAudioInApp(appId, audioId);
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

	public boolean audioExistsInApp(String appId, String audioId) {
		return this.model.audioExistsInApp(appId, audioId);
	}

	public byte[] downloadAudioInApp(String appId, String audioId,String ext) {
		return this.model.downloadAudioInApp(appId, audioId,ext);
	}

	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude,
			double longitude, double radius) {
		return model.getAllAudioIdsInRadius(appId, latitude, longitude, radius);
	}

	public boolean uploadAudioFileToServerWithoutGeoLocation(String appId, String audioId, String fileType,
			String fileName) {
		String dir = "apps/"+appId+AUDIOFOLDER;
		return this.model.uploadFileToServer(appId, audioId, MEDIAFOLDER, AUDIOTYPE,dir, null, fileType,fileName);
	}

	public boolean uploadAudioFileToServerWithGeoLocation(String appId, String videoId, String fileType, String fileName,
			String location){
		String dir = "apps/"+appId+AUDIOFOLDER;
		return this.model.uploadFileToServer(appId, videoId, MEDIAFOLDER, AUDIOFOLDER, dir, location, fileType, fileName);
	}

}
