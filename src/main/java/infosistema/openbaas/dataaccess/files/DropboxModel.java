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
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxFileInfo;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.RequestTokenPair;
import com.dropbox.client2.session.Session.AccessType;
import com.dropbox.client2.session.WebAuthSession;

public class DropboxModel implements FileInterface {

	private static DropboxModel instance;
	final private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	private DropboxAPI<WebAuthSession> mDBApi;
	
	private static AppModel appModel = null;
	
	public static DropboxModel getInstance() {
		if (instance == null) instance = new DropboxModel();
		appModel = AppModel.getInstance();
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
	
	public static void getDropboxTokens() {
		try {
			AppKeyPair appKeys = new AppKeyPair(Const.getDROPBOX_CONSUMER_APPKEY(), Const.getDROPBOX_CONSUMER_APPSECRET()); //Both from Dropbox developer website
	        WebAuthSession session = new WebAuthSession(appKeys, AccessType.APP_FOLDER);
	
	        DropboxAPI<WebAuthSession> mDBApi = new DropboxAPI<WebAuthSession>(session);
        
			System.out.println(mDBApi.getSession().getAuthInfo().url);
			/*Pausa!!! ir ao url e dar o allow! Depois prosseguir em debug para obter key + secret*/
			
			AccessTokenPair tokenPair = mDBApi.getSession().getAccessTokenPair();
			// wait for user to allow app in above URL, 
			// then return back to executing code below
			RequestTokenPair tokens = new RequestTokenPair(tokenPair.key, tokenPair.secret);
			mDBApi.getSession().retrieveWebAccessToken(tokens); // completes initial auth
			 
			//these two calls will retrive access tokens for future use
			System.out.println("key:"+session.getAccessTokenPair().key);    // store String returned by this call somewhere
			System.out.println("secret:"+session.getAccessTokenPair().secret);
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
