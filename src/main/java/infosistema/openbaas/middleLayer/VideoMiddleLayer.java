package infosistema.openbaas.middleLayer;

import infosistema.openbaas.model.media.video.Video;
import infosistema.openbaas.model.media.video.VideoInterface;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;


import com.sun.jersey.core.header.FormDataContentDisposition;

public class VideoMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	private static final String VIDEOTYPE = "video";
	private static final String MEDIAFOLDER = "media";
	private static final String VIDEOFOLDER = "/media/video";

	
	// *** INSTANCE *** //

	private static VideoMiddleLayer instance = null;

	protected static VideoMiddleLayer getInstance() {
		if (instance == null) instance = new VideoMiddleLayer();
		return instance;
	}

	private VideoMiddleLayer() {
		super();
	}


	// *** CREATE *** //


	// *** UPDATE *** //
	

	// *** DELETE *** //

	public void deleteVideoInApp(String appId, String id) {
		String dir = getFileDirectory(appId, id, MEDIAFOLDER, VIDEOTYPE);
		deleteFile(dir);
		if (redisModel.videoExistsInApp(appId, id))
			redisModel.deleteVideoInApp(appId, id);
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.deleteVideoInApp(appId, id);
		} else {
			System.out.println("Database not implemented.");
		}
	}

	
	// *** GET LIST *** //

	
	// *** GET *** //
	
	public ArrayList<String> getAllVideoIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			return mongoModel.getAllVideoIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public VideoInterface getVideoInApp(String appId, String videoId) {
		Map<String, String> videoFields = redisModel.getVideoInApp(appId,
				videoId);
		if (videoFields == null || videoFields.size() == 0) {
			String dir = null;
			String type = null;
			String size = null;
			String resolution = null;
			String creationDate = null;
			String fileName = null;
			String location = null;
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				videoFields = mongoModel.getImageInApp(appId, videoId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : videoFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("dir"))
							dir = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("type"))
							type = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("size"))
							size = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("resolution"))
							resolution = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("fileName"))
							fileName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("location"))
							location = entry.getValue();
					}
					redisModel.createVideoInApp(appId, videoId, dir, type,
							size, resolution, creationDate, fileName, location);
				}
			} else {
				System.out.println("Database not implemented.");
			}
		} else {
			videoFields.put("id", videoId);
			videoFields.put("appId", appId);
		}

		VideoInterface temp = new Video();

		for (Map.Entry<String, String> entry : videoFields.entrySet()) {
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

	// *** UPLOAD *** //

	public String uploadVideo(InputStream uploadedInputStream, FormDataContentDisposition fileDetail, String appId, String location) {
		String fileNameWithType = null;
		String fileType = new String();
		String fileName = new String();
		boolean uploadOk = false;
		fileNameWithType = fileDetail.getFileName();
		char[] charArray = fileNameWithType.toCharArray();
		boolean pop = false;
		int i = 0;
		while (!pop) {
			fileName += charArray[i];
			if (charArray[i + 1] == '.')
				pop = true;
			i++;
		}
		for (int k = 0; k < charArray.length - 1; k++) {
			if (charArray[k] == '.') {
				for (int j = k + 1; j < charArray.length; j++)
					fileType += charArray[j];
			}
		}
		String dir = "apps/" + appId + "/media/video";
		String id = MiddleLayerFactory.getStorageMiddleLayer().createLocalFile(uploadedInputStream, fileDetail, appId, fileType, dir);
		uploadOk = uploadVideoFileToServerWithoutGeoLocation(appId, id, fileType, fileName);
		if(id != null && uploadOk)
			return id;
		else
			return null;
	}
	
	public boolean uploadVideoFileToServerWithoutGeoLocation(String appId, String videoId, String fileType, String fileName) {
		String dir = "apps/" + appId + VIDEOFOLDER;

		boolean opOk = false;
		if (uploadFileToServer(appId, videoId, MEDIAFOLDER, VIDEOTYPE,dir, null, fileType,fileName))
			opOk = true;
		return opOk;
	}
	
	public boolean uploadVideoFileToServerWithGeoLocation(String appId, String videoId, String fileType, String fileName, String location){
		String dir = "apps/"+appId+VIDEOFOLDER;
		boolean opOk = false;
		if(uploadFileToServer(appId, videoId, MEDIAFOLDER, VIDEOFOLDER, dir, location, fileType, fileName))
			opOk = true;
		return opOk;
	}

	// *** DOWNLOAD *** //

	public byte[] downloadVideoInApp(String appId, String videoId,String ext) {
		return download(appId, MEDIAFOLDER, VIDEO, videoId,ext);
	}


	// *** EXISTS *** //

	public boolean videoExistsInApp(String appId, String videoId) {
		if (redisModel.videoExistsInApp(appId, videoId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.videoExistsInApp(appId, videoId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	
	// *** OTHERS *** //

}
