package infosistema.openbaas.data.models;

import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement
public class Application {

	private String createdAt;
	private String updatedAt;
	private String appId;
	private String alive;
	private String appName;
	private String appKey;
	private Boolean confirmationEmail;
	private Boolean AWS;
	private Boolean FTP;
	private Boolean FileSystem;
	


	/**
	 * Application constructor with no variables being affected, don't forget to
	 * affect them later.
	 */
	public Application(){
	}
	
	public Application(String appId){
		this.appId = appId;
		this.alive = "true";
	}
	public Application(String appId, String date) {
		this.appId = appId;
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
	public String getAppId() {
		return this.appId;
	}
	public void setAppId(String appId){
		this.appId = appId;
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
}
