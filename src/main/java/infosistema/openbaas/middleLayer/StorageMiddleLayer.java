package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.models.Model;
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
import org.apache.commons.io.IOUtils;

import com.sun.jersey.core.header.FormDataContentDisposition;

public class StorageMiddleLayer {

	// *** MEMBERS *** ///

	private Model model;
	private static final String STORAGEFOLDER = "storage";
	
	// *** INSTANCE *** ///

	private static StorageMiddleLayer instance = null;

	protected static StorageMiddleLayer getInstance() {
		if (instance == null) instance = new StorageMiddleLayer();
		return instance;
	}
	
	private StorageMiddleLayer() {
		model = Model.getModel(); // SINGLETON
	}

	// *** CREATE *** ///
	
	public String uploadStorageFileToServer(String appId, String fileIdentfier, String fileType, String fileName) {
		if (this.model.uploadFileToServer(appId, fileIdentfier, STORAGEFOLDER, null, "apps/"+appId+"/storage", null, fileType, fileName))
			return fileIdentfier;
		else
			return null;
	}

	public String uploadStorageFileToServer(String appId, String fileIdentfier, String fileType, String fileName, String location) {
		if (this.model.uploadFileToServer(appId, fileIdentfier, STORAGEFOLDER, null, "apps/"+appId+"/storage", location, fileType, fileName))
			return fileIdentfier;
		else
			return null;
	}

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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return id;
	}

	// *** UPDATE *** ///
	
	// *** DELETE *** ///
	
	public void deleteStorageInApp(String appId, String storageId) {
		this.model.deleteStorageFile(appId, storageId);
	}

	// *** GET *** ///
	
	public ArrayList<String> getAllStorageIdsInApp(String appId,Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllStorageIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
	}

	public Media getStorageInApp(String appId, String storageId) {
		Map<String, String> storageFields = this.model.getStorageInApp(appId, storageId);
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

	// *** OTHERS *** ///
	
	public byte[] downloadStorageInApp(String appId, String storageId,String ext) {
		return this.model.downloadStorageInApp(appId, storageId,ext);
	}

	public boolean storageExistsInApp(String appId, String storageId) {
		return this.model.storageExistsInApp(appId, storageId);
	}

}
