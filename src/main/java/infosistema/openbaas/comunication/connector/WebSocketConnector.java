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
package infosistema.openbaas.comunication.connector;

import infosistema.openbaas.comunication.bound.Outbound;
import infosistema.openbaas.comunication.message.Message;
import infosistema.openbaas.utils.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.WsOutbound;

public class WebSocketConnector extends MessageInbound implements IConnector {

	private Outbound outbound;
	private WsOutbound wsOutbound;

	@Override
	public void onOpen(WsOutbound wsOutbound){
		try {
			this.wsOutbound = wsOutbound;
			this.outbound = new Outbound(this);
			Outbound.addOutbound(outbound);
		} catch (Exception e) {
			//TODO
		}
	}

	@Override
	public void onClose(int status){
		//TODO
	}

	@Override
	public void onTextMessage(CharBuffer cb) throws IOException {
		try {
			String message = CharBuffer.wrap(cb).toString();
			outbound.processMessage(message);
		}catch (Exception e) {
			Log.error("", this, "run", "Error running thread", e);
		}
	}

	@Override
	public void onBinaryMessage(ByteBuffer bb) throws IOException{
	}


	/*** SEND MESSAGES ***/
	
	@Override
	public boolean sendMessage(Message message) {
		try {
			this.wsOutbound.writeTextMessage(CharBuffer.wrap(message.toString()));
			this.wsOutbound.flush();
		} catch (IOException e) {
			Log.error("", this, "sendMessage", "Error sending Message", e);
			return false;
		}
		return true;
	}

}

