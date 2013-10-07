package rest_Models;


import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;

import modelInterfaces.User;

/**
 * Contains all the user information and possible actions.
 * 
 * @author Miguel Aniceto
 * 
 */
@XmlRootElement
public class DefaultUser implements User {
	private String userId;
	private String email;
	private String userName;
	@JsonIgnore
	private byte[] salt;
	@JsonIgnore
	private byte[] hash;
	private String creationDate;
	private String updatedDate;
	private String alive;
	private String returnToken;
	/**
	 * Provides the user creation mechanism.
	 * 
	 * The password's hash is computed using PBKDF2, salt and hash are stored.
	 * 
	 * @param name
	 *            User name;
	 * @param email
	 *            User email;
	 * @param password
	 *            User password in Hash;
	 * @return true if the user was created sucessfully.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 */
	public DefaultUser(String userId, String email) {
		this.userId = userId;
		this.email = email;
		this.creationDate = new Date().toString();
		this.setAlive("true");
	}
	public DefaultUser(String userId){
		this.userId = userId;
	}
	public DefaultUser() {
		this.creationDate = new Date().toString();
		this.setAlive("true");
	}

	public DefaultUser(String id, String email, byte[] hash,  byte[] salt) {
			this.userId = id;
			this.email = email;
		this.creationDate = new Date().toString();
		this.setAlive("true");
	}

	public String getIDByEmail(String email) {
		return this.userId;
	}

	public void setUserEmail(String email) {
		this.email = email;
	}

	public boolean equals(String id) {
		if (this.userId.equalsIgnoreCase(id))
			return true;
		else
			return false;
	}

	public void setUserID(String id) {
		this.updatedDate = new Date().toString();

	}
	public void setUserID2(String id) {
		this.userId = id;

	}

	public void setUserIDAndEmail(String id, String email) {
		this.userId = id;
		this.email = email;
		this.updatedDate = new Date().toString();
		this.updatedDate = new Date().toString();
	}

	public void updateAllFields(String id, String email, String password) {
		try {
			this.updatedDate = new Date().toString();
			PasswordEncryptionService service = new PasswordEncryptionService();
			salt = service.generateSalt();
			this.email = email;
			this.userId = id;

			this.hash = service.getEncryptedPassword(password, salt);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setAllFields(String id, String email, String password) {
		this.updatedDate = new Date().toString();
		this.userId = id;
		this.email = email;
		this.setUserPassword(password);
	}

	public String getUserId() {
		return this.userId;
	}

	public String getEmail() {
		return this.email;
	}

	public String getDateOfCreation() {
		return this.creationDate;
	}

	public String getDateOfUpdate() {
		return this.updatedDate;
	}

	public void setUserPassword(String password) {
		try {
			this.updatedDate = new Date().toString();
			PasswordEncryptionService service = new PasswordEncryptionService();
			salt = service.generateSalt();
			hash = service.getEncryptedPassword(password, salt);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	@JsonIgnore
	public byte[] getHash() {
		return this.hash;
	}
	public void setInactive(){
		this.setAlive("false");
	}
	@Override
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	@Override
	public void setUpdatedDate(String updateDate) {
		this.updatedDate = updateDate;
	}
	@Override
	public String getUserName() {
		return this.userName;
	}	
	public void setUserName(String userName){
		this.userName = userName;
	}
	@Override
	public String getAlive() {
		return alive;
	}
	@Override
	public void setAlive(String alive) {
		this.alive = alive;
	}
	public String getReturnToken() {
		return returnToken;
	}
	public void setReturnToken(String returnToken) {
		this.returnToken = returnToken;
	}
}
