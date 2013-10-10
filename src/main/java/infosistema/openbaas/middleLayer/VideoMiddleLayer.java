package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.models.Model;
import infosistema.openbaas.model.media.video.Video;
import infosistema.openbaas.model.media.video.VideoInterface;
import java.util.ArrayList;
import java.util.Map;

public class VideoMiddleLayer {

	// *** MEMBERS *** ///

	private Model model;
	private static final String VIDEOTYPE = "video";
	private static final String MEDIAFOLDER = "media";
	private static final String VIDEOFOLDER = "/media/video";

	
	// *** INSTANCE *** ///

	private static VideoMiddleLayer instance = null;

	protected static VideoMiddleLayer getInstance() {
		if (instance == null) instance = new VideoMiddleLayer();
		return instance;
	}

	private VideoMiddleLayer() {
		model = Model.getModel(); // SINGLETON
	}


	// *** CREATE *** ///

	public boolean uploadVideoFileToServerWithoutGeoLocation(String appId, String videoId, String fileType, String fileName) {
		String dir = "apps/" + appId + VIDEOFOLDER;
		boolean opOk = false;
		if (this.model.uploadFileToServer(appId, videoId, MEDIAFOLDER, VIDEOTYPE,dir, null, fileType,fileName))
			opOk = true;
		return opOk;
	}
	
	public boolean uploadVideoFileToServerWithGeoLocation(String appId, String videoId, String fileType, String fileName, String location){
		String dir = "apps/"+appId+VIDEOFOLDER;
		boolean opOk = false;
		if(this.model.uploadFileToServer(appId, videoId, MEDIAFOLDER, VIDEOFOLDER, dir, location, fileType, fileName))
			opOk = true;
		return opOk;
	}


	// *** UPDATE *** ///
	

	// *** DELETE *** ///

	public void deleteVideoInApp(String appId, String videoId) {
		this.model.deleteVideoInApp(appId, videoId, MEDIAFOLDER, VIDEOTYPE);
	}

	
	// *** GET *** ///
	
	public ArrayList<String> getAllVideoIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllVideoIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public VideoInterface getVideoInApp(String appId, String videoId) {
		Map<String, String> imageFields = this.model.getVideoInApp(appId,
				videoId);
		VideoInterface temp = new Video();

		for (Map.Entry<String, String> entry : imageFields.entrySet()) {
			if(entry.getKey().equalsIgnoreCase("location"))
				temp.setLocation(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("creationDate"))
				temp.setCreationDate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("dir"))
				temp.setDir(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("id"))
				temp.setId(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("size"))
				temp.setSize(Long.parseLong(entry.getValue()));
			else if (entry.getKey().equalsIgnoreCase("fileName")) 
				temp.setFileName(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("type"))
				temp.setType(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("resolution"))
				temp.setResolution(entry.getValue());
		}
		return temp;
	}

	// *** OTHERS *** ///

	public boolean videoExistsInApp(String appId, String videoId) {
		return this.model.videoExistsInApp(appId, videoId);
	}

	public byte[] downloadVideoInApp(String appId, String videoId,String ext) {
		return this.model.downloadVideoInApp(appId, videoId,ext);
	}

}
