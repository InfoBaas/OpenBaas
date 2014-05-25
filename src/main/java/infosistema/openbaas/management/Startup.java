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
package infosistema.openbaas.management;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.servlet.*;

import infosistema.openbaas.comunication.bound.InboundSocket;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

public class Startup implements ServletContextListener {
	public ServletContext context = null;
	private static String OPENBAASADMIN = "openbaasAdmin";
	private static String ADMINPASSWORD = "infosistema";
	private String AdminAppId = "~app";
	private static String AdminId = "~id";
	private static String AdminEmail = "admin@openbaas.infosistema.com";
	private static String AdminSessionId =Const.getADMIN_TOKEN();

	public void contextInitialized(ServletContextEvent event) {
		this.context = event.getServletContext();
		InboundSocket.createServerSockets();
		UsersMiddleLayer usersMid = UsersMiddleLayer.getInstance();
		SessionMiddleLayer sessionMid = SessionMiddleLayer.getInstance();
		PasswordEncryptionService service = new PasswordEncryptionService();
		byte[] hash = null;
		byte[] salt = null;
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(ADMINPASSWORD, salt);
		} catch (NoSuchAlgorithmException e) {
			Log.error("", this, "contextInitialized", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
		} catch (InvalidKeySpecException e) {
			Log.error("", this, "contextInitialized", "Invalid Key.", e); 
		}
		if (!usersMid.userEmailExists(AdminAppId, AdminEmail)) {
			usersMid.createUser(this.AdminAppId, AdminId,OPENBAASADMIN,"NOK", "NOK", AdminEmail, salt, hash, null, null, null, false, null, null, null);
		}
		if(sessionMid.createSession(AdminSessionId, AdminAppId,AdminId, ADMINPASSWORD)){
			FeedBackSchedule.startManager();
			NotificationsThread.startManager();
		}
		else{
			Log.warning("", this, "contextInitialized", "No admin Session created.");
		}
	}

	public void contextDestroyed(ServletContextEvent arg0) {
		this.context = null;
	}
}