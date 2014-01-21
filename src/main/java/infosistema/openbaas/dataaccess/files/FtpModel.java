package infosistema.openbaas.dataaccess.files;

import infosistema.openbaas.data.enums.ModelEnum;

import java.io.IOException;
import java.io.InputStream;

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
	
	@Override
	public boolean createUser(String appId, String userId, String userName) throws Exception {
		//TODO
		return false;
	}

	
	// *** UPLOAD *** //

	@Override
	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream) throws Exception {
		//TODO
		return null;
	}
	
	
	// *** DOWNLOAD *** //
	
	@Override
	public byte[] download(String appId, ModelEnum type, String id, String extension) throws IOException {
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
	public void deleteUser(String appId, String userId) throws Exception {
		//TODO
	}
	
}
