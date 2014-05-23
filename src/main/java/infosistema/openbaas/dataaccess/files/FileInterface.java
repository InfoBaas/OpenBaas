package infosistema.openbaas.dataaccess.files;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import infosistema.openbaas.data.enums.ModelEnum;

public interface FileInterface {

	// *** CREATE *** //
	
	public boolean createApp(String appId) throws Exception;
	
	// *** UPLOAD *** //

	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream) throws Exception;
	
	
	// *** DOWNLOAD *** //
	
	public byte[] download(String appId, ModelEnum type, String id, String extension, String quality, String bars) throws IOException;

	
	// *** DETETE *** //
	
	public boolean deleteFile(String appId, ModelEnum type, String id, String extension);
	
	public Boolean delFilesResolution(String appId, ModelEnum type, List<String> filesRes);
	
}
