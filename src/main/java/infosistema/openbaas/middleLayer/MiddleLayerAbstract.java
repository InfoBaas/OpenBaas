package infosistema.openbaas.middleLayer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.dataaccess.files.AwsModel;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.files.FileSystemModel;
import infosistema.openbaas.dataaccess.files.FtpModel;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.MetadataModel;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.dataaccess.models.UserModel;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AppModel appModel;
	protected UserModel userModel;
	protected DocumentModel docModel;
	protected MetadataModel metadataModel;
	protected SessionModel sessionsModel;

	
	// *** INIT *** //
	
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
		appModel = new AppModel();;
		userModel = new UserModel();
		metadataModel = new MetadataModel(); 
		sessionsModel = new SessionModel();
	}

	// *** FILESYSTEM *** //
	
	protected FileInterface getAppFileInterface(String appId) {
		FileMode appFileMode = appModel.getApplicationFileMode(appId);
		if (appFileMode == FileMode.aws) return AwsModel.getInstance();
		else if (appFileMode == FileMode.ftp) return FtpModel.getInstance();
		else return FileSystemModel.getInstance();
	}
	
	// *** METADATA *** //
	
	public Metadata getMetadata(String key) {
		metadataModel = new MetadataModel(); 
		Metadata retObj = new Metadata();
		Map<String, String> fields = metadataModel.getMetadata(key);
		DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy");
		try { 
			retObj.setCreateDate(df.parse(fields.get(Metadata.CREATE_DATE)));
		} catch (Exception e) {}
		retObj.setCreateUser(fields.get(Metadata.CREATE_USER));
		try {
			retObj.setLastUpdateDate(df.parse(fields.get(Metadata.LAST_UPDATE_DATE)));
		} catch (Exception e) {}
		retObj.setLastUpdateUser(fields.get(Metadata.LAST_UPDATE_USER));
		retObj.setLocation(fields.get(Metadata.LOCATION));
		return retObj;
	}
	
	public Metadata createMetadata(String key, String userId, String location) {
		metadataModel = new MetadataModel(); 
		Map<String, String> fields = new HashMap<String, String>();
		fields.put(Metadata.CREATE_DATE, (new Date()).toString());
		fields.put(Metadata.CREATE_USER, userId);
		fields.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		fields.put(Metadata.LAST_UPDATE_USER, userId);
		fields.put(Metadata.LOCATION, location);
		if (metadataModel.createUpdateMetadata(key, fields))
			return getMetadata(key);
		else
			return null;
	}
	
	public Metadata updateMetadata(String key, String userId, String location) {
		metadataModel = new MetadataModel(); 
		Map<String, String> fields = new HashMap<String, String>();
		fields.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		fields.put(Metadata.LAST_UPDATE_USER, userId);
		if (location != null && !"".equals(location))
			fields.put(Metadata.LOCATION, location);
		if (metadataModel.createUpdateMetadata(key, fields))
			return getMetadata(key);
		else
			return null;
	}
	
	public Boolean deleteMetadata(String key) {
		metadataModel = new MetadataModel(); 
		return metadataModel.deleteMetadata(key, true);
	}
	
	

}
