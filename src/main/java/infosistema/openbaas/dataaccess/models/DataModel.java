package infosistema.openbaas.dataaccess.models;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import com.mongodb.util.JSONSerializers;

import infosistema.openbaas.dataaccess.models.database.CacheInterface;
import infosistema.openbaas.dataaccess.models.database.DatabaseInterface;
import infosistema.openbaas.dataaccess.models.database.MongoDBDataModel;
import infosistema.openbaas.dataaccess.models.database.RedisDataModel;
import infosistema.openbaas.dataaccess.models.document.DocumentInterface;
import infosistema.openbaas.dataaccess.models.document.DocumentModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;


/**
 * This class handles the cache system regarding the Databases, Redis is the
 * Cache as it runs in the RAM. atm MongoDB is the secundary DB, new modules can
 * be added as long as respective managers use the Database Interfase.
 * 
 * @author miguel
 * 
 */
public class DataModel {

	public static String auxDatabase = "mongodb"; // false = cache only // true
													// aux only (testing
													// purposes)
	public static final String MONGODB = "mongodb";
	CacheInterface redisModel;
	DatabaseInterface mongoModel;
	DocumentInterface docModel;
	public static final long MAXCACHESIZE = 10485760; // bytes
	//public static final String SERVER = "localhost";
	//public static final int PORT = 27017;

	public DataModel() {
		redisModel = new RedisDataModel();
		if (auxDatabase.equalsIgnoreCase("mongodb"))
			mongoModel = new MongoDBDataModel(Const.SERVER, Const.MONGO_PORT);
		docModel = new DocumentModel();
	}

	/**
	 * Creates the app in the Database.
	 * 
	 * @param appId
	 * @param creationDate
	 * @param creationDate2
	 * @return
	 */
	public boolean createApp(String appId, String appName, String creationDate, boolean userEmailConfirmation) {
		boolean operationOk = false;
		boolean cacheOk = false;
		boolean auxOk = false;
		if (redisModel.getCacheSize() <= MAXCACHESIZE) {
			cacheOk = redisModel.createApp(appId, appName, creationDate, userEmailConfirmation);
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createApp(appId, appName, creationDate, userEmailConfirmation);
			if (auxOk && cacheOk)
				operationOk = true;
		} else {
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				auxOk = mongoModel.createApp(appId, appName, creationDate, userEmailConfirmation);
			if (auxOk)
				operationOk = true;
		}
		return operationOk;
	}

	/**
	 * Verifies the appId existence. using its appId.
	 */
	public boolean appExists(String appId) {
		if (redisModel.appExists(appId))
			return true;
		else {
			return mongoModel.appExists(appId);
		}
	}

	/**
	 * Removes the application from the database/s.
	 * 
	 * @param appId
	 * @return
	 */
	public boolean deleteApp(String appId) {
		boolean auxOk = false;
		boolean operationOk = false;
		boolean cacheOk = redisModel.deleteApp(appId);
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			auxOk = mongoModel.deleteApp(appId);
		if (cacheOk || auxOk)
			operationOk = true;
		return operationOk;
	}

	/**
	 * Retrieves the appId application fields, these fields are:
	 * "alive","creationDate", "appName".
	 * 
	 * @param appId
	 * @return
	 */
	public Map<String, String> getApplication(String appId) {
		Map<String, String> map = redisModel.getApplication(appId);
		String creationDate = null;
		String appName = null;
		Boolean confirmUsersEmail = false;
		if (map == null || map.size() == 0) {
			map = mongoModel.getApplication(appId);
			if (redisModel.getCacheSize() <= MAXCACHESIZE) {
				for (Entry<String, String> entry : map.entrySet()) {
					if (entry.getKey().equalsIgnoreCase("creationdate"))
						creationDate = entry.getValue();
					else if (entry.getKey().equalsIgnoreCase("appName"))
						appName = entry.getValue();
					else if(entry.getKey().equalsIgnoreCase("confirmUsersEmail"))
						confirmUsersEmail = Boolean.parseBoolean(entry.getValue());
				}
				redisModel.createApp(appId, appName, creationDate, confirmUsersEmail);
			}
		}
		return map;
	}

	public boolean createUserWithFlag(String appId, String userId, String userName, String socialId, String socialNetwork,
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
	public boolean createUserWithoutFlag(String appId, String userId, String userName, String socialId, String socialNetwork,
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
	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.getAllAppIds(pageNumber, pageSize, orderBy, orderType);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public boolean userExistsInApp(String appId, String userId, String email) {
		if (redisModel.userExistsInApp(appId, email))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.userExistsInApp(appId, email);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}
	
	public String socialUserExistsInApp(String appId, String socialId, String socialNetwork) {
		if (redisModel.socialUserExistsInApp(appId, socialId, socialNetwork))
			return redisModel.getUserIdUsingSocialInfo(appId, socialId,socialNetwork);
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.getUserIdUsingSocialInfo(appId, socialId, socialNetwork);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public boolean identifierInUseByUserInApp(String appId, String userId) {
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.identifierInUseByUserInApp(appId, userId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public Map<String, String> getUser(String appId, String userId)
			throws UnsupportedEncodingException {
		Map<String, String> userFields = redisModel.getUser(appId, userId);
		String email = null;
		String creationDate = null;
		String userName = null;
		String socialId = null;
		String socialNetwork = null;
		
		byte[] hash = null;
		byte[] salt = null;
		String flag = null;
		
		if (userFields == null || userFields.size() == 0) {
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				userFields = mongoModel.getUser(appId, userId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : userFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("email"))
							email = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("userName"))
							userName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("hash"))
							hash = JSONSerializers.getStrict()
									.serialize(entry.getValue()).getBytes();
						else if (entry.getKey().equalsIgnoreCase("salt"))
							salt = JSONSerializers.getStrict()
									.serialize(entry.getValue()).getBytes();
						else if(entry.getKey().equalsIgnoreCase("flag"))
							flag = entry.getValue();
						else if(entry.getKey().equalsIgnoreCase("socialId"))
							socialId = entry.getValue();
						else if(entry.getKey().equalsIgnoreCase("socialNetwork"))
							socialNetwork = entry.getValue();
					}
					if(flag != null)
						redisModel.createUserWithFlag(appId, userId,userName, socialId, socialNetwork, email, salt, hash, creationDate, flag);
					else{
						redisModel.createUserWithoutFlag(appId, userId, userName, socialId, socialNetwork, email, salt, hash, creationDate);
					}
				} else {
					System.out.println("Warning: Cache is full.");
				}
			} else {
				System.out.println("Database not implemented.");
			}
		}
		return userFields;
	}

	public ArrayList<String> getAllUserIdsForApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.getAllUserIdsForApp(appId,pageNumber,pageSize,orderBy,orderType);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public boolean updateAllAppFields(String appId, String alive, String newAppName, boolean confirmUsersEmail) {
		boolean sucess = false;
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			sucess = mongoModel.updateAllAppFields(appId, alive, newAppName, confirmUsersEmail);
			if (redisModel.appExists(appId)) {
				redisModel.updateAllAppFields(appId, alive, newAppName, confirmUsersEmail);
			}
		}
		return sucess;
	}

	public void updateUser(String appId, String userId, String email,
			byte[] salt, byte[] hash, String alive) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			try {
				mongoModel.updateUser(appId, userId, email, hash, salt, alive);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if (redisModel.userExistsInApp(appId, email))
				try {
					redisModel.updateUser(appId, userId, email, hash, salt,	alive);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public ArrayList<String> getAllAudioIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.getAllAudioIds(appId,pageNumber,pageSize,orderBy,orderType);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public boolean audioExistsInApp(String appId, String audioId) {
		if (redisModel.audioExistsInApp(appId, audioId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.audioExistsInApp(appId, audioId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public Map<String, String> getAudioInApp(String appId, String audioId) {
		Map<String, String> audioFields = redisModel.getAudioInApp(appId,
				audioId);

		if (audioFields == null || audioFields.size() == 0) {
			String dir = null;
			String type = null;
			String size = null;
			String bitRate = null;
			String fileName = null;
			String creationDate = null;
			String location = null;
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				audioFields = mongoModel.getAudioInApp(appId, audioId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : audioFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("dir"))
							dir = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("type"))
							type = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("size"))
							size = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("bitRate"))
							bitRate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("fileName"))
							fileName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("location"))
							location = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
					}
					redisModel.createAudioInApp(appId, audioId, dir, type,
							size, bitRate, creationDate, fileName, location);
				} else {
					System.out.println("Warning: Cache is full.");
				}
			} else {
				System.out.println("Database not implemented.");
			}
		} else {
			audioFields.put("id", audioId);
			audioFields.put("appId", appId);
		}
		return audioFields;
	}

	public void deleteAudioInApp(String appId, String audioId) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.deleteAudioInApp(appId, audioId);
			if (redisModel.audioExistsInApp(appId, audioId)) {
				redisModel.deleteAudioInApp(appId, audioId);
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public boolean deleteUserInApp(String appId, String userId) {
		boolean auxOk = false;
		boolean cacheOk = false;
		boolean operationOk = false;
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			auxOk = mongoModel.deleteUser(appId, userId);
			String email = redisModel.getEmailUsingUserId(appId, userId);
			if (redisModel.userExistsInApp(appId, email)) {
				cacheOk = redisModel.deleteUser(appId, userId);
				if (auxOk && cacheOk)
					operationOk = true;
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return operationOk;
	}

	public boolean createAudioInApp(String appId, String audioId,
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

	public ArrayList<String> getAllImageIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			return mongoModel.getAllImageIdsInApp(appId,pageNumber, pageSize, orderBy, orderType);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public ArrayList<String> getAllVideoIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			return mongoModel.getAllVideoIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public boolean imageExistsInApp(String appId, String imageId) {
		if (redisModel.imageExistsInApp(appId, imageId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.imageExistsInApp(appId, imageId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public Map<String, String> getImageInApp(String appId, String imageId) {
		Map<String, String> imageFields = redisModel.getImageInApp(appId,
				imageId);

		if (imageFields == null || imageFields.size() == 0) {
			String dir = null;
			String type = null;
			String size = null;
			String pixelsSize = null;
			String creationDate = null;
			String fileName = null;
			String location = null;
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				imageFields = mongoModel.getImageInApp(appId, imageId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : imageFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("dir"))
							dir = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("type"))
							type = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("size"))
							size = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("pixelsSize"))
							pixelsSize = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("fileName"))
							fileName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("location"))
							location = entry.getValue();

					}
					redisModel.createImageInApp(appId, imageId, dir, type,
							size, pixelsSize, creationDate, fileName, location);
				} else {
					System.out.println("Warning: Cache is full.");
				}
			} else {
				System.out.println("Database not implemented.");
			}
		} else {
			imageFields.put("id", imageId);
			imageFields.put("appId", appId);
		}
		return imageFields;
	}

	public boolean createImageInApp(String appId, String id,
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
	
	public Integer countAllImagesInApp(String appId) {
		return mongoModel.countAllImagesInApp(appId);
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
	public boolean createVideoInApp(String appId, String id,
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

	public ArrayList<String> getAllStorageIdsInApp(String appId,Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			return mongoModel.getAllStorageIdsInApp(appId,pageNumber,pageSize,orderBy,orderType);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public boolean createStorageInApp(String appId, String id,
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

	public boolean videoExistsInApp(String appId, String videoId) {
		if (redisModel.videoExistsInApp(appId, videoId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.videoExistsInApp(appId, videoId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public boolean deleteVideoInApp(String appId, String videoId) {
		boolean sucess = false;
		if (redisModel.videoExistsInApp(appId, videoId))
			redisModel.deleteVideoInApp(appId, videoId);
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.deleteVideoInApp(appId, videoId);
			sucess = true;
		} else {
			System.out.println("Database not implemented.");
		}
		return sucess;
	}

	public String getFileDirectory(String appId, String id, String folderType,
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

	public Map<String, String> getVideoInApp(String appId, String videoId) {
		Map<String, String> videoFields = redisModel.getVideoInApp(appId,
				videoId);
		if (videoFields == null || videoFields.size() == 0) {
			String dir = null;
			String type = null;
			String size = null;
			String resolution = null;
			String creationDate = null;
			String fileName = null;
			String location = null;
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				videoFields = mongoModel.getImageInApp(appId, videoId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : videoFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("dir"))
							dir = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("type"))
							type = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("size"))
							size = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("resolution"))
							resolution = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("fileName"))
							fileName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("location"))
							location = entry.getValue();
					}
					redisModel.createVideoInApp(appId, videoId, dir, type,
							size, resolution, creationDate, fileName, location);
				}
			} else {
				System.out.println("Database not implemented.");
			}
		} else {
			videoFields.put("id", videoId);
			videoFields.put("appId", appId);
		}
		return videoFields;
	}

	public String getEmailUsingUserName(String appId, String userName) {
		String email = redisModel.getEmailUsingUserName(appId, userName);
		if (email == null)
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				email = mongoModel.getEmailUsingUserName(appId, userName);
		return email;
	}

	public String getUserIdUsingUserName(String appId, String userName) {
		String userId = redisModel.getUserIdUsingUserName(appId, userName);
		if (userId == null)
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				userId = mongoModel.getUserIdUsingUserName(appId, userName);
		return userId;
	}
	
	public String getUserIdUsingEmail(String appId, String email) {
		String userId = redisModel.getUserIdUsingEmail(appId, email);
		if (userId == null)
			if (auxDatabase.equalsIgnoreCase(MONGODB))
				userId = mongoModel.getUserIdUsingEmail(appId, email);
		return userId;
	}

	public void reviveApp(String appId) {
		if (redisModel.appExists(appId))
			redisModel.reviveApp(appId);
		if (auxDatabase.equalsIgnoreCase(MONGODB))
			mongoModel.reviveApp(appId);

	}

	public void updateUser(String appId, String userId, String email) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.updateUser(appId, userId, email);
			if (redisModel.userExistsInApp(appId, email))
				redisModel.updateUser(appId, userId, email);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public void updateUser(String appId, String userId, String email,
			byte[] hash, byte[] salt) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			try {
				mongoModel.updateUser(appId, userId, email, hash, salt);
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			if (redisModel.userExistsInApp(appId, email))
				try {
					redisModel.updateUser(appId, userId, email, hash, salt);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public boolean createDocumentForApplication(String appId) {
		try {
			docModel.createDocumentForApplication(appId);
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public boolean insertIntoAppDocument(String appId, String url,
			JSONObject data, String location) {
		try {
			docModel.insertIntoDocument(appId, url, data, location);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public String getElementInDocument(String path) {
		try {
			return docModel.getDataInDocument(path);
		} catch (Exception e) {
			return null;
		}
	}

	public boolean dataExistsForElement(String path) {
		try {
			return docModel.dataExistsForElement(path);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean elementExistsInDocument(String url) {
		try {
			return docModel.elementExistsInDocument(url);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean updateDataInDocument(String url, String data) {
		try {
			return docModel.updateDataInDocument(url, data);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean deleteDataInElement(String appId, String url) {
		try {
			return docModel.deleteDataInDocument(url);
		} catch (Exception e) {
			return false;
		}
	}

	public String patchDataInElement(String url, JSONObject inputJson, String appId, String location) {
			try {
				return docModel.patchDataInElement(url, inputJson, appId, location);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		return null;
	}

	public boolean insertDocumentRoot(String appId, JSONObject data, String location) {
		try {
			return docModel.insertDocumentRoot(appId, data, location);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public String getAllDocInApp(String appId) {
		try {
			return docModel.getAllDocInApp(appId);
		} catch (Exception e) {
			return null;
		}
	}

	public ArrayList<String> getAllMediaIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			return mongoModel.getAllMediaIds(appId, pageNumber, pageSize, orderBy, orderType);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	public boolean createNonPublishableDocument(String appId, JSONObject data,
			String url, String location) {
		try {
			return docModel.createNonPublishableDocument(appId, data, url, location);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean insertIntoUserDocument(String appId, String userId,
			String url, JSONObject data, String location) {
		try {
			return docModel.insertIntoUserDocument(appId, userId, data, url, location);
		} catch (Exception e) {
			return false;
		}
	}

	public String getElementInUserDocument(String appId, String userId,
			String url) {
		try {
			return docModel.getElementInUserDocument(appId, userId, url);
		} catch (Exception e) {
			return null;
		}
	}

	public boolean insertUserDocumentRoot(String appId, String userId, JSONObject data, String location) {
		try {
			return docModel.insertUserDocumentRoot(appId, userId, data, location);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	boolean createNonPublishableUserDocument(String appId, String userId,
			JSONObject data, String url, String location) {
		try {
			return docModel.createNonPublishableUserDocument(appId, userId,
					data, url, location);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void updateUserLocationAndDate(String userId, String appId, String sessionToken, String location, String date) {
		mongoModel.updateUserLocationAndDate(userId, appId, sessionToken, location, date);
	}

	public boolean authenticateUser(String appId, String userId,
			String attemptedPassword) throws UnsupportedEncodingException,
			NoSuchAlgorithmException, InvalidKeySpecException {
		Map<String, String> userFields = redisModel.getUser(appId, userId);
		PasswordEncryptionService service = new PasswordEncryptionService();
		byte[] salt = null;
		byte[] hash = null;
		boolean authenticated = false;
		if (userFields == null) {
			userFields = mongoModel.getUser(appId, userId);
			for (Map.Entry<String, String> entry : userFields.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("salt")) {
					salt = entry.getValue().getBytes("ISO-8859-1");
				} else if (entry.getKey().equalsIgnoreCase("hash")) {
					hash = entry.getValue().getBytes("ISO-8859-1");
				}
			}
			authenticated = service.authenticate(attemptedPassword, hash, salt);
		} else {
			for (Map.Entry<String, String> entry : userFields.entrySet()) {
				if (entry.getKey().equalsIgnoreCase("salt")) {
					salt = entry.getValue().getBytes("ISO-8859-1");
				} else if (entry.getKey().equalsIgnoreCase("hash")) {
					hash = entry.getValue().getBytes("ISO-8859-1");
				}
			}
			authenticated = service.authenticate(attemptedPassword, hash, salt);
		}
		return authenticated;
	}

	public boolean storageExistsInApp(String appId, String storageId) {
		if (redisModel.storageExistsInApp(appId, storageId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.storageExistsInApp(appId, storageId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public Map<String, String> getStorageInApp(String appId, String storageId) {
		Map<String, String> storageFields = redisModel.getStorageInApp(appId, storageId);

		if (storageFields == null || storageFields.size() == 0) {
			String dir = null;
			String type = null;
			String size = null;
			String bitRate = null;
			String fileName = null;
			String creationDate = null;
			String location = null;
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				storageFields = mongoModel.getAudioInApp(appId, storageId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : storageFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("dir"))
							dir = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("type"))
							type = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("size"))
							size = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("bitRate"))
							bitRate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("fileName"))
							fileName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("location"))
							location = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
					}
					redisModel.createAudioInApp(appId, storageId, dir, type,
							size, bitRate, creationDate, fileName, location);
				} else {
					System.out.println("Warning: Cache is full.");
				}
			} else {
				System.out.println("Database not implemented.");
			}
		} else {
			storageFields.put("id", storageId);
			storageFields.put("appId", appId);
		}
		return storageFields;
	}

	public ArrayList<String> getAllDocsInRadius(String appId, double latitude,
			double longitude, double radius) {
		try {
			return docModel.getAllDocsInRadius(appId, latitude, longitude, radius);
		} catch (Exception e) {
			return null;
		}
	}

	public ArrayList<String> getElementInDocumentInRadius(String appId, String url,
			double latitude, double longitude, double radius) {
		try {
			return docModel.getDataInDocumentInRadius(appId, url, latitude, longitude,radius);
		} catch (Exception e) {
			return null;
		}
	}

	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId, double latitude,
			double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		try {
			return docModel.getAllUserDocsInRadius(appId, userId, latitude, longitude, radius, pageNumber,pageSize,orderBy,orderType);
		} catch (Exception e) {
			return null;
		}
	}

	public String getAllUserDocs(String appId, String userId) {
		try {
			return docModel.getAllUserDocs(appId, userId);
		} catch (Exception e) {
			return null;
		}
	}

	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude,double longitude, double radius) {
		try {
			return docModel.getAllAudioIdsInRadius(appId, latitude, longitude, radius);
		} catch (Exception e) {
			return null;
		}
	}
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,double longitude, double radius, 
			Integer pageNumber, Integer pageSize, String orderBy, String orderType){
		try{
			return docModel.getAllImagesIdsInRadius(appId, latitude, longitude, radius,pageNumber,pageSize,orderBy,orderType);
		} catch (Exception e) {
			return null;
		}
	}

	public void deleteStorageInApp(String appId, String storageId) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.deleteStorageInApp(appId, storageId);
			if (redisModel.storageExistsInApp(appId, storageId)) {
				redisModel.deleteStorageInApp(appId, storageId);
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public boolean confirmUsersEmail(String appId) {
		if(redisModel.appExists(appId))
			return redisModel.confirmUsersEmail(appId);
		else if(auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.confirmUsersEmail(appId);
		else{
			return false;
		}
	}

	public void deleteImageInApp(String appId, String imageId) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.deleteImageInApp(appId, imageId);
			if (redisModel.imageExistsInApp(appId, imageId)) {
				redisModel.deleteImageInApp(appId, imageId);
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public boolean createUserWithFlagWithEmailConfirmation(String appId,
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

	public boolean createUserWithoutFlagWithEmailConfirmation(String appId,
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

	public void confirmUserEmail(String appId, String userId) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.confirmUserEmail(appId, userId);
			String email = redisModel.getEmailUsingUserId(appId, userId);
			if (redisModel.userExistsInApp(appId, email)) {
				redisModel.confirmUserEmail(appId, userId);
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	public boolean userEmailIsConfirmed(String appId, String userId) {
		if (redisModel.userEmailIsConfirmed(appId, userId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.userEmailIsConfirmed(appId, userId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	public boolean updateConfirmUsersEmailOption(String appId,
			Boolean confirmUsersEmail) {
		boolean sucess = false;
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			sucess = mongoModel.updateConfirmUsersEmailOption(appId, confirmUsersEmail);
			if (redisModel.appExists(appId)) {
				redisModel.updateConfirmUsersEmailOption(appId, confirmUsersEmail);
			}
		}
		return sucess;
	}

	public boolean updateAppName(String appId, String newAppName) {
		boolean sucess = false;
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			sucess = mongoModel.updateAppName(appId, newAppName);
			if (redisModel.appExists(appId)) {
				redisModel.updateAppName(appId, newAppName);
			}
		}
		return sucess;
	}

	public boolean updateUserPassword(String appId, String userId, byte[] hash,
			byte[] salt) {
		boolean sucess = false;
		String email = mongoModel.getEmailUsingUserId(appId, userId);
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			try {
				sucess = mongoModel.updateUserPassword(appId, userId, hash, salt);
				if (redisModel.appExists(appId) && redisModel.userExistsInApp(appId, email)) {
					redisModel.updateUserPassword(appId, userId, hash, salt);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
		}
		return sucess;
	}

	public boolean userExistsInApp(String appId, String userId) {
		if (redisModel.userExistsInApp(appId, redisModel.getEmailUsingUserId(appId, userId)))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.userExistsInApp(appId, mongoModel.getEmailUsingUserId(appId, userId));
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	

	

	
}
