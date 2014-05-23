package infosistema.openbaas.dataaccess.files;

import infosistema.openbaas.data.enums.ModelEnum;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FtpModel implements FileInterface {

	private static FtpModel instance;

	public static FtpModel getInstance() {
		if (instance == null) instance = new FtpModel();
		return instance;
	}

	private FtpModel() {
	}
	
	
	// *** CREATE *** //
	
	@Override
	public boolean createApp(String appId) throws Exception {
		//TODO
		return true;
	}
	
	// *** UPLOAD *** //

	@Override
	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream) throws Exception {
		//TODO
		return null;
	}
	
	
	// *** DOWNLOAD *** //
	
	@Override
	public byte[] download(String appId, ModelEnum type, String id, String extension, String quality, String bars) throws IOException {
		//TODO
		return null;
	}

	
	// *** DETETE *** //
	
	@Override
	public boolean deleteFile(String appId, ModelEnum type, String id, String extension) {
		//TODO
		return false;
	}
	
	
	@Override
	public Boolean delFilesResolution(String appId, ModelEnum type,	List<String> filesRes) {
		// TODO 
		return null;
	}
	
}
