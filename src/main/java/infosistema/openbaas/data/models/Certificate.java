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
