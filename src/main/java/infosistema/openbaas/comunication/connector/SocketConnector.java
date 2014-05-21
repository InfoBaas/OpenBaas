package infosistema.openbaas.comunication.connector;

import infosistema.openbaas.comunication.bound.Outbound;
import infosistema.openbaas.comunication.message.Message;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.CharBuffer;
import java.util.Arrays;

public class SocketConnector implements Runnable, IConnector {

	private Outbound outbound;
	BufferedReader in = null;
	PrintWriter out = null;
	Socket socketToClose = null;
	String strGuid = Utils.getRandomString(5);

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
				while ((newLinePos = nextNewLine(cbuf, startPos, readLength)) > 0)  
				{
					if (newLinePos - startPos <= 1) continue;	
					message.append(cbuf, startPos, (newLinePos-startPos));
					Utils.printMemoryStats();
					//Log.debug("",  "SocketConnector (" + strGuid + ")", "", "###Message received: " + message.toString());
					outbound.processMessage(message.toString());
					startPos = newLinePos +1;
					message.setLength(0);
				}
				message.append(cbuf, startPos, (readLength-startPos)); 
			}catch (Exception e) {
				message.setLength(0);
				Log.error("",  "SocketConnector (" + strGuid + ")", "", "Error running thread", e);
			}
		}
		//Log.debug("",  "SocketConnector (" + strGuid + ")", "", "###EXIT - logout do user:" + outbound.getUserId());
		close();
	}

	public boolean sendMessage(Message message) {
		try {
			out.println(CharBuffer.wrap(message.toString()));
			//Log.debug("",  "SocketConnector (" + strGuid + ")", "", "###Message sent to chat room: " + message.toString());
		} catch (Exception e) {
			Log.error("",  "SocketConnector (" + strGuid + ")", "sendMessage", "Error sending Message", e);
			return false;
		}
		return true;
	}
	
	private int nextNewLine(char[] cbuf, int startPos, int length) {
		for (int i = startPos; i < length; i++)
			if (cbuf[i] == Const.CHAR_NULL) return i;
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

