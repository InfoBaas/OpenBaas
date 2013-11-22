package infosistema.openbaas.management;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.servlet.*;

import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

/**
 * Administrator class
 * 
 * @author miguel
 * 
 */
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
		UsersMiddleLayer usersMid = MiddleLayerFactory.getUsersMiddleLayer();
		SessionMiddleLayer sessionMid = MiddleLayerFactory.getSessionMiddleLayer();
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
		if (!usersMid.userExistsInApp(AdminAppId, AdminId, AdminEmail)) {
			Log.debug("", this, "contextInitialized", "*****************Creating user***************");
			Log.debug("", this, "contextInitialized", "userId: " + AdminId + " email: " + AdminEmail);
			Log.debug("", this, "contextInitialized", "********************************************");
			usersMid.createUser(this.AdminAppId, AdminId,OPENBAASADMIN,"NOK", "NOK", AdminEmail, salt, hash, null, null, null);
			// Output a simple message to the server's console
			System.out
					.println("***********************************************");
			System.out
					.println("*********************WELCOME!******************");
			System.out
					.println("****************OpenBaas is Deployed***********");
			System.out
					.println("*******************Login to start**************");
			System.out
					.println("***********************************************");
		}
		if(sessionMid.createSession(AdminSessionId, AdminAppId,AdminId, ADMINPASSWORD))
			Log.debug("", this, "contextInitialized", "Admin Session created. Id: ");
		else{
			Log.warning("", this, "contextInitialized", "No admin Session created.");
		}
	}

	public void contextDestroyed(ServletContextEvent arg0) {
		this.context = null;
	}// end constextDestroyed method

}