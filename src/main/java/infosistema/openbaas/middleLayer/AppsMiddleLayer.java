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
package infosistema.openbaas.middleLayer;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;


import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.mongodb.DBObject;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.Certificate;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.dataaccess.models.NotificationsModel;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

public class AppsMiddleLayer extends MiddleLayerAbstract {

	// *** MEMBERS *** //

	AppModel appModel = new AppModel();	
	NotificationsModel noteModel = new NotificationsModel();	
	MediaModel mediaModel = new MediaModel();	
	MediaMiddleLayer mediaMiddleLayer = MediaMiddleLayer.getInstance();
	// *** INSTANCE *** //
	
	private static AppsMiddleLayer instance = null;

	public static AppsMiddleLayer getInstance() {
		if (instance == null) instance = new AppsMiddleLayer();
		return instance;
	}
	
	private AppsMiddleLayer() {
		super();
	}

	// *** CREATE *** //
	
	/**
	 * returns true if created Application sucessfully.
	 * 
	 * @param appId
	 * @param appName
	 * @return
	 */
	public Application createApp(String appId, String appKey, String appName, boolean userEmailConfirmation,
			boolean AWS,boolean FTP,boolean FileSystem, JSONObject ImageRes,JSONObject ImageBars,
			JSONObject videoRes,JSONObject AudioRes, List<String> clientsList) {
		byte[] salt = null;
		byte[] hash = null;
		PasswordEncryptionService service = new PasswordEncryptionService();
		Application app = null;
		
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(appKey, salt);
			app = appModel.createApp(appId,appKey, hash, salt, appName, new Date().toString(), userEmailConfirmation,AWS,FTP,FileSystem,clientsList);
			if(ImageBars!=null && ImageBars.length()>0){
				appModel.createAppResolutions(ImageRes,appId,ModelEnum.bars);
			}
			if(ImageRes!=null && ImageRes.length()>0){
				appModel.createAppResolutions(ImageRes,appId,ModelEnum.image);
			}
			if(videoRes!=null && videoRes.length()>0){
				appModel.createAppResolutions(videoRes,appId,ModelEnum.video);
			}
			if(AudioRes!=null && AudioRes.length()>0){
				appModel.createAppResolutions(AudioRes,appId,ModelEnum.audio);
			}
		} catch (Exception e) {
			Log.error("", this, "createApp Login","", e); 
		}
		return app;
	}

	public boolean createAppFileSystem(String appId) {
		FileInterface fileModel = getAppFileInterface(appId);
		try{
			return fileModel.createApp(appId);
		} catch(EntityAlreadyExistsException e) {
			Log.error("", this, "createAppFileSystem", "Entity Already Exists.", e); 
		} catch(AmazonServiceException e) {
			Log.error("", this, "createAppFileSystem", "Amazon Service error.", e); 
		}catch(Exception e) {
			Log.error("", this, "createAppFileSystem", "An error ocorred.", e); 
		}
		return false;
	}


	// *** UPDATE *** //
	
	public Application updateAllAppFields(String appId, String alive, String newAppName, boolean confirmUsersEmail,
			boolean AWS,boolean FTP,boolean FILESYSTEM, List<String> clientsList) {
		if (appModel.appExists(appId)) {
			appModel.updateAppFields(appId, alive, newAppName, confirmUsersEmail,AWS,FTP,FILESYSTEM, clientsList);
			return appModel.getApplication(appId);
		}
		return null;
	}

	public void updateFilesRes(JSONObject imageRes,JSONObject imageBars,JSONObject videoRes,JSONObject audioRes, String appId, 
			List<String> oldImageRes, List<String> oldVideoRes,List<String> oldAudioRes) {
		Boolean flag = false;
		if(imageRes!=null && imageRes.length()>0){
			appModel.createAppResolutions(imageRes,appId,ModelEnum.image);
			flag =true;
		}
		if(imageBars!=null && imageBars.length()>0){
			appModel.createAppResolutions(imageBars,appId,ModelEnum.bars);
			flag =true;
		}
		if(flag){
			if(oldImageRes.size()>0){
				mediaMiddleLayer.deleteMediaByResolution(appId, ModelEnum.image,oldImageRes);
			}
		}
		if(videoRes!=null && videoRes.length()>0){
			appModel.createAppResolutions(videoRes,appId,ModelEnum.video);
			if(oldVideoRes.size()>0){
				mediaMiddleLayer.deleteMediaByResolution(appId, ModelEnum.video,oldVideoRes);
			}
		}
		if(audioRes!=null && audioRes.length()>0){
			appModel.createAppResolutions(audioRes,appId,ModelEnum.audio);
			if(oldAudioRes.size()>0){
				mediaMiddleLayer.deleteMediaByResolution(appId, ModelEnum.audio, oldAudioRes);
			}
		}
	}

	// *** DELETE *** //
	
	public boolean removeApp(String appId) {
		return appModel.deleteApp(appId);
	}


	// *** GET LIST *** //

	protected List<DBObject> getAllSearchResults(String appId, String userId, String url, Double latitude, Double longitude, Double radius, JSONObject query, String orderType, String orderBy, ModelEnum type, List<String> toShow) throws Exception {
		return null;
	}


	// *** GET *** //
	
	public Application getApp(String appId) {	
		Application temp = new Application(appId);
		temp = appModel.getApplication(appId);
		return temp;
	}
	
	public HashMap<String, String> getAuthApp(String appId) {	
		HashMap<String, String> temp = new HashMap<String, String>();
		temp = appModel.getApplicationAuth(appId);
		return temp;
	}


	// *** EXISTS *** //

	public boolean appExists(String appId) {
		return appModel.appExists(appId);
	}

	
	// *** OTHERS *** //
	
	public void reviveApp(String appId){
		appModel.reviveApp(appId);
	}

	public Boolean authenticateApp(String appId, String appKey) {
		try {
			HashMap<String, String> fieldsAuth = getAuthApp(appId);
			byte[] salt = null;
			byte[] hash = null;
			if(fieldsAuth.containsKey(Application.HASH) && fieldsAuth.containsKey(Application.SALT)){
				salt = fieldsAuth.get(Application.SALT).getBytes("ISO-8859-1");
				hash = fieldsAuth.get(Application.HASH).getBytes("ISO-8859-1");
			}
			PasswordEncryptionService service = new PasswordEncryptionService();
			Boolean authenticated = false;
			authenticated = service.authenticate(appKey, hash, salt);
			return authenticated;
		} catch (Exception e) {
			Log.error("", "", "authenticateAPP", "", e); 
		} 	
		return false;
	}

	public Certificate createCertificate(String appId, String certificatePath, String aPNSPassword, String clientId) {
		Certificate cert = new Certificate();
		cert.setAPNSPassword(aPNSPassword);
		cert.setCertificatePath(certificatePath);
		cert.setClientId(clientId);
		cert.setCreatedDate(new Timestamp(new Date().getTime()));
		Boolean flag = noteModel.createUpdateCertificate(appId, cert);
		if(flag)
			return cert;
		return null;
	}

	

}
