package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.models.database.CacheInterface;
import infosistema.openbaas.dataaccess.models.database.DatabaseInterface;
import infosistema.openbaas.dataaccess.models.database.RedisDataModel;
import infosistema.openbaas.dataaccess.models.document.DocumentInterface;
import infosistema.openbaas.dataaccess.models.fileSystem.AWSModel;
import infosistema.openbaas.model.media.Media;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

import com.sun.jersey.core.header.FormDataContentDisposition;

public class StorageMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	private static final String STORAGEFOLDER = "storage";
	
	// *** INSTANCE *** //

	private static StorageMiddleLayer instance = null;

	protected static StorageMiddleLayer getInstance() {
		if (instance == null) instance = new StorageMiddleLayer();
		return instance;
	}
	
	private StorageMiddleLayer() {
	}

	// *** CREATE *** //
	
	public String createLocalFile(InputStream uploadedInputStream,FormDataContentDisposition fileDetail, String appId, String extension, String dir) {
		String id = Utils.getRandomString(Const.IDLENGTH);
		File dirFolders = new File(dir);
		dirFolders.mkdirs();
		File f = new File(dir + id + "." + extension);
		while (f.exists()) {
			id = Utils.getRandomString(Const.IDLENGTH);
			f = new File(dir + id);
		}
		OutputStream out;
		try {
			out = new FileOutputStream(f);
			IOUtils.copy(uploadedInputStream, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return id;
	}

	public String createLocalFile2(InputStream uploadedInputStream,FormDataContentDisposition fileDetail, String appId, String extension, String dir, String imageId) {
		String id = Utils.getRandomString(Const.IDLENGTH);
		id = imageId;
		File dirFolders = new File(dir);
		dirFolders.mkdirs();
		File f = new File(dir + id + "." + extension);
		while (f.exists()) {
			id = Utils.getRandomString(Const.IDLENGTH);
			id = imageId;
			f = new File(dir + id);
		}
		OutputStream out;
		try {
			out = new FileOutputStream(f);
			IOUtils.copy(uploadedInputStream, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return id;
	}

	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	public void deleteStorageInApp(String appId, String storageId) {
		String fileDirectory = getFileDirectory(appId, storageId, STORAGEFOLDER, null);
		deleteFile(fileDirectory);
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.deleteStorageInApp(appId, storageId);
			if (redisModel.storageExistsInApp(appId, storageId)) {
				redisModel.deleteStorageInApp(appId, storageId);
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	// *** GET LIST *** //

	
	// *** GET *** //
	
	public ArrayList<String> getAllStorageIdsInApp(String appId,Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			return mongoModel.getAllStorageIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public Media getStorageInApp(String appId, String storageId) {
		Map<String, String> storageFields = redisModel.getStorageInApp(appId, storageId);

		if (storageFields == null || storageFields.size() == 0) {
			String dir = null;
			String type = null;
			String size = null;
			String bitRate = null;
			String fileName = null;
			String creationDate = null;
			String location = null;
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				storageFields = mongoModel.getAudioInApp(appId, storageId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : storageFields.entrySet()) {
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
					redisModel.createAudioInApp(appId, storageId, dir, type,
							size, bitRate, creationDate, fileName, location);
				} else {
					System.out.println("Warning: Cache is full.");
				}
			} else {
				System.out.println("Database not implemented.");
			}
		} else {
			storageFields.put("id", storageId);
			storageFields.put("appId", appId);
		}

		Media temp = new Media();
		for (Map.Entry<String, String> entry : storageFields.entrySet()) {
			if (entry.getKey().equalsIgnoreCase("id"))
				temp.setId(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("size"))
				temp.setSize(Long.parseLong(entry.getValue()));
			else if (entry.getKey().equalsIgnoreCase("dir"))
				temp.setDir(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("fileName")) 
				temp.setFileName(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("location"))
				temp.setLocation(entry.getValue());
		}
		return temp;
	}

	// *** UPLOAD *** //

	public String uploadStorageFileToServer(String appId, String fileIdentfier, String fileType, String fileName) {
		if (uploadFileToServer(appId, fileIdentfier, STORAGEFOLDER, null, "apps/"+appId+"/storage", null, fileType, fileName))
			return fileIdentfier;
		else
			return null;
	}

	public String uploadStorageFileToServer(String appId, String fileIdentfier, String fileType, String fileName, String location) {
		if (uploadFileToServer(appId, fileIdentfier, STORAGEFOLDER, null, "apps/"+appId+"/storage", location, fileType, fileName))
			return fileIdentfier;
		else
			return null;
	}


	// *** DOWNLOAD *** //

	public byte[] downloadStorageInApp(String appId, String storageId,String ext) {
		return download(appId, STORAGEFOLDER, null, storageId,ext);
	}

	// *** EXISTS *** //

	public boolean storageExistsInApp(String appId, String storageId) {
		if (redisModel.storageExistsInApp(appId, storageId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.storageExistsInApp(appId, storageId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	
	// *** OTHERS *** //
	
}
