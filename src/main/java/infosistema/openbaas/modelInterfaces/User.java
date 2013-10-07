package infosistema.openbaas.modelInterfaces;

/**
 * User Model object
 * 
 * Various atributes of User and its behavior.	
 * 
 * 
 * @author Miguel Aniceto
 * @version 0.0 
 */
public interface User {

	//******************************GETS*****************************
	/**
	 * Get User identifier.
	 * @return
	 */
	public String getUserId();
	
	public String getEmail();
	/**
	 * Returns the user ID using its email as a key.
	 * @param email User email.
	 * @return String User ID.
	 */
	public String getUserName();
	/**
	 * Returns the user fields.
	 * 
	 * @param id UserID.
	 * @return JSONObject containing user information if the user exists
	 */
//	public User getUserByID(String userId);
	
	/**
	 * Returns the Date when the User was created.
	 * @return Date
	 */
	public String getDateOfCreation();
	
	/**
	 * Returns the last Date when the User was updated.
	 * @return Date
	 */
	public String getDateOfUpdate();
	

	//******************************ACTIONS*****************************
	
	/**
	 * Deletes the user with the given id.
	 * @param key User login or email.
	 */
//	public void deleteUserByID(String id);
	
	/**
	 * Updates the ID.
	 * @param id User identifier.
	 * @return true if User exists and could update the field.
	 */
//	public void setUserID(String userId);
	
	/**
	 * Updates the User email.
	 * @param email User email.
	 * @return true if User exists and could update the field.
	 */
	public void setUserEmail(String email);
	
	/**
	 * Updates the User password.
	 * @param email User password.
	 * @return true if User exists and could update the field.
	 */
	public void setUserPassword(String password);
	/**
	 * Set user Identifier and email.
	 * @param id
	 * @param email
	 */
	public void setUserIDAndEmail(String id, String email);
	/**
	 * Set all user fields.
	 * @param id
	 * @param email
	 * @param password
	 */
	public void setAllFields(String id, String email, String  password);
	/**
	 * Get user Hash.
	 * @return
	 */
	public byte[] getHash();
	
	/**
	 * set user creation Date.
	 * @param creationDate
	 */
	public void setCreationDate(String creationDate);
	/**
	 * set user update Date.
	 * @param updateDate
	 */
	public void setUpdatedDate(String updateDate);
	public void setAlive(String alive);
	public String getAlive();

	public void setUserID(String userId);

	public void setReturnToken(String sessionToken);

	public void setUserID2(String userId);
	}

	
	
	