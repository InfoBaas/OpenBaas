package infosistema.openbaas.management;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.servlet.*;

import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
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
	private static String AdminSessionId ="~session";

	public void contextInitialized(ServletContextEvent event) {
		this.context = event.getServletContext();
		//AppsMiddleLayer appsMid = MiddleLayerFactory.getAppsMiddleLayer();
		UsersMiddleLayer usersMid = MiddleLayerFactory.getUsersMiddleLayer();
		SessionMiddleLayer sessionMid = MiddleLayerFactory.getSessionMiddleLayer();
		PasswordEncryptionService service = new PasswordEncryptionService();
		byte[] hash = null;
		byte[] salt = null;
		try {
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(ADMINPASSWORD, salt);
		} catch (NoSuchAlgorithmException e) {
			System.out
					.println("Hashing Algorithm failed, please review the PasswordEncryptionService.");
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			System.out.println("Invalid Key.");
			e.printStackTrace();
		}
		if (!usersMid.userExistsInApp(AdminAppId, AdminId, AdminEmail)) {
			System.out.println("*****************Creating user***************");
			System.out.println("userId: " + AdminId + " email: " + AdminEmail);
			System.out.println("********************************************");
			usersMid.createUser(this.AdminAppId, AdminId,OPENBAASADMIN, AdminEmail, salt, hash, null);
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
			System.out.println("Admin Session created. Id: ~session");
		else{
			System.out.println("No admin Session created.");
		}
	}

	public void contextDestroyed(ServletContextEvent arg0) {
		this.context = null;
	}// end constextDestroyed method

}