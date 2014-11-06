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

import infosistema.openbaas.utils.Log;

import java.io.Serializable;
import java.util.Date;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


public class ChatRoom implements Serializable{
	private static final long serialVersionUID = 1L;

	private String _id;
	private String roomName;
	private Boolean flagNotification;
	private String roomCreator;
	private String[] participants;
	private Date createdDate;
	private Integer unreadMessages;
	
	public final static String CREATEDDATE = "createdDate";
	public final static String FLAG_NOTIFICATION = "flagNotification";
	public final static String PARTICIPANTS = "participants";
	public final static String ROOM_NAME = "roomName";
	public final static String UNREADMSGS = "unreadMessages";
	public final static String ROOM_CREATOR = "roomCreator";
	public final static String _ID = "_id";
	public final static String SEPARATOR = ";";

	public ChatRoom() {
	}
	
	public ChatRoom(String _id, String roomName, Boolean flagNotification) {
		this._id = _id;
		this.roomName = roomName;
		this.flagNotification = flagNotification;
	}

	public String get_id() {
		return _id;
	}

	public void set_id(String _id) {
		this._id = _id;
	}

	public String getRoomName() {
		return roomName;
	}

	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

	public Boolean getFlagNotification() {
		return flagNotification;
	}

	public void setFlagNotification(Boolean flagNotification) {
		this.flagNotification = flagNotification;
	}

	public String getRoomCreator() {
		return roomCreator;
	}

	public void setRoomCreator(String roomCreator) {
		this.roomCreator = roomCreator;
	}

	public String[] getParticipants() {
		return participants;
	}

	public void setParticipants(String[] participants) {
		this.participants = participants;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Integer getUnreadMessages() {
		return unreadMessages;
	}

	public void setUnreadMessages(Integer unreadMessages) {
		this.unreadMessages = unreadMessages;
	}
	
	public JSONObject serialize() {
		JSONObject retObj = new JSONObject();
		try {
			if (_id != null) retObj.put(_ID, _id);
			if (roomName != null) retObj.put(ROOM_NAME, roomName);
			if (flagNotification != null) retObj.put(FLAG_NOTIFICATION, flagNotification);
			if (roomCreator != null) retObj.put(ROOM_CREATOR, roomCreator);
			if (participants != null) {
				for (int i = 0; i < participants.length; i++)
					retObj.accumulate(PARTICIPANTS, participants[i]);
			}
			if (createdDate != null) retObj.put(CREATEDDATE, "" + createdDate.getTime());
			if (unreadMessages != null) retObj.put(UNREADMSGS, "" + unreadMessages);
		} catch (JSONException e) {
			Log.error("", this, "serialize", "Error Serializing Chat", e);
		}
		return retObj;
	}

}
