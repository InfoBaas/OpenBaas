package infosistema.openbaas.middleLayer;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;

import infosistema.openbaas.dataaccess.models.database.CacheInterface;
import infosistema.openbaas.dataaccess.models.database.DatabaseInterface;
import infosistema.openbaas.dataaccess.models.database.RedisDataModel;
import infosistema.openbaas.dataaccess.models.document.DocumentInterface;
import infosistema.openbaas.dataaccess.models.fileSystem.AWSModel;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AWSModel aws;
	protected static String FILESYSTEM = "aws";

	protected static String auxDatabase = "mongodb";
	protected static final String MONGODB = "mongodb";
	protected CacheInterface redisModel = new RedisDataModel();;
	protected DatabaseInterface mongoModel;
	protected DocumentInterface docModel;
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

	// *** FILESYSTEM *** //

	protected byte[] download(String appId, String mediafolder, String requestType,
			String id,String ext) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try {
				return this.aws.download(appId, mediafolder, requestType, id,ext);
			} catch (IOException e) {
				e.printStackTrace();
			}
		else{
			System.out.println("FileSystem not yet implemented.");
		}
		return null;
	}

	protected boolean upload(String appId, String destinationDirectory, String id,
			File fileToUpload) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try{
				return this.aws.upload(appId, destinationDirectory, id, fileToUpload);
			}catch(AmazonServiceException e){
				System.out.println("Amazon Service Exception.");
			}catch(AmazonClientException e){
				System.out.println("Amazon Client Exception.");
			}
		else{
			System.out.println("FileSystem not yet implemented.");
			return true;
		}
		return false;
	}

	protected boolean deleteFile(String fileDirectory) {
		File f = new File(fileDirectory);
		if(f.exists())
			f.delete();
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try{
				return this.aws.deleteFile(fileDirectory);
			}catch(NoSuchEntityException e){
				System.out.println("No such element exception.");
			}
		else{
			System.out.println("FileSystem not yet implemented.");
			return true;
		}
		return false;
	}


	// *** DATA *** //

	protected boolean createUserWithFlag(String appId, String userId, String userName, String socialId, String socialNetwork,
			String email, byte[] salt, byte[] hash, String userFile)
			throws UnsupportedEncodingException {
		boolean operationOk = false;
		boolean cacheOk = false;
		boolean auxOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createUserWithFlag(appId, userId, userName, socialId, socialNetwork, email,
					salt, hash, new Date().toString(), userFile);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createUserWithFlag(appId, userId, userName, socialId, socialNetwork, email,
						salt, hash, new Date().toString(), userFile);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createUserWithFlag(appId, userId, userName, socialId, socialNetwork, email,
						salt, hash, new Date().toString(), userFile);
			if (auxOk)
				operationOk = true;
		}
		return operationOk;
	}
	
	protected boolean createUserWithoutFlag(String appId, String userId, String userName, String socialId, String socialNetwork,
			String email, byte[] salt, byte[] hash)
			throws UnsupportedEncodingException {
		boolean operationOk = false;
		boolean cacheOk = false;
		boolean auxOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createUserWithoutFlag(appId, userId, userName, socialId, socialNetwork,email,
					salt, hash, new Date().toString());
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createUserWithoutFlag(appId, userId, userName, socialId, socialNetwork, email,
						salt, hash, new Date().toString());
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createUserWithoutFlag(appId, userId, userName, socialId, socialNetwork, email,
						salt, hash, new Date().toString());
			if (auxOk)
				operationOk = true;
		}
		return operationOk;
	}

	protected boolean createAudioInApp(String appId, String audioId,
			String directory, String fileExtension, String fileSize,
			String bitRate, String creationDate, String fileName,
			String location) {
		boolean auxOk = false;
		boolean cacheOk = false;
		boolean operationOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createAudioInApp(appId, audioId, directory,
					fileExtension, fileSize, bitRate, creationDate, fileName, location);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createAudioInApp(appId, audioId, directory,
						fileExtension, fileSize, bitRate, creationDate, fileName, location);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			System.out.println("Warning: Cache is full.");
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createAudioInApp(appId, audioId, directory,
						fileExtension, fileSize, bitRate, creationDate,
						fileName, location);
			if (auxOk)
				operationOk = true;
		}
		return operationOk;
	}

	protected boolean createImageInApp(String appId, String id,
			String destinationDirectory, String type, String size,
			String pixelsSize, String creationDate, String fileName,
			String location) {
		boolean auxOk = false;
		boolean cacheOk = false;
		boolean operationOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createImageInApp(appId, id, destinationDirectory, type,
					size, pixelsSize, creationDate,	fileName, location);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createImageInApp(appId, id, destinationDirectory, 
						type, size, pixelsSize,	creationDate, fileName, location);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			System.out.println("Warning: Cache is full.");
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createImageInApp(appId, id, destinationDirectory, 
						type, size, pixelsSize,	creationDate, fileName, location);
		}
		if (auxOk)
			operationOk = true;
		return operationOk;
	}
	
	/**
	 * Creates a video in the Database using the params.
	 * 
	 * @param appId
	 * @param id
	 * @param destinationDirectory
	 * @param type
	 * @param size
	 * @param resolution
	 * @param creationDate
	 * @param fileName
	 * @param location
	 * @return
	 */
	protected boolean createVideoInApp(String appId, String id,
			String destinationDirectory, String type, String size,
			String resolution, String creationDate, String fileName,
			String location) {
		boolean auxOk = false;
		boolean cacheOk = false;
		boolean operationOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createVideoInApp(appId, id,
					destinationDirectory, type, size, resolution, creationDate,
					fileName, location);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createVideoInApp(appId, id,
						destinationDirectory, type, size, resolution,
						creationDate, fileName, location);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			System.out.println("Warning: Cache is full.");
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createVideoInApp(appId, id,
						destinationDirectory, type, size, resolution,
						creationDate, fileName, location);
			if (auxOk)
				operationOk = true;
		}
		return operationOk;

	}

	protected boolean createStorageInApp(String appId, String id,
			String destinationDirectory, String fileExtension, String fileSize,
			String creationDate, String fileName, String location) {
		boolean auxOk = false;
		boolean cacheOk = false;
		boolean operationOk = false;

		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createStorageInApp(appId, id,
					destinationDirectory, fileExtension, fileSize,
					creationDate, fileName, location);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createStorageInApp(appId, id,
						destinationDirectory, fileExtension, fileSize,
						creationDate, fileName, location);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createStorageInApp(appId, id,
						destinationDirectory, fileExtension, fileSize,
						creationDate, fileName, location);
			if (auxOk)
				operationOk = true;
		}
		return operationOk;
	}

	protected String getFileDirectory(String appId, String id, String folderType,
			String requestType) {
		String dir = redisModel.getFileDirectory(appId, id, folderType,
				requestType);
		if (dir == null) {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				dir = mongoModel.getFileDirectory(appId, id, folderType,
						requestType);
			else {
				System.out.println("Database not implemented.");
			}
		}
		return dir;
	}

	protected boolean createUserWithFlagWithEmailConfirmation(String appId,
			String userId, String userName, String socialId, String socialNetwork, String email, byte[] salt,
			byte[] hash, String flag, boolean emailConfirmed) throws UnsupportedEncodingException {
		boolean operationOk = false;
		boolean cacheOk = false;
		boolean auxOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createUserWithFlagWithEmailConfirmation(appId, userId, userName, socialId, socialNetwork, email,
					salt, hash, new Date().toString(), flag, emailConfirmed);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createUserWithFlagWithEmailConfirmation(appId, userId, userName, socialId, socialNetwork, email,
						salt, hash, new Date().toString(), flag, emailConfirmed);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createUserWithFlagWithEmailConfirmation(appId, userId, userName, socialId, socialNetwork, email,
						salt, hash, new Date().toString(), flag, emailConfirmed);
			if (auxOk)
				operationOk = true;
		}
		return operationOk;
	}

	protected boolean createUserWithoutFlagWithEmailConfirmation(String appId,
			String userId, String userName, String socialId, String socialNetwork, String email, byte[] salt,
			byte[] hash, boolean emailConfirmed) throws UnsupportedEncodingException {
		boolean operationOk = false;
		boolean cacheOk = false;
		boolean auxOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createUserWithoutFlagWithEmailConfirmation(appId, userId, userName,socialId, socialNetwork,  email,
					salt, hash, new Date().toString(), emailConfirmed);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createUserWithoutFlagWithEmailConfirmation(appId, userId, userName,socialId, socialNetwork,  email,
						salt, hash, new Date().toString(), emailConfirmed);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createUserWithoutFlag(appId, userId, userName,"NOK", "NOK", email,
						salt, hash, new Date().toString());
			if (auxOk)
				operationOk = true;
		}
		return operationOk;
	}

	public boolean uploadFileToServer(String appId, String id, String folderType, String requestType, String fileDirectory, String location, String fileType, String fileName) {
		boolean upload = false;
		boolean databaseOk = false;
		String fileFormat = "";
		String fileExtension = "";
		String fileSize = "";
		if (folderType.equalsIgnoreCase(MEDIAFOLDER)) {
			if (requestType.equalsIgnoreCase(AUDIO))
				fileFormat = DEFAULTAUDIOFORMAT;
			else if (requestType.equalsIgnoreCase(IMAGES))
				fileFormat = DEFAULTIMAGEFORMAT;
			else if (requestType.equalsIgnoreCase(VIDEO))
				fileFormat = DEFAULTVIDEOFORMAT;
			fileDirectory += "/"+ id+"."+fileType;
			String destinationDirectory = "apps/" + appId + "/" + folderType + "/" + requestType + "/" + id + fileFormat;
			File fileToUpload = new File(fileDirectory);
			upload = upload(appId, destinationDirectory, id, fileToUpload);
			databaseOk = false;
			
			if (upload) {
				fileExtension = FilenameUtils.getExtension(fileDirectory);
				fileName = FilenameUtils.getBaseName(fileDirectory);
				fileSize = "" + fileToUpload.length();
				if (requestType.equalsIgnoreCase("audio"))
					databaseOk = createAudioInApp(appId, id, destinationDirectory, fileExtension, fileSize, MINIMUMBITRATE, new Date().toString(), fileName, location);
				else if (requestType.equalsIgnoreCase("image"))
					databaseOk = createImageInApp(appId, id, destinationDirectory, fileExtension, fileSize, SMALLIMAGE, new Date().toString(), fileName, location);
				else if (requestType.equalsIgnoreCase("video"))
					databaseOk = createVideoInApp(appId, id, destinationDirectory, fileExtension, fileSize, SMALLRESOLUTION, new Date().toString(), fileName, location);
			}
		} else if (folderType.equalsIgnoreCase(STORAGEFOLDER)) {
			fileDirectory += "/"+ id+"."+fileType;
			fileFormat = FilenameUtils.getExtension(fileDirectory);
			String destinationDirectory = "apps/" + appId + "/" + folderType + "/" + id + "." + fileFormat;
			System.out.println(destinationDirectory);
			File fileToUpload = new File(fileDirectory);
			upload = upload(appId, destinationDirectory, id,	fileToUpload);
			if (upload) {
				fileExtension = FilenameUtils.getExtension(fileDirectory);
				fileSize = "" + fileToUpload.length();
				databaseOk = createStorageInApp(appId, id, destinationDirectory, fileExtension, fileSize, new Date().toString(), fileName, location);
			}
		}
		// Finalizing
		if (upload && databaseOk)
			return true;
		return false;
	}

}
