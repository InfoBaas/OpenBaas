package infosistema.openbaas.dataaccess.files;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;

public class DropboxModel implements FileInterface {

	private static DropboxModel instance;
	final private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	private DropboxAPI<WebAuthSession> mDBApi;
	private static AppModel appModel = null;
	
	public static DropboxModel getInstance() {
		if (instance == null) instance = new DropboxModel();
		appModel = new AppModel();
		return instance;
	}

	private DropboxModel() {
	}

		
	private Boolean authenticateDropbox() {
		Boolean res = false;
		try {
			AppKeyPair appKeys = new AppKeyPair(Const.getDROPBOX_CONSUMER_APPKEY(), Const.getDROPBOX_CONSUMER_APPSECRET());
			WebAuthSession session = new WebAuthSession(appKeys, ACCESS_TYPE);
			mDBApi = new DropboxAPI<WebAuthSession>(session);
			String ACCESS_TOKEN_KEY = Const.getDROPBOX_ACCESS_TOKEN_KEY();
			String ACCESS_TOKEN_SECRET = Const.getDROPBOX_ACCESS_TOKEN_SECRET();
			AccessTokenPair reAuthTokens = new AccessTokenPair(ACCESS_TOKEN_KEY, ACCESS_TOKEN_SECRET);
			mDBApi.getSession().setAccessTokenPair(reAuthTokens);			
			res = true;
			Log.info("", "Media", "authenticateDropbox", "Connected to "+mDBApi.accountInfo().displayName);
		} catch (DropboxException e) {
			Log.error("", "Media", "authenticateDropbox", "Error in: "+ e.toString());
		}
		return res;
	}
	
	// *** CREATE *** //
	
	@Override
	public boolean createApp(String appId) throws Exception {
		return true;
	}

	
	// *** UPLOAD *** //

	@Override
	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream){
		String filePath = null;
		try{
			Boolean auth = authenticateDropbox();
			if(auth){
				String dirPath = FilesUtils.getDirPath(appId, type);
				filePath = FilesUtils.getFilePath(dirPath, id, extension);				    
			    byte[] bytes = IOUtils.toByteArray(stream);
				int a = bytes.length;
				InputStream is1 = new ByteArrayInputStream(bytes); 
			    long size = a;
				mDBApi.putFile(filePath, is1, size, null, null);
				Log.info("", "Media", "upload Dropbox", "Upload file to dropbox: "+filePath);
			}
		}catch(Exception e){
			Log.error("", "Media", "upload Dropbox", "Error in: "+ e.toString());
		}
		return filePath;
	}
	
	@Override
	public byte[] download(String appId, ModelEnum type, String id, String extension, String quality, String bars){
		byte[] res = null;
		String filePath = null;
		String filePathOriginal = FilesUtils.getFilePath(FilesUtils.getDirPath(appId, type), id, extension);
		if(quality.equals("") || quality==null) quality=FilesUtils.ORIGINAL;
		if(quality.equals(FilesUtils.ORIGINAL)){
			filePath = FilesUtils.getFilePath(FilesUtils.getDirPath(appId, type), id, extension);
		}else{
			if(type.equals(ModelEnum.image)) extension = Image.EXTENSION;
			filePath = FilesUtils.getFilePathWithQuality(FilesUtils.getDirPath(appId, type), id, quality, extension,bars);
		}
		try{
			Boolean auth = authenticateDropbox();
			if(auth){
				List<Entry> search = mDBApi.search(filePath, id, 1, false);
				if(search.size()>0){
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					DropboxFileInfo fileInfo = mDBApi.getFile(filePath, null, bos , null);
					res = bos.toByteArray();
					Log.info("", "Media", "Download Dropbox", "Download file from dropbox: "+fileInfo.getMetadata().fileName());
				}else {
					String qualityRes = appModel.getFileQuality(appId, type, quality);
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					mDBApi.getFile(filePathOriginal, null, bos , null);
					byte[] byteArray = bos.toByteArray();
					if(qualityRes!=null) qualityRes=qualityRes.toUpperCase();
					res = FilesUtils.getInstance().resizeFile(appId, byteArray, qualityRes, type, null, extension,filePath,bars);
					InputStream is = new ByteArrayInputStream(res);
					mDBApi.putFile(filePath, is, res.length, null, null);
					Log.info("", "Media", "Download Dropbox", "Upload/Download resized file to dropbox: "+filePath);
				}
			}
		}catch(Exception e){
			Log.error("", "Media", "Download Dropbox", "Error in: "+ e.toString());
		}
		return res;
	}

	
	// *** DELETE *** //
	
	@Override
	public boolean deleteFile(String appId, ModelEnum type, String id, String extension) {
		try {
			Boolean auth = authenticateDropbox();
			if(auth){
				String filePath = FilesUtils.getFilePath(FilesUtils.getDirPath(appId, type), id, extension);
				mDBApi.delete(filePath);
			}
			return true;
		} catch (Exception e) {
			Log.error("", this, "delete", "An error ocorred.", e); 
			return false;
		}
	}
	
	
	@Override
	public Boolean delFilesResolution(String appId, ModelEnum type,	List<String> filesRes) {
		Boolean res = false;
		String dirPath = FilesUtils.getDirPath(appId, type);
		
		List<Entry> searchList;
		try {
			Boolean auth = authenticateDropbox();
			if(auth){
				searchList = mDBApi.search(dirPath, "image", -1, false);
				if(searchList.size()>0){
					for(int i = 0; i<searchList.size(); i++){
						Entry curr = searchList.get(i);
						String extension = FilenameUtils.getExtension(curr.fileName());
						Iterator<String> it = filesRes.iterator();
						while(it.hasNext()){
							String fileRes = it.next();
							if(curr.fileName().endsWith(fileRes+"."+extension)){
								try {
									mDBApi.delete(curr.path+curr.fileName());
									res = true;
								} catch (Exception e) {
									Log.error("", this, "delete", "An error ocorred.", e); 
									res = false;
								}
							}	
						}	
					}
				}
			}
		} catch (Exception e) {
			Log.error("", this, "deleteRes", "An error ocorred.", e); 
		}
		return res;
	}
}
