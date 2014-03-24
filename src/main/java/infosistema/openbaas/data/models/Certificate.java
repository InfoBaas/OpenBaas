package infosistema.openbaas.data.models;

import java.sql.Timestamp;

public class Certificate {

	private String certificatePath;
	private String APNSPassword;
	private String clientId;
	private String appId;
	private Timestamp createdDate;
	
	
	public Certificate() {
		super();
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public String getAPNSPassword() {
		return APNSPassword;
	}
	
	public void setAPNSPassword(String aPNSPassword) {
		APNSPassword = aPNSPassword;
	}
	
	public String getCertificatePath() {
		return certificatePath;
	}
	
	public void setCertificatePath(String certificatePath) {
		this.certificatePath = certificatePath;
	}

	public Timestamp getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Timestamp createdDate) {
		this.createdDate = createdDate;
	}



	public String getAppId() {
		return appId;
	}



	public void setAppId(String appId) {
		this.appId = appId;
	}
	
}
