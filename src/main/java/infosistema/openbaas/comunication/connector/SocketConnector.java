package infosistema.openbaas.comunication.connector;

import infosistema.openbaas.comunication.bound.Outbound;
import infosistema.openbaas.comunication.message.Message;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Calendar;

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

	public void run1() {
		Calendar ini = Calendar.getInstance();
		String message = "";
		int n = 0;
		while (n >= 0) {
			try{
				char[] cbuf = new char[4096];
				if ((n = in.read(cbuf)) < 0) continue;
				String smtp = CharBuffer.wrap(cbuf).toString();
				if (smtp.indexOf(0) > 0) { 
					smtp = smtp.replaceAll("\0", "");
				}
				while (smtp.contains("\n")) {
					message += smtp.substring(0, smtp.indexOf("\n"));  
					if (message.indexOf("\0") > 0) {
						Log.error("", this, "###", "### 4 - null - message total: " + message);
						message = message.replaceAll("\0", "");
					}
					outbound.processMessage(message);
					if (smtp.length() > smtp.indexOf("\n") + 1 && smtp.charAt(smtp.indexOf("\n") + 1) != 0) {
						smtp = smtp.substring(smtp.indexOf("\n") + 1);
					} else 
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
		System.out.println("tempo: "+ (Calendar.getInstance().getTimeInMillis() - ini.getTimeInMillis()));
	}

	public void run() {
		Calendar ini = Calendar.getInstance();
		StringBuilder message = new StringBuilder();
		int readLength = 0;
		char[] cbuf;
		try {
			cbuf = new char[socketToClose.getReceiveBufferSize()];
		} catch (SocketException e1) {
			Log.error("", this, "run", "Error getting the socket buffer size");
			close();
			return;
		}
		while (readLength >= 0) {
			try{
				Arrays.fill(cbuf, Const.CHAR_NULL);
				if ((readLength = in.read(cbuf)) < 0) continue;
				int newLinePos = -1;
				int startPos = 0;
				while ((newLinePos = nextNewLine(cbuf, startPos, readLength)) > 0)  {
					message.append(cbuf, startPos, (newLinePos-startPos));
					outbound.processMessage(message.toString());
					startPos = newLinePos +1;
					//System.out.println(message.toString());
					message.setLength(0);
				}
				message.append(cbuf, startPos, (readLength-startPos)); 
			}catch (Exception e) {
				message.setLength(0);
				Log.error("", this, "run", "Error running thread", e);
			}
		}
		System.out.println("tempo: "+ (Calendar.getInstance().getTimeInMillis() - ini.getTimeInMillis()));
		close();
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
	
	private int nextNewLine(char[] cbuf, int startPos, int length) {
		for (int i = startPos; i < length; i++)
			if (cbuf[i] == Const.CHAR_NEW_LINE) return i;
		return -1;
	}

	private void close() {
		try{
			Outbound.removeUserOutbound(outbound.getUserId());
			in.close();
			out.close();
			socketToClose.close();
		}catch (Exception e) {
			Log.error("", this, "run", "Error closing thread", e);
		}
	}
}

