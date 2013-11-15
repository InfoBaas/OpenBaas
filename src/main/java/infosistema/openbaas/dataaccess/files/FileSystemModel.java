package infosistema.openbaas.dataaccess.files;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

public class FileSystemModel implements FileInterface {

	private static final String DIR_PATH_FORMAT = "%sapps/%s/media/%s";
	private static final String FILE_PATH_FORMAT = "%/%s.$s";
	private static FileSystemModel instance;

	public static FileSystemModel getInstance() {
		if (instance == null) instance = new FileSystemModel();
		return instance;
	}

	private FileSystemModel() {
	}

	// *** PRIVATE *** //

	private String getDirPath(String appId, ModelEnum type) {
		return String.format(DIR_PATH_FORMAT, Const.getLocalStoragePath(), appId, type);
	}
	
	private String getFilePath(String dirPath, String id, String extension) {
		return String.format(FILE_PATH_FORMAT, dirPath, id, extension);
	}
	
	// *** CREATE *** //
	
	@Override
	public boolean createApp(String appId) throws Exception {
		return true;
	}
	
	@Override
	public boolean createUser(String appId, String userId, String userName) throws Exception {
		return false;
	}

	
	// *** UPLOAD *** //

	@Override
	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream) throws Exception {
		String dirPath = getDirPath(appId, type);
		File dirFolder = new File(dirPath);
		if (!dirFolder.exists()) dirFolder.mkdirs();
		String filePath = getFilePath(dirPath, id, extension);
		File file = new File(filePath);
		try {
			OutputStream out = new FileOutputStream(file);
			IOUtils.copy(stream, out);
		} catch (FileNotFoundException e) {
			Log.error("", this, "upload", "File not found.", e); 
			return null;
		} catch (IOException e) {
			Log.error("", this, "upload", "An error ocorred.", e); 
			return null;
		}
		return filePath;
	}

	
	// *** DOWNLOAD *** //
	
	@Override
	public byte[] download(String appId, ModelEnum type, String id, String extension) throws IOException {
		String filePath = getFilePath(getDirPath(appId, type), id, extension);
		File file = new File(filePath);
		byte[] byteArray = null;
		try {
			InputStream in = new FileInputStream(file);
			byteArray = IOUtils.toByteArray(in);
		} catch (FileNotFoundException e) {
			Log.error("", this, "download", "File not found.", e); 
			return null;
		} catch (Exception e) {
			Log.error("", this, "download", "An error ocorred.", e); 
			return null;
		}
		return byteArray;
	}

	
	// *** DETETE *** //
	
	@Override
	public boolean deleteFile(String appId, ModelEnum type, String id, String extension) {
		String filePath = getFilePath(getDirPath(appId, type), id, extension);
		try {
			File file = new File(filePath);
			return file.delete();
		} catch (Exception e) {
			Log.error("", this, "download", "An error ocorred.", e); 
		}
		return false;
	}
	
	@Override
	public void deleteUser(String appId, String userId) throws Exception {
	}
	
}
