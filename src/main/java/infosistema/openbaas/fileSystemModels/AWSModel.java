package infosistema.openbaas.fileSystemModels;

import java.io.ByteArrayInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class AWSModel implements FileSystemInterface{
	private static AWSModel ref;
	private static final String OPENBAASBUCKET = "openbaas";
	//private static final String DEFAULTIMAGEFORMAT = ".jpg";
	//private static final String DEFAULTVIDEOFORMAT = ".mpg";
	//private static final String DEFAULTAUDIOFORMAT = ".mp3";

	private static final String APPMASTERSGROUP = "ApplicationMasters";
	private AmazonS3 s3;
	private AmazonIdentityManagementClient client;

	public static AWSModel getAWSModel() {
		if (ref == null) {
			ref = new AWSModel();
			ref.startAWS();
			ref.startIAM();
		}
		return ref;
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * Starts the S3 Amazon connection.
	 */
	public void startAWS() {
		s3 = new AmazonS3Client(
				new ClasspathPropertiesFileCredentialsProvider());
		Region euWest = Region.getRegion(Regions.EU_WEST_1);
		s3.setRegion(euWest);
	}

	/**
	 * Starts the Amazon Identity Management Client
	 */
	public void startIAM() {
		client = new AmazonIdentityManagementClient(
				new ClasspathPropertiesFileCredentialsProvider());
	}
	
	public void listBuckets() {
		System.out.println("Listing buckets");
		for (Bucket bucket : s3.listBuckets()) {
			System.out.println(" - " + bucket.getName());
		}
	}
	@Override
	/**
	 * Allowed types images, audio, video
	 * 
	 * Allowed structures, media, storage
	 * 
	 * @param appId
	 * @param requestType
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public byte[] download(String appId, String folder, String requestType, String id, String ext) throws IOException {
		OutputStream soutputStream=null;
		byte[] byteArray = null;
		try{
			this.startAWS();
	//		if(folder.equalsIgnoreCase(MEDIAFOLDER)){
	//			if (requestType == "audio")
	//				fileFormat = DEFAULTAUDIOFORMAT;
	//			else if (requestType == "images")
	//				fileFormat = DEFAULTIMAGEFORMAT;
	//			else if (requestType == "video")
	//				fileFormat = DEFAULTVIDEOFORMAT;
	//		}
	//		else if(folder.equalsIgnoreCase(STORAGEFOLDER)){
	//			
	//		}
			
			//directory= "apps/296/media/images/3d2.jpg"
			StringBuffer directory = new StringBuffer("apps/" + appId + "/" + folder + "/" + requestType + "/"+id);
			if(ext!=null)
				directory.append("."+ext);
			System.out.println(directory);
			S3Object object = s3.getObject(new GetObjectRequest(OPENBAASBUCKET,directory.toString()));
			S3ObjectInputStream s3ObjInputStream = object.getObjectContent();
			byteArray = IOUtils.toByteArray(s3ObjInputStream);
			//String dir = "apps/" + appId + "/" + folder + "/" + requestType	+ "/"+object.getKey(); //id?
			System.out.println("Downloading to: " + directory.toString());
			soutputStream = new FileOutputStream(new File(directory.toString()));
			int read = 0;
			byte[] bytes = new byte[1024];
			S3Object object2 = s3.getObject(new GetObjectRequest(OPENBAASBUCKET,directory.toString()));
			
			while ((read = object2.getObjectContent().read(bytes)) != -1) {
				soutputStream.write(bytes, 0, read);
			}	 
		}catch(Exception e){
			System.err.println(e.toString());
		}finally{
			soutputStream.close();
		}
		return byteArray;
	}
	@Override
	public boolean upload(String appId, String destinationDirectory, String id,
			File file) throws AmazonServiceException, AmazonClientException {
		this.startAWS();
		s3.putObject(new PutObjectRequest(OPENBAASBUCKET, destinationDirectory,
				file));

		return true;
	}
	@Override
	/**
	 * Other possibility is to deploy individual buckets per app, not viable
	 * because you can't create buckets that already exists in the AWS. Try to
	 * create a twitter bucket and you get an error, this would severelly limit
	 * our application system, we do not want to be name limited by amazon.
	 * 
	 * It is harder to handle user permissions this way but it is possible to
	 * restrict users to their app folders.
	 * 
	 * @param appId
	 * @return
	 */
	public boolean createApp(String appId)
			throws EntityAlreadyExistsException, AmazonServiceException {
		this.startAWS();
		this.startIAM();
		boolean sucess = false;
		// ------------Creating the AppMaster User + Adding it to the AppMasters
		// group (limited permissions)
		CreateUserRequest user = new CreateUserRequest(appId);
		CreateAccessKeyRequest key = new CreateAccessKeyRequest();
		key.withUserName(user.getUserName());
		user.setRequestCredentials(key.getRequestCredentials());
		user.setPath("/");
		/*CreateUserResult result = */client.createUser(user);

		AddUserToGroupRequest addUserToGroupRequest = new AddUserToGroupRequest()
			.withGroupName(APPMASTERSGROUP).withUserName(appId);
		client.addUserToGroup(addUserToGroupRequest);
		// ------------------------------------------------
		// ------------Creating the Default Folders--------
		// s3 = new AmazonS3Client(new
		// ClasspathPropertiesFileCredentialsProvider());
		// Region euWest = Region.getRegion(Regions.EU_WEST_1);
		// s3.setRegion(euWest);
		InputStream input = new ByteArrayInputStream(new byte[0]);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);
		s3.putObject(OPENBAASBUCKET, "apps/" + appId + "/", input, metadata);
		s3.putObject(OPENBAASBUCKET, "apps/" + appId + "/storage/", input, metadata);
		s3.putObject(OPENBAASBUCKET, "apps/" + appId + "/media/audio/", input, metadata);
		s3.putObject(OPENBAASBUCKET, "apps/" + appId + "/media/images/", input,	metadata);
		s3.putObject(OPENBAASBUCKET, "apps/" + appId + "/media/video/", input, metadata);
		sucess = true;
		return sucess;
	}
	@Override
	public boolean createUser(String appId, String userId, String userName)
			throws EntityAlreadyExistsException {
		System.out.println("appId: " + appId + " userId: " + userId
				+ " userName: " + userName);
		this.startIAM();
		CreateUserRequest user = new CreateUserRequest(userId);
		CreateAccessKeyRequest key = new CreateAccessKeyRequest();
		key.withUserName(user.getUserName());
		user.setRequestCredentials(key.getRequestCredentials());
		user.setPath("/");
		/*CreateUserResult result = */client.createUser(user); //Occasional error here, WHY?
		AddUserToGroupRequest addUserToGroupRequest = new AddUserToGroupRequest()
				.withGroupName(APPMASTERSGROUP).withUserName(appId);
		client.addUserToGroup(addUserToGroupRequest);
		return true;
	}
	@Override
	public boolean deleteFile(String fileDirectory) {
		this.startAWS();
		s3.deleteObject(OPENBAASBUCKET, fileDirectory);
		return true;
	}
	@Override
	public void deleteUser(String appId, String userId)
			throws NoSuchEntityException {
		try{
			this.startIAM();
			DeleteUserRequest user = new DeleteUserRequest(userId);
			client.deleteUser(user);
		}catch(Exception e){
			System.err.println(e.toString());
		}
	}

	public void streamClient() {

	}
}
