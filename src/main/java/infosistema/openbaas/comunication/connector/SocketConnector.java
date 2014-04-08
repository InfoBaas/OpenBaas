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
	Socket socketToClose = null;

	//Constructor
	public SocketConnector(Socket socket) {
		outbound = new Outbound(this);
		try{
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			socketToClose = socket; 
		} catch (IOException e) {
			Log.error("", this, "SocketMessage", "Error in Constructor", e);
		}
	}

	public void run() {
		String message = "";
		char[] cbuf = new char[1024];
		int n = 0;
		while (n >= 0) {
			try{
				if ((n = in.read(cbuf)) < 0) continue;
				String smtp = CharBuffer.wrap(cbuf).toString();
				//Log.error("", this, "___0", "___smtp: " + smtp);
				while (smtp.contains("\n")) {
					message += smtp.substring(0, smtp.indexOf("\n"));  
					Log.error("", this, "######0", "########msg1: " + message);
					outbound.processMessage(message);
					if (smtp.length() > smtp.indexOf("\n") + 1 && smtp.charAt(smtp.indexOf("\n") + 1) != 0)
						smtp = smtp.substring(smtp.indexOf("\n") + 1);
					else 
						smtp = "";
					message = "";
				}
				message += smtp; 
			}catch (Exception e) {
				message = "";
				Log.error("", this, "run", "Error running thread", e);
			}
		}
		try{
			Outbound.removeUserOutbound(outbound.getUserId());
			in.close();
			out.close();
			socketToClose.close();
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

