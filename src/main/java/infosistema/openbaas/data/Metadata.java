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
package infosistema.openbaas.data;

import infosistema.openbaas.utils.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONObject;

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

	public static Map<String, String> getNewMetadata(String location) {
		Map<String, String> fields = new HashMap<String, String>();
		if (location != null) fields.put(Metadata.LOCATION, location);
		return fields;
	}

	public static Metadata getMetadata(JSONObject obj) {
		Metadata metadata = null;
		if (obj != null) {
			try {
				metadata = new Metadata();
				DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy");
				try { 
					if(obj.has(Metadata.CREATE_DATE))
						metadata.setCreateDate(df.parse(obj.getString(Metadata.CREATE_DATE)));
				} catch (Exception e) { }
				if(obj.has(Metadata.CREATE_USER))
					metadata.setCreateUser(obj.getString(Metadata.CREATE_USER));
				try { 
					if(obj.has(Metadata.LAST_UPDATE_DATE))
						metadata.setLastUpdateDate(df.parse(obj.getString(Metadata.LAST_UPDATE_DATE)));
				} catch (Exception e) { }
				if(obj.has(Metadata.LAST_UPDATE_USER))
					metadata.setLastUpdateUser(obj.getString(Metadata.LAST_UPDATE_USER));
				if(obj.has(Metadata.LOCATION))
					metadata.setLocation(obj.getString(Metadata.LOCATION));
			} catch (Exception e) {
				Log.error("", "Metadata", "getMetadata", "Error serializing Metadata.", e);
				metadata = null;
			}
		}
		return metadata;
	}

}
