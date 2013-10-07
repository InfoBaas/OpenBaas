package infosistema.openbaas.fileSystemModels;

import java.io.File;
import java.io.IOException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;

public interface FileSystemInterface {
	/**
	 * Downloads the file with the id contained in the app.
	 * @param appId
	 * @param folder (can be media/storage)
	 * @param requestType (video/audio/image)
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public byte[] download(String appId, String folder, String requestType,
			String id, String ext) throws IOException;
	/**
	 * Uploads the file to the given directory.
	 * @param appId
	 * @param destinationDirectory
	 * @param id
	 * @param file
	 * @return
	 * @throws AmazonServiceException
	 * @throws AmazonClientException
	 */
	boolean upload(String appId, String destinationDirectory, String id,
			File file) throws AmazonServiceException,
			AmazonClientException;
	/**
	 * Creates the application folders and sets the filesystem to be ready to receive files.
	 * @param appId
	 * @return
	 * @throws EntityAlreadyExistsException
	 * @throws AmazonServiceException
	 */
	public boolean createApp(String appId)
			throws EntityAlreadyExistsException, AmazonServiceException;
	/**
	 * Creates a user in the filesystem, useful for services like aws that can have users.
	 * @param appId
	 * @param userId
	 * @param userName
	 * @return
	 * @throws EntityAlreadyExistsException
	 */
	public boolean createUser(String appId, String userId, String userName)
			throws EntityAlreadyExistsException;
	/**
	 * Deletes the file in the given directory.
	 * @param fileDirectory
	 * @return
	 */
	public boolean deleteFile(String fileDirectory) ;
	/**
	 * Deletes the given user in the application.
	 * @param appId
	 * @param userId
	 * @throws NoSuchEntityException
	 */
	public void deleteUser(String appId, String userId)
			throws NoSuchEntityException;
	
}
