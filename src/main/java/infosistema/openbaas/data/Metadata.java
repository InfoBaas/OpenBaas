package infosistema.openbaas.data;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Metadata {

	public static final String CREATE_USER = "createUser";
	public static final String CREATE_DATE = "createDate";
	public static final String LAST_UPDATE_USER = "lastUpdateUser";
	public static final String LAST_UPDATE_DATE = "lastUpdateDate";
	public static final String LOCATION = "location";
	
    private String createUser;
    private Date createDate;
    private String lastUpdateUser;
    private Date lastUpdateDate;
    private String location;

	public Metadata(){
	}
	
	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getLastUpdateUser() {
		return lastUpdateUser;
	}

	public void setLastUpdateUser(String lastUpdateUser) {
		this.lastUpdateUser = lastUpdateUser;
	}

	public Date getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}


}
