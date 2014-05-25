/*****************************************************************************************
Infosistema - OpenBaas
Copyright(C) 2002-2014 Infosistema, S.A.
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
www.infosistema.com
info@openbaas.com
Av. José Gomes Ferreira, 11 3rd floor, s.34
Miraflores
1495-139 Algés Portugal
****************************************************************************************/
package infosistema.openbaas.middleLayer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.mongodb.DBObject;
import com.sun.jersey.core.header.FormDataContentDisposition;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Audio;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.data.models.Media;
import infosistema.openbaas.data.models.Storage;
import infosistema.openbaas.data.models.Video;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.dataaccess.models.ModelAbstract;
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

	public Result createMedia(InputStream stream, FormDataContentDisposition fileDetail, String appId, String userId,
			ModelEnum type, String location, Map<String, String> extraMetadata, String messageId) {

		String id = createFileId(appId, type);
		
		///// OLD
		String filePath = "";
		///// OLD /////

		// Get data from file
		Map<String, String> fields = getFileFields(stream, fileDetail, location, type);
		
		//Upload File
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			byte[] bytes = IOUtils.toByteArray(stream);
			InputStream is1 = new ByteArrayInputStream(bytes); 
			filePath = fileModel.upload(appId, type, id, fields.get(Media.FILEEXTENSION), is1);
			int size = bytes.length;
			//File file = new File(filePath);
			fields.put(Media.PATH, filePath);
			fields.put(type+"Id", id);
			fields.put(Media.SIZE, String.valueOf(size));
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
		
		Metadata metadata = null;
		Object data = null;
		data = mediaModel.createMedia(appId, userId, type, id, fields, extraMetadata);
		if (data != null) {
			try {
				metadata = Metadata.getMetadata(new JSONObject(((JSONObject) data).getString(ModelAbstract._METADATA)));
			} catch (JSONException e) {
				Log.error("", this, "createMedia", "Error gettin metadata.", e);
			}
			((JSONObject) data).remove(ModelAbstract._METADATA);
			
			((JSONObject) data).remove(ModelAbstract._TYPE);
			
			Media media = null;
			if (type == ModelEnum.audio) {
				media = new Audio();
				//((Audio)media).setDefaultBitRate(obj.get(Audio.BITRATE));
			} else if (type == ModelEnum.image) {
				media = new Image();
				//((Image)media).setResolution(obj.get(Image.RESOLUTION));
			} else if (type == ModelEnum.storage) {
				media = new Storage();
			} else if (type == ModelEnum.video) {
				media = new Video();
				//((Video)media).setResolution(obj.get(Video.RESOLUTION));
			}
			
			try {
				media.set_id(((JSONObject) data).getString(ModelAbstract._ID));
				((JSONObject) data).remove(ModelAbstract._ID);
				media.setSize(((JSONObject) data).getLong(Media.SIZE));
				media.setDir(((JSONObject) data).getString(Media.PATH));
				media.setFileName(((JSONObject) data).getString(Media.FILENAME));
				media.setFileExtension(((JSONObject) data).getString(Media.FILEEXTENSION));
				if(((JSONObject) data).has(Media.LOCATION))
					media.setLocation(((JSONObject) data).getString(Media.LOCATION));
				//data = (DBObject)JSON.parse(data.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(messageId != null){
				ChatMiddleLayer.getInstance().updateMessageWithMedia(appId, messageId, type, media.get_id());
			}
			return new Result(media, metadata);
		} else {
			return null;
		}
	}
	
	public Boolean createLog(InputStream stream, FormDataContentDisposition fileDetail, String appId,
			String userId) {
        String date = Utils.printDate(new Date());
		String id = appId + userId + date;
		Boolean res = false;
		///// OLD
		String filePath = "";
		///// OLD /////

		// Get data from file
		Map<String, String> fields = getFileFields(stream, fileDetail, null, ModelEnum.log);
		
		//Upload File
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			filePath = fileModel.upload(appId, ModelEnum.log, id, fields.get(Media.FILEEXTENSION), stream);
			if(filePath!=null)
				res = true;
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
		return res;
	}


	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	public boolean deleteMedia(String appId, ModelEnum type, String id) {
		String extension = mediaModel.getMediaField(appId, type, id, Media.FILEEXTENSION);
		FileInterface fileModel = getAppFileInterface(appId);
		Boolean res = false;
		try{
			res = fileModel.deleteFile(appId, type, id, extension);
			
		}catch(NoSuchEntityException e){
			Log.error("", this, "deleteFile", "No such element error.", e); 
		}
		res = mediaModel.deleteMedia(appId, type, id);
				
		return res ;
	}
	
	public boolean deleteMediaByResolution(String appId, ModelEnum type, List<String> filesRes) {
		Boolean res = false;
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			res = fileModel.delFilesResolution(appId, type, filesRes);
		}catch(Exception e){
			Log.error("", this, "deleteMediaByResolution", "Delete file with list of images res.", e); 
		}	
		return res ;
	}


	// *** GET LIST *** //

	@Override
	protected List<DBObject> getAllSearchResults(String appId, String userId, String url, Double latitude, Double longitude, Double radius, JSONObject query, String orderType, String orderBy, ModelEnum type, List<String> toShow) throws Exception {
		if(query==null||query.length()==0){
			query = new JSONObject();
			JSONObject jAux= new JSONObject();
			jAux.put("$exists",1);
			query.put(Media.FILENAME, jAux); 
		}
		return docModel.getDocuments(appId, userId, url, latitude, longitude, radius, query, orderType, orderBy,toShow);
	}
	
	
	// *** GET *** //
	public Result getMedia(String appId, ModelEnum type, String id, boolean getMetadata) {
		JSONObject obj = mediaModel.getMedia(appId, type, id, getMetadata);

		Media media = null;
		Metadata metadata = null;
		
		try {
			obj.put(Media._ID, id);
			if (type == ModelEnum.audio) {
				media = new Audio();
				//((Audio)media).setDefaultBitRate(obj.get(Audio.BITRATE));
			} else if (type == ModelEnum.image) {
				media = new Image();
				//((Image)media).setResolution(obj.get(Image.RESOLUTION));
			} else if (type == ModelEnum.storage) {
				media = new Storage();
			} else if (type == ModelEnum.video) {
				media = new Video();
				//((Video)media).setResolution(obj.get(Video.RESOLUTION));
			}
			media.set_id(obj.getString(Media._ID));
			media.setSize(obj.getLong(Media.SIZE));
			media.setDir(obj.getString(Media.PATH));
			media.setFileName(obj.getString(Media.FILENAME));
			media.setFileExtension(obj.getString(Media.FILEEXTENSION));
			if(obj.has(Media.LOCATION))
				media.setLocation(obj.getString(Media.LOCATION));
			if (getMetadata) {
				metadata = Metadata.getMetadata(new JSONObject(obj.getString(ModelAbstract._METADATA)));
			}
		} catch (JSONException e) {
			Log.error("", this, "getMedia", "Error getting Media.", e);
		}

		return new Result(media, metadata);

	}

	
	// *** DOWNLOAD *** //

	public byte[] download(String appId, ModelEnum type, String id,String ext,String quality, String bars) {
		FileInterface fileModel = getAppFileInterface(appId);
		try {
			return fileModel.download(appId, type, id, ext, quality,bars);
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
