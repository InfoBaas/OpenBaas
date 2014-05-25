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
