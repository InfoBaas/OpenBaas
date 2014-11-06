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
