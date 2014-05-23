package infosistema.openbaas.dataaccess.files;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.utils.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class FileSystemModel implements FileInterface {

	private static FileSystemModel instance;
	private static AppModel appModel =null;

	public static FileSystemModel getInstance() {
		if (instance == null) instance = new FileSystemModel();
		appModel = new AppModel();
		return instance;
	}

	private FileSystemModel() {
	}

	// *** CREATE *** //
	
	@Override
	public boolean createApp(String appId) throws Exception {
		return true;
	}

	
	// *** UPLOAD *** //

	@Override
	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream) throws Exception {
		String dirPath = FilesUtils.getDirPath(appId, type);
		File dirFolder = new File(dirPath);
		if (!dirFolder.exists()) dirFolder.mkdirs();
		String filePath = FilesUtils.getFilePath(dirPath, id, extension);
		File file = new File(filePath);
		try {
			OutputStream out = new FileOutputStream(file);
			IOUtils.copy(stream, out);
			out.flush();
			out.close();
			stream.close();
		} catch (FileNotFoundException e) {
			Log.error("", this, "upload", "File not found.", e); 
			return null;
		} catch (Exception e) {
			Log.error("", this, "upload", "An error ocorred.", e); 
			return null;
		}
		return filePath;
	}

	
	// *** DOWNLOAD *** //
	
	@Override
	public byte[] download(String appId, ModelEnum type, String id, String extension, String quality, String bars) throws IOException {
		byte[] byteArrayRes = null;
		String filePath = null;
		if(quality.equals("") || quality==null) quality=FilesUtils.ORIGINAL;
		String filePathOriginal = FilesUtils.getFilePath(FilesUtils.getDirPath(appId, type), id, extension);
		
		if(quality.equals(FilesUtils.ORIGINAL)){
			filePath = FilesUtils.getFilePath(FilesUtils.getDirPath(appId, type), id, extension);
		}else{
			if(type.equals(ModelEnum.image)) extension = Image.EXTENSION;
			filePath = FilesUtils.getFilePathWithQuality(FilesUtils.getDirPath(appId, type), id, quality, extension,bars);
		}
		File file = new File(filePath);
		try {
			if(file.exists()){
				InputStream in = new FileInputStream(file);
				byteArrayRes = IOUtils.toByteArray(in);
				in.close();
			}else{
				String qualityRes = appModel.getFileQuality(appId, type, quality);
				File fileAux = new File(filePathOriginal);
				byte[] byteArray = null;
				InputStream in = new FileInputStream(filePathOriginal);
				FileOutputStream fos = new FileOutputStream(filePath);
				byteArray = IOUtils.toByteArray(in);
				if(qualityRes!=null) qualityRes.toUpperCase();
				byteArrayRes= FilesUtils.getInstance().resizeFile(appId,byteArray, qualityRes, type, fileAux, extension,filePath,bars);
				fos.write(byteArrayRes);
				fos.close();
				in.close();
			}
		} catch (FileNotFoundException e) {
			Log.error("", this, "download", "File not found.", e); 
			return null;
		} catch (Exception e) {
			Log.error("", this, "download", "An error ocorred.", e);
			//file.delete();
			return null;
		}
		return byteArrayRes;
	}

	
	
	
	// *** DELETE *** //
	@Override
	public Boolean delFilesResolution(String appId, ModelEnum type, List<String> filesRes) {
		Boolean res = false;
		File folder = new File(FilesUtils.getDirPath(appId, type));
		File[] listOfFiles = folder.listFiles();
		for(int i = 0; i<listOfFiles.length; i++){
			File curr = listOfFiles[i];
			if(curr.isFile()){
				String extension = FilenameUtils.getExtension(curr.getAbsolutePath());
				Iterator<String> it = filesRes.iterator();
				while(it.hasNext()){
					String fileRes = it.next();
					if(curr.getName().endsWith(fileRes+"."+extension)){
						try {
							curr.delete();
							res = true;
						} catch (Exception e) {
							Log.error("", this, "delete", "An error ocorred.", e); 
							res = false;
						}
					}
				}
			}
		}
		return res;
	}
	
	@Override
	public boolean deleteFile(String appId, ModelEnum type, String id, String extension) {
		String filePath = FilesUtils.getFilePath(FilesUtils.getDirPath(appId, type), id, extension);
		try {
			File file = new File(filePath);
			return file.delete();
		} catch (Exception e) {
			Log.error("", this, "delete", "An error ocorred.", e); 
		}
		return false;
	}
	
		
}
