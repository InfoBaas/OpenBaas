package infosistema.openbaas.middleLayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.sun.jersey.core.header.FormDataContentDisposition;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Audio;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.data.models.Media;
import infosistema.openbaas.data.models.Storage;
import infosistema.openbaas.data.models.Video;
import infosistema.openbaas.data.models.files.FileInterface;
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


	// *** PRIVATE *** //

	private String createFileId(String appId, ModelEnum type) {
		String id = Utils.getRandomString(Const.getIdLength());
		while (mediaModel.mediaExists(appId, type, id)) {
			id = Utils.getRandomString(Const.getIdLength());
		}
		return id;
	}
	
	private Map<String, String> getFileFields(InputStream stream, FormDataContentDisposition fileDetail, ModelEnum type) {
		
		String fullFileName = fileDetail.getFileName();
		int idx = fullFileName.lastIndexOf(".");
		String fileName = (idx < 0 ? fullFileName : fullFileName.substring(0, idx));
		String fileExtension = (idx < 0 ? "" : fullFileName.substring(idx + 1));
		String fileSize = "";

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			IOUtils.copy(stream, out);
			fileSize = "" + out.size();
		} catch (Exception e) { }

		Map<String, String> fields = new HashMap<String, String>();

		fields.put(Audio.SIZE, fileSize);
		fields.put(Audio.FILENAME, fileName);
		fields.put(Audio.FILEEXTENSION, fileExtension);
		//TODO: SACAR do STREAM A INFORMAÇÃO AQUI A BAIXO
		if (type == ModelEnum.audio) {
			fields.put(Audio.BITRATE, Const.getAudioDegaultBitrate());
		} else if (type == ModelEnum.image) {
			fields.put(Image.SIZE, fileSize);
			fields.put(Image.RESOLUTION, Const.getImageDefaultSize());
			fields.put(Image.PIXELSIZE, Const.getImageDefaultSize());
		} else if (type == ModelEnum.video) {
			fields.put(Video.RESOLUTION, Const.getVideoDefaultResolution());
		} else if (type == ModelEnum.storage) {
		}
		return fields;
	}

	
	// *** CREATE *** //

	public String createMedia(InputStream stream, FormDataContentDisposition fileDetail, String appId,
			ModelEnum type, String location) {

		String id = createFileId(appId, type);
		
		///// OLD
		String filePath = "";
		///// OLD /////

		// Get data from file
		Map<String, String> fields = getFileFields(stream, fileDetail, type);
		
		//Upload File
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			filePath = fileModel.upload(appId, type, id, fields.get(Media.FILEEXTENSION), stream);
			fields.put(Media.PATH, filePath);
		} catch(AmazonServiceException e) {
			Log.error("", this, "upload", "Amazon Service error.", e);
			return null;
		} catch(AmazonClientException e) {
			Log.error("", this, "upload", "Amazon Client error.", e); 
			return null;
		} catch(Exception e) {
			Log.error("", this, "upload", "An error ocorred.", e); 
			return null;
		}
		
		if (mediaModel.createMedia(appId, ModelEnum.audio, id, fields))
			return id;
		else 
			return null;
	}


	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	public boolean deleteMedia(String appId, ModelEnum type, String id) {
		String extension = mediaModel.getMediaField(appId, type, id, Media.FILEEXTENSION);
		FileInterface fileModel = getAppFileInterface(appId);
		boolean ok = false;
		try{
			ok = fileModel.deleteFile(appId, type, id, extension);
		}catch(NoSuchEntityException e){
			Log.error("", this, "deleteFile", "No such element error.", e); 
		}
		return mediaModel.deleteMedia(appId, type, id) && ok;
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
			((Audio)media).setDefaultBitRate(fields.get(Audio.BITRATE));
		} else if (type == ModelEnum.image) {
			media = new Image();
			((Image)media).setResolution(fields.get(Image.RESOLUTION));
		} else if (type == ModelEnum.storage) {
			media = new Storage();
		} else if (type == ModelEnum.video) {
			media = new Video();
			((Video)media).setResolution(fields.get(Video.RESOLUTION));
		}
		media.setId(fields.get(Media.ID));
		media.setSize(Long.parseLong(fields.get(Media.SIZE)));
		media.setDir(fields.get(Media.PATH));
		media.setFileName(fields.get(Media.FILENAME));

		return media;
	}

	// *** DOWNLOAD *** //

	public byte[] download(String appId, ModelEnum type, String id,String ext) {
		FileInterface fileModel = getAppFileInterface(appId);
		try {
			return fileModel.download(appId, type, id,ext);
		} catch (IOException e) {
			Log.error("", this, "download", "An error ocorred.", e); 
		}
		return null;
	}

	// *** EXISTS *** //



	// *** EXISTS *** //

	public boolean mediaExists(String appId, ModelEnum type, String id) {
		return mediaModel.mediaExists(appId, type, id);
	}



	// *** OTHERS *** //
	
	// *** OTHERS *** //
	
	public Integer countAllMedia(String appId, ModelEnum type) {
		return mediaModel.countAllMedia(appId, type);
	}



}
