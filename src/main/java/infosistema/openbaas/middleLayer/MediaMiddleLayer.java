package infosistema.openbaas.middleLayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;

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
import infosistema.openbaas.dataaccess.files.FileInterface;
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
		return type+":"+id;
	}
	
	private Map<String, String> getFileFields(InputStream stream, FormDataContentDisposition fileDetail,
			String location, ModelEnum type) {
		
		String fullFileName = fileDetail.getFileName();
		int idx = fullFileName.lastIndexOf(".");
		String fileName = (idx < 0 ? fullFileName : fullFileName.substring(0, idx));
		String fileExtension = (idx < 0 ? "" : fullFileName.substring(idx + 1));
		String fileSize = "-1";
		Map<String, String> fields = new HashMap<String, String>();

		fields.put(Media.SIZE, fileSize);
		fields.put(Media.FILENAME, fileName);
		fields.put(Media.FILEEXTENSION, fileExtension);
		fields.put(Media.LOCATION, location);
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
		Map<String, String> fields = getFileFields(stream, fileDetail, location, type);
		
		//Upload File
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			filePath = fileModel.upload(appId, type, id, fields.get(Media.FILEEXTENSION), stream);
			File file = new File(filePath);
			fields.put(Media.PATH, filePath);
			fields.put(type+"Id", id);
			fields.put(Media.SIZE, String.valueOf(file.length()));
			
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
		if (location != null){
			String[] splitted = location.split(":");
			geo.insertObjectInGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), type, appId, id);
		}

		
		if (mediaModel.createMedia(appId, type, id, fields))
			return id;
		else 
			return null;
	}


	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	public boolean deleteMedia(String appId, ModelEnum type, String id) {
		String extension = mediaModel.getMediaField(appId, type, id, Media.FILEEXTENSION);
		String location = mediaModel.getMediaField(appId, type, id, Media.LOCATION);
		FileInterface fileModel = getAppFileInterface(appId);
		Boolean res = false;
		try{
			res = fileModel.deleteFile(appId, type, id, extension);
			
		}catch(NoSuchEntityException e){
			Log.error("", this, "deleteFile", "No such element error.", e); 
		}
		res = mediaModel.deleteMedia(appId, type, id);
				
		if (location != null){
			String[] splitted = location.split(":");
			geo.deleteObjectFromGrid(Double.parseDouble(splitted[0]),	Double.parseDouble(splitted[1]), type, appId, id);
		}
		
		return res ;
	}


	// *** GET LIST *** //

	@Override
	protected List<String> getAllSearchResults(String appId, String userId, String url, JSONObject query, String orderType, ModelEnum type) throws Exception {
		if(query==null){
			query = new JSONObject();
			JSONObject jAux= new JSONObject();
			jAux.put("$exists",1);
			query.put("fileExtension", jAux); 
			query.put("imageId", jAux);
			query.put("pixelsSize", jAux);
			query.put("fileName", jAux); 
		}
		return docModel.getDocuments(appId, userId, url, query, orderType);
		//return mediaModel.getMedia(appId, type, query, orderType);
	}
	
	
	// *** GET *** //
	public Media getMedia(String appId, ModelEnum type, String id) {
		Map<String, String> fields = mediaModel.getMedia(appId, type, id);
		fields.put(Media.ID, id);

		Media media = null;
		
		if (type == ModelEnum.audio) {
			media = new Audio();
			//((Audio)media).setDefaultBitRate(fields.get(Audio.BITRATE));
		} else if (type == ModelEnum.image) {
			media = new Image();
			//((Image)media).setResolution(fields.get(Image.RESOLUTION));
		} else if (type == ModelEnum.storage) {
			media = new Storage();
		} else if (type == ModelEnum.video) {
			media = new Video();
			//((Video)media).setResolution(fields.get(Video.RESOLUTION));
		}
		media.setId(fields.get(Media.ID));
		media.setSize(Long.parseLong(fields.get(Media.SIZE)));
		media.setDir(fields.get(Media.PATH));
		media.setFileName(fields.get(Media.FILENAME));
		media.setFileExtension(fields.get(Media.FILEEXTENSION));
		media.setLocation(fields.get(Media.LOCATION));

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

	public boolean mediaExists(String appId, ModelEnum type, String id) {
		return mediaModel.mediaExists(appId, type, id);
	}


	// *** OTHERS *** //

}
