package infosistema.openbaas.middleLayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.sun.jersey.core.header.FormDataContentDisposition;

import infosistema.openbaas.data.ModelEnum;
import infosistema.openbaas.data.models.Audio;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.data.models.Media;
import infosistema.openbaas.data.models.Storage;
import infosistema.openbaas.data.models.Video;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

public class MediaMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	private MediaModel mediaModel;


	// *** INSTANCE *** //
	
	private static MediaMiddleLayer instance = null;
	
	private MediaMiddleLayer() {
		super();
		mediaModel = new MediaModel();
	}
	
	public static MediaMiddleLayer getInstance() {
		if (instance == null) instance = new MediaMiddleLayer();
		return instance;
	}
	
	// *** CREATE *** //
	
	//XPTO LOCATION
	protected boolean createMedia(String appId, String id, Map<String, String> fields, String location) {
		return mediaModel.createMedia(appId, ModelEnum.audio, id, fields);
	}

	// *** UPDATE *** //
	
	public boolean uploadFileToServer(String appId, String id, ModelEnum type, String location, String fileDirectory, String fileName, String extension) {
		boolean upload = false;
		boolean databaseOk = false;
		String fileSize = "";
		String filePath = fileDirectory + id + "." + extension;
		File fileToUpload = new File(filePath);
		upload = upload(appId, filePath, id, fileToUpload);
		fileSize = "" + fileToUpload.length();
		databaseOk = false;
		if (upload) {
			Map<String, String> fields = new HashMap<String, String>();
			if (type == ModelEnum.audio) {
				fields.put(Media.DIR, filePath);
				fields.put(Media.TYPE, extension);
				fields.put(Audio.BITRATE, MINIMUMBITRATE);
				fields.put(Audio.SIZE, fileSize);
				fields.put(Audio.CREATIONDATE, new Date().toString());
				fields.put(Audio.FILENAME, fileName);
			} else if (type == ModelEnum.image) {
				fields.put(Image.DIR, filePath);
				fields.put(Image.TYPE, type.toString());
				fields.put(Image.SIZE, fileSize);
				fields.put(Image.RESOLUTION, SMALLIMAGE);
				fields.put(Image.CREATIONDATE, new Date().toString());
				fields.put(Image.PIXELSIZE, SMALLIMAGE);
				fields.put(Image.FILENAME, fileName);
			} else if (type == ModelEnum.video) {
				fields.put(Media.DIR, filePath);
				fields.put(Media.TYPE, type.toString());
				fields.put(Media.SIZE, fileSize);
				fields.put(Video.RESOLUTION, SMALLRESOLUTION);
				fields.put(Media.CREATIONDATE, new Date().toString());
				fields.put(Media.FILENAME, fileName);
			} else if (type == ModelEnum.storage) {
				fields.put(Media.DIR, filePath);
				fields.put(Media.TYPE, extension);
				fields.put(Media.SIZE, fileSize);
				fields.put(Media.CREATIONDATE, new Date().toString());
				fields.put(Media.FILENAME, fileName);
			}
			databaseOk = createMedia(appId, id, fields, location);
		}
		// Finalizing
		if (upload && databaseOk)
			return true;
		return false;
	}

	// *** DELETE *** //
	
	public boolean deleteMedia(String appId, ModelEnum type, String id) {
		String fileDirectory = getFilePath(appId, id, type);
		deleteFile(fileDirectory);
		return mediaModel.deleteMedia(appId, type, id);
	}


	// *** GET LIST *** //
	
	public ArrayList<String> getAllMediaIds(String appId, ModelEnum type, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return mediaModel.getAllMediaIds(appId, type, pageNumber, pageSize, orderBy, orderType);
	}

	//XPTO:
	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude, double longitude, double radius) {
		try {
			return docModel.getAllAudioIdsInRadius(appId, latitude, longitude, radius);
		} catch (Exception e) {
			Log.error("", this, "getAllAudioIdsInRadius", "An error ocorred.", e); 
			return null;
		}
	}

	//XPTO: Not Like this
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude, double longitude, double radius,
			Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		try{
			return docModel.getAllImagesIdsInRadius(appId, latitude, longitude, radius,pageNumber,pageSize,orderBy,orderType);
		} catch (Exception e) {
			Log.error("", this, "getAllImagesIdsInRadius", "An error ocorred.", e); 
			return null;
		}
	}

	// *** GET *** //
	
	public Media getMedia(String appId, ModelEnum type, String id) {
		Map<String, String> fields = mediaModel.getMedia(appId, ModelEnum.video, id);
		fields.put(Media.ID, id);

		Media media = null;
		
		if (type == ModelEnum.audio) {
			media = new Audio();
			media.setId(fields.get(Video.ID));
			((Audio)media).setAudioType(fields.get(Audio.TYPE));
			media.setSize(Long.parseLong(fields.get(Audio.SIZE)));
			((Audio)media).setDefaultBitRate(fields.get(Audio.BITRATE));
			media.setDir(fields.get(Audio.DIR));
			media.setCreationDate(fields.get(Audio.CREATIONDATE));
			media.setFileName(fields.get(Audio.FILENAME));
		} else if (type == ModelEnum.image) {
			media = new Image();
			media.setCreationDate(fields.get(Image.CREATIONDATE));
			media.setDir(fields.get(Image.DIR));
			media.setId(fields.get(Image.ID));
			media.setSize(Long.parseLong(fields.get(Image.SIZE)));
			media.setFileName(fields.get(Image.FILENAME));
			((Image)media).setImageType(fields.get(Image.TYPE));
			((Image)media).setResolution(fields.get(Image.RESOLUTION));
		} else if (type == ModelEnum.storage) {
			media = new Storage();
			media.setId(fields.get(Storage.ID));
			media.setSize(Long.parseLong(fields.get(Storage.SIZE)));
			media.setDir(fields.get(Storage.DIR));
			media.setFileName(fields.get(Storage.FILENAME));
		} else if (type == ModelEnum.video) {
			media = new Video();
			media.setCreationDate(fields.get(Video.CREATIONDATE));
			media.setDir(fields.get(Video.DIR));
			media.setId(fields.get(Video.ID));
			media.setSize(Long.parseLong(fields.get(Video.SIZE)));
			media.setFileName(fields.get(Video.FILENAME));
			((Video)media).setType(fields.get(Video.TYPE));
			((Video)media).setResolution(fields.get(Video.RESOLUTION));
		}
		return media;
	}

	// *** UPLOAD *** //

	public String uploadMedia(InputStream uploadedInputStream, FormDataContentDisposition fileDetail,
			String appId, ModelEnum type, String location) {
		String fullFileName = fileDetail.getFileName();
		int idx = fullFileName.lastIndexOf(".");
		String fileName = (idx < 0 ? fullFileName : fullFileName.substring(0, idx));
		String fileExtension = (idx < 0 ? "" : fullFileName.substring(idx + 1));
		boolean uploadOk = false;
		String fileDirectory = "apps/"+appId+"/media/" + type.toString() + "/";
		String id = createLocalFile(uploadedInputStream, fileDetail, appId, fileDirectory, fileExtension);
		uploadOk = uploadFileToServer(appId, id, ModelEnum.image, location, fileDirectory, fileName, fileExtension); 
		if(id != null && uploadOk)
			return id;
		else
			return null;
	}
	
	public String createLocalFile(InputStream uploadedInputStream,FormDataContentDisposition fileDetail, String appId, String fileDirectory, String extension) {
		String id = Utils.getRandomString(Const.IDLENGTH);
		File dirFolders = new File(fileDirectory);
		dirFolders.mkdirs();
		File f = new File(fileDirectory + id + "." + extension);
		while (f.exists()) {
			id = Utils.getRandomString(Const.IDLENGTH);
			f = new File(fileDirectory + id);
		}
		try {
			OutputStream out = new FileOutputStream(f);
			IOUtils.copy(uploadedInputStream, out);
		} catch (FileNotFoundException e) {
			Log.error("", this, "createLocalFile", "File not found.", e); 
		} catch (IOException e) {
			Log.error("", this, "createLocalFile", "An error ocorred.", e); 
		}

		return id;
	}

	// *** DOWNLOAD *** //

	// *** EXISTS *** //

	public boolean mediaExists(String appId, ModelEnum type, String id) {
		return mediaModel.mediaExists(appId, type, id);
	}

	
	// *** OTHERS *** //
	
	public Integer countAllMedia(String appId, ModelEnum type) {
		return mediaModel.countAllMedia(appId, type);
	}

	protected String getFilePath(String appId, String id, ModelEnum type) {
		String dir = null;
		MediaModel mediaModel = new MediaModel(); 
		dir = mediaModel.getMediaField(appId, type, id, Audio.DIR);
		return dir;
	}

}
