package infosistema.openbaas.data.models;


import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.encryption.PasswordEncryptionService;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;


/**
 * Contains all the user information and possible actions.
 * 
 * @author Miguel Aniceto
 * 
 */
@XmlRootElement
public class User {
	
	public static final String USER_NAME = "userName";
	public static final String EMAIL = "email";
	public static final String USER_FILE = "userFile";
	public static final String SALT = "salt";
	public static final String HASH = "hash";
	public static final String FLAG = "flag";
	public static final String ALIVE = "alive";
	public static final String EMAIL_CONFIRMED = "emailConfirmed";
	public static final String BASE_LOCATION_OPTION = "baseLocationOption";
	public static final String BASE_LOCATION = "baseLocation";
	public static final String LOCATION = "location";
	private static final String SN_PREFIXO = "SN_";
	private static final String SN_SUFIXO = "_ID";
	private static final String SOCIAL_NETWORK_ID_FORMAT = SN_PREFIXO +"%s" + SN_SUFIXO;
	
	private String userId;
	private String email;
	private String emailConfirmed;
	private String userName;
	private String userFile;
	@JsonIgnore
	private byte[] salt;
	@JsonIgnore
	private byte[] hash;
	private String alive;
	private String returnToken;
	private String baseLocationOption;
	private String baseLocation;
	private String lastLocation;

	public User(String userId){
		this.userId = userId;
	}
	public User() {
		this.setAlive("true");
	}

	public boolean equals(String id) {
		return this.userId.equalsIgnoreCase(id);
	}

	public void setUserID(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return this.userId;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getEmail() {
		return this.email;
	}

	public void setEmailConfirmed(String emailConfirmed) {
		this.emailConfirmed = emailConfirmed;
	}
	
	public String getEmailConfirmed() {
		return this.emailConfirmed;
	}

	public void setUserPassword_(String password) {
		try {
			PasswordEncryptionService service = new PasswordEncryptionService();
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(password, salt);
		} catch (NoSuchAlgorithmException e) {
			Log.error("", this, "setUserPassword", "Hashing Algorithm failed, please review the PasswordEncryptionService.", e); 
		} catch (InvalidKeySpecException e) {
			Log.error("", this, "setUserPassword", "Invalid Key.", e); 
		}

	}

	@JsonIgnore
	public byte[] getHash_() {
		return this.hash;
	}
	
	public String getUserName() {
		return this.userName;
	}	

	public void setUserName(String userName){
		this.userName = userName;
	}
	
	public String getUserFile() {
		return this.userFile;
	}	

	public void setUserFile(String userFile){
		this.userFile = userFile;
	}
	
	public String getAlive() {
		return alive;
	}
	
	public void setAlive(String alive) {
		this.alive = alive;
	}
	
	public String getReturnToken() {
		return returnToken;
	}
	
	public void setReturnToken(String returnToken) {
		this.returnToken = returnToken;
	}

	public String getBaseLocationOption() {
		return baseLocationOption;
	}

	public void setBaseLocationOption(String baseLocationOption) {
		this.baseLocationOption = baseLocationOption;
	}
	
	public String getLastLocation() {
		return lastLocation;
	}
	
	public void setLastLocation(String lastLocation) {
		this.lastLocation = lastLocation;
	}
	
	public String getBaseLocation() {
		return baseLocation;
	}
	
	public void setBaseLocation(String baseLocation) {
		this.baseLocation = baseLocation;
	}

	public static String SOCIAL_NETWORK_ID(String socialNetwork) {
		return String.format(SOCIAL_NETWORK_ID_FORMAT, socialNetwork); 
	}
	
	public static boolean isIndexedField(String key) {
		return key!=null && (key.equals(EMAIL) || key.equals(USER_NAME) || (key.startsWith(SN_PREFIXO) && key.endsWith(SN_SUFIXO))); 
	}

}
