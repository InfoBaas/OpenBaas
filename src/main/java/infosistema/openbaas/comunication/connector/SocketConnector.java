package infosistema.openbaas.comunication.connector;

import infosistema.openbaas.comunication.bound.Outbound;
import infosistema.openbaas.comunication.message.Message;
import infosistema.openbaas.data.models.User;
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
import java.util.Calendar;
import java.util.Date;

public class SocketConnector implements Runnable, IConnector {

	private Outbound outbound;
	BufferedReader in = null;
	PrintWriter out = null;
	Socket socketToClose = null;
	Date startDate1 ;
	Date startDate2 ;

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
				while ((newLinePos = nextNewLine(cbuf, startPos, readLength)) > 0)  {
					startDate1 = Utils.getDate();
					Log.error("", this, "######startDate1", "########startDate1 inicio: " + startDate1);
					if (newLinePos - startPos <= 1) continue;
					message.append(cbuf, startPos, (newLinePos-startPos));
					//Utils.printMemoryStats();
					startDate2 = Utils.getDate();
					Log.error("", this, "######startDate2", "########startDate2 inicio: " + startDate2);
					Log.error("", this, "######1", "########msg1: " + message.toString());
					outbound.processMessage(message.toString());
					startPos = newLinePos +1;
					message.setLength(0);
				}
				message.append(cbuf, startPos, (readLength-startPos)); 
			}catch (Exception e) {
				message.setLength(0);
				Log.error("", this, "run", "Error running thread", e);
			}
		}
		Log.error("", this, "", "###EXIT - logout do user:" + outbound.getUserId());
		close();
	}

	public boolean sendMessage(Message message) {
		try {
			Log.error("", this, "######2", "########msg2: " + message.toString());
			out.println(CharBuffer.wrap(message.toString()));
		} catch (Exception e) {
			Log.error("", this, "sendMessage", "Error sending Message", e);
			return false;
		}
		Date endDate = Utils.getDate();
		Log.error("", "", "Time1", "Time Start1: " + Utils.printDate(startDate1) + " - Time Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate1.getTime()));
		Log.error("", "", "Time2", "Time Start2: " + Utils.printDate(startDate2) + " - Time Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate2.getTime()));
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

