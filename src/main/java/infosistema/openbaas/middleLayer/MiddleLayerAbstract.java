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

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AWSModel aws = new AWSModel();
	protected static String FILESYSTEM = "aws";

	protected static String auxDatabase = "mongodb";
	protected static final String MONGODB = "mongodb";
	protected AppModel appModel = new AppModel();;
	protected UserModel userModel = new UserModel();;
	protected DocumentModel docModel;
	protected static final long MAXCACHESIZE = 10485760; // bytes

	// request types
	protected static final String AUDIO = "audio";
	protected static final String IMAGES = "image";
	protected static final String VIDEO = "video";

	// Request folders
	protected static final String MEDIAFOLDER = "media";
	protected static final String STORAGEFOLDER = "storage";

	// File stuff
	protected static final String DEFAULTIMAGEFORMAT = ".jpg";
	protected static final String DEFAULTVIDEOFORMAT = ".mpg";
	protected static final String DEFAULTAUDIOFORMAT = ".mp3";

	// VIDEO RESOLUTIONS
	protected static final String SMALLRESOLUTION = "360p";

	// Image Sizes
	protected static final String SMALLIMAGE = "300x300";

	// AUDIO BITRATES
	protected static final String MINIMUMBITRATE = "32";
	
	// *** INIT *** //
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
	}


	// *** FILESYSTEM *** //

	public byte[] download(String appId, ModelEnum type, String id,String ext) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try {
				return this.aws.download(appId, type, id,ext);
			} catch (IOException e) {
				e.printStackTrace();
			}
		else{
			System.out.println("FileSystem not yet implemented.");
		}
		return null;
	}

	protected boolean upload(String appId, String filePath, String id, File fileToUpload) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try{
				AWSModel aws = new AWSModel();  
				return aws.upload(appId, filePath, id, fileToUpload);
			}catch(AmazonServiceException e){
				System.out.println("Amazon Service Exception."+ e.toString());
			}catch(AmazonClientException e){
				System.out.println("Amazon Client Exception." + e.toString());
			}
		else{
			System.out.println("FileSystem not yet implemented.");
			return true;
		}
		return false;
	}

	protected boolean deleteFile(String filePath) {
		File f = new File(filePath);
		if(f.exists())
			f.delete();
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try{
				return this.aws.deleteFile(filePath);
			}catch(NoSuchEntityException e){
				System.out.println("No such element exception.");
			}
		else{
			System.out.println("FileSystem not yet implemented.");
			return true;
		}
		return false;
	}

}
