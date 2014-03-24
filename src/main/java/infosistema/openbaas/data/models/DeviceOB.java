package infosistema.openbaas.data.models;

import javapns.devices.implementations.basic.BasicDevice;

public class DeviceOB extends BasicDevice {

	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	
}
