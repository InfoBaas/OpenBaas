package infosistema.openbaas.modelInterfaces;


public interface Application {
	public void setCreationDate(String creationDate);
	public String getCreationDate();
	public String getUpdateDate();
	public void setInactive();
	public String getAlive();
	public String getAppName();
	public void setAppName(String appName);
	public String getAppId();
	public String getConfirmUsersEmail();
	public void setConfirmUsersEmail(String confirmUsersEmail);

}
