package infosistema.openbaas.comunication.bound;

import infosistema.openbaas.comunication.connector.WebSocketConnector;
import infosistema.openbaas.data.models.ChatRoom;
import infosistema.openbaas.middleLayer.ChatMiddleLayer;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class InboundWebSocket extends WebSocketServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected StreamInbound createWebSocketInbound(String protocol, HttpServletRequest request) {
		return new WebSocketConnector();
	}
	
}