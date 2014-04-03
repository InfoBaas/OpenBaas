package infosistema.openbaas.comunication.bound;

import infosistema.openbaas.comunication.connector.SocketConnector;
import infosistema.openbaas.utils.Const;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class InboundSocket {

	private static Integer nextPort = Const.SOCKET_PORT_MIN; 

	public static void createServerSockets() {
		for (int port = Const.SOCKET_PORT_MIN; port <= Const.SOCKET_PORT_MAX; port++) {
			createSocket(port);
		}
	}
	
	private static void createSocket(int port) {
		try{
			ServerSocket server = new ServerSocket(port);
			while(true){
				try{
					Socket socket = server.accept();
					SocketConnector msg = new SocketConnector(socket);
					Thread t = new Thread(msg);
					t.start();
				} catch (IOException e) {
					System.out.println("Accept failed: 4444");
					System.exit(-1);
				}
			}
		} catch (IOException e) {
			System.out.println("Could not listen on port 4444");
		}
	}
	
	public static Integer getNextPort() {
		nextPort++;
		if (nextPort > Const.SOCKET_PORT_MAX) nextPort = Const.SOCKET_PORT_MIN;
		return nextPort; 
	}
}