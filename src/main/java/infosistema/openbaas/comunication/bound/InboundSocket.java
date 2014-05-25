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
				server.setPerformancePreferences(0, 2, 1);
				server.setReceiveBufferSize(8192);
			} catch (IOException e) {
				Log.error("", "SocketWait", "SocketWait", "Could not listen on port " + port);
			}
		}
	}
}