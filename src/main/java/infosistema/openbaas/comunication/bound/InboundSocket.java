package infosistema.openbaas.comunication.bound;

import infosistema.openbaas.comunication.connector.SocketConnector;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class InboundSocket {

	private static Integer nextPort = Const.SOCKET_PORT_MIN; 

	public static void createServerSockets() {
		for (int port = Const.SOCKET_PORT_MIN; port <= Const.SOCKET_PORT_MAX; port++) {
			Thread t = new Thread(new InboundSocket.SocketWait(port));
			t.start();
		}
	}
	
	public static Integer getNextPort() {
		nextPort++;
		if (nextPort > Const.SOCKET_PORT_MAX) nextPort = Const.SOCKET_PORT_MIN;
		return nextPort; 
	}
	
	public static class SocketWait implements Runnable {
		ServerSocket server = null; 
		
		public void run(){
			while(true){
				try{
					Socket socket = server.accept();
					SocketConnector msg = new SocketConnector(socket);
					Thread t = new Thread(msg);
					t.start();
				} catch (IOException e) {
					Log.error("", "SocketWait", "createSocket", "Accept failed", e);
				}
			}
		}
		
		SocketWait(int port) {
			try{
				server = new ServerSocket(port);
			} catch (IOException e) {
				Log.error("", "SocketWait", "SocketWait", "Could not listen on port " + port);
			}
		}
	}
}