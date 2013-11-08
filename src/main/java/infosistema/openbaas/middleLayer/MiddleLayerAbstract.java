package infosistema.openbaas.middleLayer;

import java.io.File;
import java.io.IOException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;

import infosistema.openbaas.data.ModelEnum;
import infosistema.openbaas.dataaccess.models.AWSModel;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.UserModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AWSModel aws = new AWSModel();

	protected AppModel appModel = new AppModel();;
	protected UserModel userModel = new UserModel();;
	protected DocumentModel docModel;

	// *** INIT *** //
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
	}


	// *** FILESYSTEM *** //

	public byte[] download(String appId, ModelEnum type, String id,String ext) {
		if (Const.AWS.equalsIgnoreCase(Const.getFileSystem()))
			try {
				return this.aws.download(appId, type, id,ext);
			} catch (IOException e) {
				Log.error("", this, "download", "An error ocorred.", e); 
			}
		else{
			Log.error("", this, "download", "FileSystem not yet implemented.");
		}
		return null;
	}

	protected boolean upload(String appId, String filePath, String id, File fileToUpload) {
		if (Const.AWS.equalsIgnoreCase(Const.getFileSystem()))
			try{
				AWSModel aws = new AWSModel();  
				return aws.upload(appId, filePath, id, fileToUpload);
			}catch(AmazonServiceException e){
				Log.error("", this, "upload", "Amazon Service error.", e); 
			}catch(AmazonClientException e){
				Log.error("", this, "upload", "Amazon Client error.", e); 
			}
		else{
			Log.error("", this, "createAppAWS", "FileSystem not yet implemented.");
			return true;
		}
		return false;
	}

	protected boolean deleteFile(String filePath) {
		File f = new File(filePath);
		if(f.exists())
			f.delete();
		if (Const.AWS.equalsIgnoreCase(Const.getFileSystem()))
			try{
				return this.aws.deleteFile(filePath);
			}catch(NoSuchEntityException e){
				Log.error("", this, "deleteFile", "No such element error.", e); 
			}
		else{
			Log.error("", this, "deleteFile", "FileSystem not yet implemented.");
			return true;
		}
		return false;
	}

}
