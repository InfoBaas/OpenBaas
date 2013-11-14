package infosistema.openbaas.middleLayer;

import java.io.File;
import java.io.IOException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;

import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.files.AwsModel;
import infosistema.openbaas.data.models.files.FileInterface;
import infosistema.openbaas.data.models.files.FileSystemModel;
import infosistema.openbaas.data.models.files.FtpModel;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.UserModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

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

}
