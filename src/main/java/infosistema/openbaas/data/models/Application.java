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
package infosistema.openbaas.data.models;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement
public class Application {

	public final static String APNS_CERTIFICATION_PATH = "APNSCertificationPath";
	public final static String APNS_PASSWORD = "APNSPassword";
	public final static String APNS_CLIENT_ID = "APNSClientId";
	public final static String CREATION_DATE = "creationdate";
	public final static String ALIVE = "alive";
	public final static String APP_NAME = "appName";
	public final static String IMAGE_RES = "imageRes";
	public final static String IMAGE_BARS = "imageBars";
	public final static String VIDEO_RES = "videoRes";
	public final static String AUDIO_RES = "audioRes";
	public final static String CLIENTS_LIST = "clientsList";
	public final static String CONFIRM_USERS_EMAIL = "confirmUsersEmail";
	public final static String UPDATE_DATE = "updateDate";
	public final static String APP_KEY = "appKey";
	public final static String SALT = "salt";
	public final static String HASH = "hash";
	public final static String ORIGINAL = "original";
	
	
	public final static String INCLUDEMISSES = "includeMisses";
	public final static String USERS = "users";
	 
	private String createdAt;
	private String updatedAt;
	private String _id;
	private String alive;
	private String appName;
	private String appKey;
	private Boolean confirmationEmail;
	private Boolean AWS;
	private Boolean FTP;
	private Boolean FileSystem;
	private Boolean Dropbox;
	private Map<String,String> imageResolutions;
	private Map<String,String> videoResolutions;
	private Map<String,String> audioResolutions;
	private Map<String,String> barsColors;
	private List<String> clients;
	


	/**
	 * Application constructor with no variables being affected, don't forget to
	 * affect them later.
	 */
	public Application(){
	}
	
	public Application(String _id){
		this._id = _id;
		this.alive = "true";
	}
	public Application(String _id, String date) {
		this._id = _id;
		createdAt = date;
		this.alive = "true";
	}
	public void setCreationDate(String creationDate){
		this.createdAt = creationDate;
	}
	public String getCreationDate() {
		return this.createdAt;
	}

	public String getUpdateDate() {
		return this.updatedAt;
	}
	public void setUpdateDate(String updatedAt){
		this.updatedAt = updatedAt;
	}
	public String get_id() {
		return this._id;
	}
	public void set_id(String _id){
		this._id = _id;
	}
	public void setAlive(String alive){
		this.alive = alive;
	}
	public void setInactive(){
		this.alive = "false";
	}
	/**
	 * Gets the application alive field (true -> an app is active, false -> it is not).
	 */
	public String getAlive() {
		return this.alive;
	}
	public String getAppName(){
		return this.appName;
	}
	public void setAppName(String appName){
		this.appName = appName;
	}
	public Boolean getConfirmUsersEmail() {
		return confirmationEmail;
	}
	public void setConfirmUsersEmail(Boolean confirmationEmail) {
		this.confirmationEmail = confirmationEmail;
	}
	
	public Boolean getAWS() {
		return AWS;
	}

	public void setAWS(Boolean aWS) {
		AWS = aWS;
	}

	public Boolean getFTP() {
		return FTP;
	}

	public void setFTP(Boolean fTP) {
		FTP = fTP;
	}

	public Boolean getFileSystem() {
		return FileSystem;
	}

	public void setFileSystem(Boolean fileSystem) {
		FileSystem = fileSystem;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public Map<String,String> getAudioResolutions() {
		return audioResolutions;
	}

	public void setAudioResolutions(Map<String,String> audioResolutions) {
		this.audioResolutions = audioResolutions;
	}

	public Map<String,String> getVideoResolutions() {
		return videoResolutions;
	}

	public void setVideoResolutions(Map<String,String> videoResolutions) {
		this.videoResolutions = videoResolutions;
	}

	public Map<String,String> getImageResolutions() {
		return imageResolutions;
	}

	public void setImageResolutions(Map<String,String> imageResolutions) {
		this.imageResolutions = imageResolutions;
	}

	public Map<String,String> getBarsColors() {
		return barsColors;
	}

	public void setBarsColors(Map<String,String> barsColors) {
		this.barsColors = barsColors;
	}

	public List<String> getClients() {
		return clients;
	}

	public void setClients(List<String> clients) {
		this.clients = clients;
	}

	public Boolean getDropbox() {
		return Dropbox;
	}

	public void setDropbox(Boolean dropbox) {
		Dropbox = dropbox;
	}
}
