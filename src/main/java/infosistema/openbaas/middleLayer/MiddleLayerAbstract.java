package infosistema.openbaas.middleLayer;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.dataaccess.files.AwsModel;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.files.FileSystemModel;
import infosistema.openbaas.dataaccess.files.FtpModel;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.UserModel;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AppModel appModel;
	protected UserModel userModel;
	protected DocumentModel docModel;

	
	// *** INIT *** //
	
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
		appModel = new AppModel();;
		userModel = new UserModel();
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
		return new Metadata();
	}
	
	public Metadata createMetadata(String key, String userId, String location) {
		return new Metadata();
	}
	
	public Metadata updateMetadata(String key, String userId, String location) {
		return new Metadata();
	}
	
	public Boolean deleteMetadata(String key) {
		return null;
	}

}
