package infosistema.openbaas.rest_Models;


import infosistema.openbaas.modelInterfaces.Application;

import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement
public class DefaultApplication 
implements Application 
{

	private String createdAt;
	private String updatedAt;
	private String appId;
	private String alive;
	private String appName;
	private String confirmationEmail;
	/**
	 * Application constructor with no variables being affected, don't forget to
	 * affect them later.
	 */
	public DefaultApplication(){
	}
	
	public DefaultApplication(String appId){
		this.appId = appId;
		this.alive = "true";
	}
	public DefaultApplication(String appId, String date) {
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
	public void setInactive(){
		this.alive = "false";
	}
	/**
	 * Gets the application alive field (true -> an app is active, false -> it is not).
	 */
	@Override
	public String getAlive() {
		return this.alive;
	}
	@Override
	public String getAppName(){
		return this.appName;
	}
	@Override
	public void setAppName(String appName){
		this.appName = appName;
	}
	@Override
	public String getConfirmUsersEmail() {
		return confirmationEmail;
	}
	@Override
	public void setConfirmUsersEmail(String confirmationEmail) {
		this.confirmationEmail = confirmationEmail;
	}
}
