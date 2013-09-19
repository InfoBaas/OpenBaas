package Model;

import java.io.File;
import java.io.IOException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;

import FileSystemModels.AWSModel;

public class FileSystemModel {

	static AWSModel aws;
	private static String FILESYSTEM = "aws";
	public FileSystemModel(){
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			this.aws = new AWSModel();
		else
			System.out.println("FileSystem not yet implemented.");
	}

	public boolean createUser(String appId, String userId, String userName) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try{
				return this.aws.createUser(appId, userId, userName);
			}catch(EntityAlreadyExistsException e){
				System.out.print("Entity Already Exists.");
			}
		else{
			System.out.println("FileSystem not yet implemented.");
			return true;
		}
			
		return false;
	}

	public boolean download(String appId, String mediafolder, String requestType,
			String id) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try {
				return this.aws.download(appId, mediafolder, requestType, id);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else{
			System.out.println("FileSystem not yet implemented.");
			return true;
		}
		return false;
	}

	public boolean upload(String appId, String destinationDirectory, String id,
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

	public boolean createAppAWS(String appId) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			try{
			return this.aws.createApp(appId);
			}catch(EntityAlreadyExistsException e){
				System.out.print("Entity Already Exists.");
			}catch(AmazonServiceException e){
				System.out.println("Amazon Service Exception.");
			}
		else{
			System.out.println("FileSystem not yet implemented.");
			return true;
		}
		return false;
	}

	public boolean deleteFile(String fileDirectory) {
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

	public void deleteUser(String appId, String userId) {
		if(FILESYSTEM.equalsIgnoreCase("aws"))
			this.aws.deleteUser(appId, userId);
		else{
			System.out.println("FileSystem not yet implemented.");
		}
	}
	
}
