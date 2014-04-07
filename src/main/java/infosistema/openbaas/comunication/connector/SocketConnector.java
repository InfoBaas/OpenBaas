package infosistema.openbaas.comunication.connector;

import infosistema.openbaas.comunication.bound.Outbound;
import infosistema.openbaas.comunication.message.Message;
import infosistema.openbaas.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.CharBuffer;

public class SocketConnector implements Runnable, IConnector {

	private Outbound outbound;
	BufferedReader in = null;
	PrintWriter out = null;

	//Constructor
	public SocketConnector(Socket socket) {
		outbound = new Outbound(this);
		try{
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			Log.error("", this, "SocketMessage", "Error in Constructor", e);
		}
	}

	public void run() {
		String message = "";
		while (message != null) {
			try{
				message = in.readLine();
				Log.error("", this, "######0", "########msg1: " + message);
				if (message != null) 
					outbound.processMessage(message);
			}catch (Exception e) {
				Log.error("", this, "run", "Error running thread", e);
			}
		}
		try{
			Outbound.removeUserOutbound(outbound.getUserId());
			in.close();
			out.close();
		}catch (Exception e) {
			Log.error("", this, "run", "Error closing thread", e);
		}
	}

	public boolean sendMessage(Message message) {
		try {
			Log.error("", this, "######7", "########msg2: " + message.toString());
			out.println(CharBuffer.wrap(message.toString()));
		} catch (Exception e) {
			Log.error("", this, "sendMessage", "Error sending Message", e);
			return false;
		}
		return true;
	}

}

