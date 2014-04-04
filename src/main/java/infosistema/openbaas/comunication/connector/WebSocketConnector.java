package infosistema.openbaas.comunication.connector;

import infosistema.openbaas.comunication.bound.Outbound;
import infosistema.openbaas.comunication.message.Message;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.ChatMessage;
import infosistema.openbaas.data.models.ChatRoom;
import infosistema.openbaas.middleLayer.ChatMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.WsOutbound;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class WebSocketConnector extends MessageInbound implements IConnector {

	private Outbound outbound;
	private WsOutbound wsOutbound;
	private SessionMiddleLayer sessionMid = SessionMiddleLayer.getInstance();
	private ChatMiddleLayer chatMid = ChatMiddleLayer.getInstance();

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
