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
package infosistema.openbaas.comunication.message;

import infosistema.openbaas.utils.Log;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Message {

	public static final String TYPE = "type";
	public static final String MESSAGE_ID = "messageId";
	public static final String APP_ID = "appId";
	public static final String SESSION_TOKEN = "sessionToken";
	public static final String DATA = "data";
	public static final String ERROR_MESSAGE = "errorMessage";
	public static final String PARTICIPANTS = "participants";
	public static final String CHAT_ROOM_ID = "chatRoomId";
	public static final String SENDER_ID = "senderId";
	public static final String TEXT = "text";
	public static final String IMAGE = "image";
	public static final String VIDEO = "video";
	public static final String FILE = "file";
	public static final String AUDIO = "audio";
	public static final String HAS_IMAGE = "hasImage";
	public static final String HAS_VIDEO = "hasVideo";
	public static final String HAS_FILE = "hasFile";
	public static final String HAS_AUDIO = "hasAudio";
	public static final String MESSAGE = "message";
	public static final String CHAT_ROOM = "chatRoom";
	public static final String UNREADMESSAGES = "unreadMessages";
	//message types
	public static final String AUTHENTICATE = "AUTHENTICATE";
	public static final String OK = "OK";
	public static final String NOK = "NOK";
	public static final String CREATE_CHAT_ROOM = "CREATE_CHAT_ROOM";
	public static final String SENT_CHAT_MSG = "SENT_CHAT_MSG";
	public static final String RECV_CHAT_MSG = "RECV_CHAT_MSG";
	public static final String PING = "PING";
	public static final String PONG = "PONG";

	private String type;
	private String messageId;
	private String appId;
	private String sessionToken;
	private JSONObject data;
	
	public Message(String type, String appId) {
		this.type= type;
		this.appId = appId;
	}
	
	public Message(String type, String appId, String messageId) {
		this(type, appId);
		this.messageId = messageId;
	}
	
	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	public void setData(JSONObject data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		JSONObject retObj = new JSONObject();
		try {
			if (type != null) retObj.put(TYPE, type);
			if (messageId != null) retObj.put(MESSAGE_ID, messageId);
			if (appId != null) retObj.put(APP_ID, appId);
			if (sessionToken != null) retObj.put(SESSION_TOKEN, sessionToken);
			if (data != null) retObj.put(DATA, data);
		} catch (JSONException e) {
			Log.error("", this, "toString", "Error Serializing Message", e);
		}
		return retObj.toString();
	}
	
}
