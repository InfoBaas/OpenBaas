package infosistema.openbaas.comunication.bound;

import infosistema.openbaas.comunication.connector.WebSocketConnector;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;

public class InboundWebSocket extends WebSocketServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected StreamInbound createWebSocketInbound(String protocol, HttpServletRequest request) {
		return new WebSocketConnector();
	}
	
}