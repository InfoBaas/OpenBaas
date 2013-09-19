package sessionsAndEmailConfirmations;

public interface EmailOperations {

	public boolean addUrlToUserId(String appId, String userId, String url);

	public boolean removeUrlToUserId(String appId, String userId);

	public boolean updateUrlToUserId(String appId, String userId, String url);

	public String getUrlUserId(String appId, String userId);

	public boolean sendRecoveryEmail(String appId, String userName,
			String userId, String email, String shortCode, String url);

	public boolean sendRegistrationEmailWithRegistrationCode(String appId, String userId,
			String userName, String email, String ref, String link);

	public boolean addRecoveryCodeToUser(String appId, String userId,
			String shortCode);
	public String getRecoveryCodeOfUser(String appId, String userId);
}
