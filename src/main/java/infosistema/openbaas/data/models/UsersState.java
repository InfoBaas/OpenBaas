package infosistema.openbaas.data.models;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UsersState {

	String _id;
	Boolean online;
	Date lastUpdateDate;
	
	public UsersState(){
		
	}

	public UsersState(String _id, Boolean online, Date lastUpdateDate) {
		super();
		this._id = _id;
		this.online = online;
		this.lastUpdateDate = lastUpdateDate;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public Boolean getOnline() {
		return online;
	}

	public void setOnline(Boolean online) {
		this.online = online;
	}

	public Date getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(Date lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
	}
	
	
	
}
