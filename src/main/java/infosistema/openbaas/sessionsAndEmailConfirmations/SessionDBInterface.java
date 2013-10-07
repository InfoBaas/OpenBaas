package infosistema.openbaas.sessionsAndEmailConfirmations;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Set;

public interface SessionDBInterface {
	// Session operations
	/**
	 * Creates a user session for userId.
	 * @param sessionId
	 * @param appid
	 * @param userId
	 */
	public void createSession(String sessionToken, String appid, String userId);

	/**
	 * 
	 * Retrieves the session Fields of the specified sessionId.
	 * @param sessionId
	 * @return
	 */
	public Map<String, String> getSessionFields(String sessionToken);
	/**
	 * Verifies if a session with sessionId exists.
	 * @param sessionId
	 * @return
	 */
	public boolean sessionTokenExists(String sessionToken);
	/**
	 * Verifies if a given session exists for the specified user.
	 * @param sessionToken
	 * @param userId
	 * @return
	 */
	public boolean sessionTokenExistsForUser(String sessionToken, String userId);
	/**
	 * Deletes the administrator Session (logout).
	 * @param adminId
	 */
	public void deleteAdminSession(String adminId);
	/**
	 * Deletes the user session with the given token.
	 * @param sessionToken
	 * @param userId
	 * @return
	 */
	public boolean deleteUserSession(String sessionToken, String userId);
	/**
	 * Creates a new administrator session.
	 * @param sessionId
	 * @param adminId
	 */
	public void createAdminSession(String sessionToken, String adminId);
	/**
	 * Refreshes the session, setting its reset timer to default. Example: A session with 12 hours left to be killed
	 * is refreshed, after the refresh, it now has 24 hours until it is killed.
	 * @param sessionId
	 * @param date 
	 */
	public void refreshSession(String sessionToken, String date);
	/**
	 * Retrieves aone session identifier for the given user.
	 * @param userId
	 * @return
	 */
	public String getUserSession(String userId);
	/**
	 * Deletes all the sessions of the given user.
	 * @param userId
	 * @return
	 */
	public boolean deleteAllUserSessions(String userId);
	/**
	 * Retrieves all the sessions of the given user.
	 * @param userId
	 * @return
	 */
	public Set<String> getAllUserSessions(String userId);

	/**
	 * Verifies if at least one session exists for the user.
	 * @param userId
	 * @return
	 */
	public boolean sessionExistsForUser(String userId);

	/**
	 * Verifies if an admin entry exists in the Database.
	 * @param admin
	 * @return
	 */
	public boolean adminExists(String admin);
	/**
	 * Retrieves all the Admin Fields.
	 * @param OPENBAASADMIN
	 * @return
	 */
	public Map<String, String> getAdminFields(String OPENBAASADMIN);
	/**
	 * Creates the admin entry in the database,
	 * @param OPENBAASADMIN
	 * @param adminSalt
	 * @param adminHash
	 * @throws UnsupportedEncodingException
	 */
	public void createAdmin(String OPENBAASADMIN, byte[] adminSalt,
			byte[] adminHash) throws UnsupportedEncodingException;

	public void addLocationToSession(String location, String sessionToken, String userAgent);

	public boolean refreshSession(String sessionToken, String location,
			String date, String userAgent);
	public String getUserUsingSessionToken(String sessionToken);
}
