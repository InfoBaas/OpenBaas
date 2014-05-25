/*****************************************************************************************
Infosistema - OpenBaas
Copyright(C) 2002-2014 Infosistema, S.A.
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
www.infosistema.com
info@openbaas.com
Av. José Gomes Ferreira, 11 3rd floor, s.34
Miraflores
1495-139 Algés Portugal
****************************************************************************************/
package infosistema.openbaas.dataaccess.files;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.ByteArrayInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

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
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

public class AwsModel implements FileInterface {

	private static final String PATH_FORMAT = "apps/%s/media/%s/%s.%s";
	private static AwsModel instance;
	private AmazonS3 s3;
	private AmazonIdentityManagementClient client;

	public static AwsModel getInstance() {
		if (instance == null) {
			instance = new AwsModel();
			instance.startAWS();
			instance.startIAM();
		}
		return instance;
	}

	private AwsModel() { }
	
	// *** PRIVATE *** //
	
	/**
	 * Starts the S3 Amazon connection.
	 */
	private void startAWS() {
		s3 = new AmazonS3Client(new ClasspathPropertiesFileCredentialsProvider());
		Region euWest = Region.getRegion(Regions.EU_WEST_1);
		s3.setRegion(euWest);
	}

	/**
	 * Starts the Amazon Identity Management Client
	 */
	private void startIAM() {
		client = new AmazonIdentityManagementClient(new ClasspathPropertiesFileCredentialsProvider());
	}

	//*** PRIVATE *** //

	private String getFilePath(String appId, ModelEnum type, String id, String extension) {
		return String.format(PATH_FORMAT, appId, type.toString(), id, extension);
	}
	
	// *** CREATE *** //
	
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
	@Override
	public boolean createApp(String appId) throws EntityAlreadyExistsException, AmazonServiceException {
		this.startAWS();
		this.startIAM();
		boolean sucess = false;
		// ------------Creating the AppMaster User + Adding it to the AppMasters group (limited permissions)
		CreateUserRequest user = new CreateUserRequest(appId);
		CreateAccessKeyRequest key = new CreateAccessKeyRequest();
		key.withUserName(user.getUserName());
		user.setRequestCredentials(key.getRequestCredentials());
		user.setPath("/");
		client.createUser(user);

		AddUserToGroupRequest addUserToGroupRequest = new AddUserToGroupRequest()
			.withGroupName(Const.getAwsAppMastersGroup()).withUserName(appId);
		client.addUserToGroup(addUserToGroupRequest);
		// ------------------------------------------------
		// ------------Creating the Default Folders--------
		InputStream input = new ByteArrayInputStream(new byte[0]);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);
		s3.putObject(Const.getAwsOpenBaasBucket(), "apps/" + appId + "/", input, metadata);
		s3.putObject(Const.getAwsOpenBaasBucket(), "apps/" + appId + "/media/storage/", input, metadata);
		s3.putObject(Const.getAwsOpenBaasBucket(), "apps/" + appId + "/media/audio/", input, metadata);
		s3.putObject(Const.getAwsOpenBaasBucket(), "apps/" + appId + "/media/images/", input,	metadata);
		s3.putObject(Const.getAwsOpenBaasBucket(), "apps/" + appId + "/media/video/", input, metadata);
		sucess = true;
		return sucess;
	}
	
	
	// *** UPLOAD *** //

	@Override
	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream) throws AmazonServiceException, AmazonClientException {
		this.startAWS();
		String filePath = getFilePath(appId, type, id, extension);
		s3.putObject(new PutObjectRequest(Const.getAwsOpenBaasBucket(), filePath, stream, new ObjectMetadata()));
		return filePath;
	}
	
	
	// *** DOWNLOAD *** //
	
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
	@Override
	public byte[] download(String appId, ModelEnum type, String id, String extension, String quality, String bars) throws IOException {
		OutputStream soutputStream=null;
		byte[] byteArray = null;
		try{
			this.startAWS();
			StringBuffer directory = new StringBuffer("apps/" + appId + "/media/" + type.toString() + "/" + id);
			if(extension!=null)
				directory.append("."+extension);
			S3Object object = s3.getObject(new GetObjectRequest(Const.getAwsOpenBaasBucket(), directory.toString()));
			S3ObjectInputStream s3ObjInputStream = object.getObjectContent();
			byteArray = IOUtils.toByteArray(s3ObjInputStream);
			soutputStream = new FileOutputStream(new File(directory.toString()));
			int read = 0;
			byte[] bytes = new byte[1024];
			S3Object object2 = s3.getObject(new GetObjectRequest(Const.getAwsOpenBaasBucket(), directory.toString()));
			
			while ((read = object2.getObjectContent().read(bytes)) != -1) {
				soutputStream.write(bytes, 0, read);
			}	 
		} catch(Exception e) {
			Log.error("", this, "download", "An error ocorred.", e); 
		}finally{
			soutputStream.close();
		}
		return byteArray;
	}

	// *** DETETE *** //
	
	@Override
	public boolean deleteFile(String appId, ModelEnum type, String id, String extension) {
		this.startAWS();
		s3.deleteObject(Const.getAwsOpenBaasBucket(), getFilePath(appId, type, id, extension));
		return true;
	}
	

	@Override
	public Boolean delFilesResolution(String appId, ModelEnum type,	List<String> filesRes) {
		// TODO 
		return null;
	}

}
