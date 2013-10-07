package infosistema.openbaas.dataModels;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

public interface DatabaseInterface {

	// App operations
	/**
	 * Creates an Application.
	 * 
	 * @param appId
	 * @param appName
	 * @param creationDate
	 * @return
	 */
	public Boolean createApp(String appId, String appName, String creationDate,
			Boolean userEmailConfirmation);

	/**
	 * Deletes the application with appId.
	 * 
	 * @param appId
	 * @return
	 */
	public Boolean deleteApp(String appId);

	/**
	 * Updates the currentId application, changing its appName and alive fields.
	 * 
	 * @param currentId
	 * @param alive
	 * @param newAppName
	 * @return
	 */
	public Boolean updateAllAppFields(String currentId, String alive,
			String newAppName, Boolean confirmUsersEmail);

	/**
	 * Retrieves the application Fields.
	 * 
	 * @param appId
	 * @return
	 */
	public Map<String, String> getApplication(String appId);

	/**
	 * Checks if the appId application exists.
	 * 
	 * @param appId
	 * @return
	 */
	public Boolean appExists(String appId);

	/**
	 * Returns all the application Identifiers.
	 * @param orderType 
	 * @param orderBy 
	 * @param pageSize 
	 * @param pageNumber 
	 * 
	 * @return
	 */
	public ArrayList<String> getAllAppIds(Integer pageNumber, Integer pageSize, String orderBy, String orderType);

	/**
	 * Converts an application identifier to its rightful appName.
	 * 
	 * @param appId
	 * @return
	 */
	public String convertAppIdToAppName(String appId);

	/**
	 * Changes an application alive field to true.
	 * 
	 * @param appId
	 */
	public void reviveApp(String appId);

	/**
	 * Updates the application name.
	 * 
	 * @param appId
	 * @param newAppName
	 * @return 
	 */
	public Boolean updateAppName(String appId, String newAppName);

	// User operations
	/**
	 * Retrieves all the User Identifiers for the application.
	 * 
	 * @param appId
	 * @param pageSize 
	 * @param pageNumber 
	 * @param orderType 
	 * @param orderBy 
	 * @return
	 */
	public ArrayList<String> getAllUserIdsForApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType);

	/**
	 * Verifies if an userId is currently in use for an application.
	 * 
	 * @param appId
	 * @param userId
	 * @return
	 */
	public Boolean identifierInUseByUserInApp(String appId, String userId);

	/**
	 * Verifies if a user existings in the application using its email.
	 * 
	 * @param appId
	 * @param email
	 * @return
	 */
	public Boolean userExistsInApp(String appId, String email);

	/**
	 * Creates a user.
	 * 
	 * @param appId
	 * @param userId
	 * @param userName
	 * @param email
	 * @param salt
	 * @param hash
	 * @param creationDate
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public Boolean createUserWithFlag(String appId, String userId,
			String userName, String email, byte[] salt, byte[] hash,
			String creationDate, String flag)
			throws UnsupportedEncodingException;

	public Boolean createUserWithoutFlag(String appId, String userId,
			String userName, String email, byte[] salt, byte[] hash,
			String creationDate) throws UnsupportedEncodingException;

	/**
	 * Retrieves the user fields.
	 * 
	 * @param appId
	 * @param userId
	 * @return
	 */
	public Map<String, String> getUser(String appId, String userId);

	/**
	 * Removes the user from the Database/s (sets it as inactive).
	 * 
	 * @param appId
	 * @param userId
	 * @return
	 */
	public Boolean deleteUser(String appId, String userId);

	/**
	 * Updates the user with the param fields.
	 * 
	 * @param appId
	 * @param userId
	 * @param email
	 * @param hash
	 * @param salt
	 * @param alive
	 * @throws UnsupportedEncodingException
	 */
	public void updateUser(String appId, String userId, String email,
			byte[] hash, byte[] salt, String alive)
			throws UnsupportedEncodingException;

	/**
	 * Retrieves the userName of the user using its userId as reference.
	 * 
	 * @param userId
	 * @return
	 */
	public String getUserNameUsingUserId(String appId, String userId);

	/**
	 * Retrieves the userId of the user using its userName as reference.
	 * 
	 * @param appId
	 * @param userName
	 * @return
	 */
	public String getUserIdUsingUserName(String appId, String userName);

	/**
	 * Retrieves the email of the specified user using the userId as reference.
	 * 
	 * @param userId
	 * @return
	 */
	public String getEmailUsingUserId(String appId, String userId);

	/**
	 * Retrieves the email of the specified user using its userName as
	 * reference.
	 * 
	 * @param appId
	 * @param userName
	 * @return
	 */
	public String getEmailUsingUserName(String appId, String userName);

	/**
	 * Updates the user email.
	 * 
	 * @param appId
	 * @param userId
	 * @param email
	 */
	public void updateUser(String appId, String userId, String email);

	/**
	 * Updates the user email and password.
	 * 
	 * @param appId
	 * @param userId
	 * @param email
	 * @param hash
	 * @param salt
	 * @throws UnsupportedEncodingException
	 */
	public void updateUser(String appId, String userId, String email,
			byte[] hash, byte[] salt) throws UnsupportedEncodingException;

	// Audio operations
	/**
	 * Retrieves all the audio Identifiers contained in the application.
	 * 
	 * @param appId
	 * @param orderType 
	 * @param orderBy 
	 * @param pageSize 
	 * @param pageNumber 
	 * @return
	 */
	public ArrayList<String> getAllAudioIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType);

	/**
	 * Verifies if the audio file with audioId exists in the application.
	 * 
	 * @param appId
	 * @param audioId
	 * @return
	 */
	public Boolean audioExistsInApp(String appId, String audioId);

	/**
	 * Retrieves the audio Fields (metadata).
	 * 
	 * @param appId
	 * @param audioId
	 * @return
	 */
	public Map<String, String> getAudioInApp(String appId, String audioId);

	/**
	 * Deletes the audio File from the Database/s.
	 * 
	 * @param appId
	 * @param audioId
	 */
	public void deleteAudioInApp(String appId, String audioId);

	/**
	 * Creates an audio entry in the DataModel with the param fields.
	 * 
	 * @param appId
	 * @param audioId
	 * @param directory
	 * @param fileExtension
	 * @param size
	 * @param bitRate
	 * @param creationDate
	 * @param fileName
	 * @param location
	 * @return
	 */
	public Boolean createAudioInApp(String appId, String audioId,
			String directory, String fileExtension, String size,
			String bitRate, String creationDate, String fileName,
			String location);

	// Image operations
	/**
	 * Retrieves all the image Identifiers of images contained in the
	 * application.
	 * 
	 * @param appId
	 * @param orderType 
	 * @param orderBy 
	 * @param pageSize 
	 * @param pageNumber 
	 * @return
	 */
	public ArrayList<String> getAllImageIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType);

	/**
	 * Verifies if an image with imageId exists in the application.
	 * 
	 * @param appId
	 * @param imageId
	 * @return
	 */
	public Boolean imageExistsInApp(String appId, String imageId);

	/**
	 * Retrieves the image fields of the specified image.
	 * 
	 * @param appId
	 * @param imageId
	 * @return
	 */
	public Map<String, String> getImageInApp(String appId, String imageId);

	/**
	 * Creates an image entry in the Database/s with the specified params.
	 * 
	 * @param appId
	 * @param imageId
	 * @param directory
	 * @param type
	 * @param size
	 * @param pixelsSize
	 * @param creationDate
	 * @param fileName
	 * @param location
	 * @return
	 */
	public Boolean createImageInApp(String appId, String imageId,
			String directory, String type, String size, String pixelsSize,
			String creationDate, String fileName, String location);

	// Video operations
	/**
	 * Retrieves all the ids of Video files contained in the application.
	 * 
	 * @param appId
	 * @param orderType 
	 * @param orderBy 
	 * @param pageSize 
	 * @param pageNumber 
	 * @return
	 */
	public ArrayList<String> getAllVideoIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType);

	/**
	 * Creates a video entry with the specified params.
	 * 
	 * @param appId
	 * @param videoId
	 * @param directory
	 * @param type
	 * @param size
	 * @param resolution
	 * @param creationDate
	 * @param fileName
	 * @param location
	 * @return
	 */
	public Boolean createVideoInApp(String appId, String videoId,
			String directory, String type, String size, String resolution,
			String creationDate, String fileName, String location);

	/**
	 * Verifies if a Video with videoId exists in the application.
	 * 
	 * @param appId
	 * @return
	 */

	public Boolean videoExistsInApp(String appId, String videoId);

	/**
	 * Removes the Video with videoId from the Database/s.
	 * 
	 * @param appId
	 * @param videoId
	 * @return
	 */
	public Boolean deleteVideoInApp(String appId, String videoId);

	/**
	 * Retrieves the Video Fields of the video with videoId.
	 * 
	 * @param appId
	 * @param videoId
	 * @return
	 */
	public Map<String, String> getVideoInApp(String appId, String videoId);

	// Storage operations
	/**
	 * Creates a storage entry in the Database/s with the specified params.
	 * 
	 * @param appId
	 * @param storageId
	 * @param directory
	 * @param fileExtension
	 * @param fileSize
	 * @param creationDate
	 * @param fileName
	 * @param location
	 * @return
	 */
	public Boolean createStorageInApp(String appId, String storageId,
			String directory, String fileExtension, String fileSize,
			String creationDate, String fileName, String location);

	/**
	 * Retrieves all the ids of the storage files contained in the application.
	 * 
	 * @param appId
	 * @param orderType 
	 * @param orderBy 
	 * @param pageSize 
	 * @param pageNumber 
	 * @return
	 */
	public ArrayList<String> getAllStorageIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType);

	// public Set<String> getAllStorageIds(String appId);

	/**
	 * Retrieves the file directory of the indicated folder. Folder type refers
	 * to MEDIA or Storage Request Type refers to audio/image/video
	 * 
	 * @param appId
	 * @param id
	 * @param folderType
	 * @param requestType
	 * @return
	 */
	public String getFileDirectory(String appId, String id, String folderType,
			String requestType);

	/**
	 * Retrieves all the ids of media files (images, video, audio).
	 * 
	 * @param appId
	 * @param orderType 
	 * @param orderBy 
	 * @param pageSize 
	 * @param pageNumber 
	 * @return
	 */
	public ArrayList<String> getAllMediaIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType);

	public void updateUserLocationAndDate(String userId, String appId,
			String sessionToken, String location, String date);

	public Boolean storageExistsInApp(String appId, String storageId);

	public Map<String, String> getStorageInApp(String appId, String storageId);

	public Boolean deleteStorageInApp(String appId, String storageId);

	public Boolean confirmUsersEmail(String appId);

	public Boolean deleteImageInApp(String appId, String imageId);

	public Boolean createUserWithFlagWithEmailConfirmation(String appId,
			String userId, String userName, String email, byte[] salt,
			byte[] hash, String creationDate, String flag,
			Boolean emailConfirmed) throws UnsupportedEncodingException;

	public Boolean createUserWithoutFlagWithEmailConfirmation(String appId,
			String userId, String userName, String email, byte[] salt,
			byte[] hash, String creationDate, Boolean emailConfirmed)
			throws UnsupportedEncodingException;

	public Boolean confirmUserEmail(String appId, String userId);

	public Boolean userEmailIsConfirmed(String appId, String userId);

	public Boolean updateConfirmUsersEmailOption(String appId,
			Boolean confirmUsersEmail);

	public Boolean updateUserPassword(String appId, String userId, byte[] hash,
			byte[] salt) throws UnsupportedEncodingException;

	public String getUserIdUsingEmail(String appId, String email);

	public Integer countAllImagesInApp(String appId);


}