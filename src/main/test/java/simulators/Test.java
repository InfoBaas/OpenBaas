package simulators;

import infosistema.openbaas.utils.Log;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.UUID;

import modelInterfaces.Image;

import com.sun.jersey.core.header.FormDataContentDisposition;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.JPG;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    Socket echoSocket = null;
	    try {
	      echoSocket = new Socket("localhost", 4005);
	      PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
	      String s ="/// DEVES COLOCAR AQUI A IMAGEM SERIALIZADA COM Base64";
	      byte[] c = s.getBytes();
	      echoSocket.getOutputStream().write(c);
	      out.flush();
	      s ="\n";
	      c = s.getBytes();
	      echoSocket.getOutputStream().write(c);
	      out.flush();
	      System.out.println("OK");
	    } catch (UnknownHostException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    } finally {
	      try {
	        echoSocket.close();
	      } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	      }
	    }
	}

}
