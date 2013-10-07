package Document;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface DocumentInterface {
	
	//Application Documents
	/**
	 * Creates the application data in the /data (root).
	 * @param appId
	 * @param data
	 * @param location 
	 * @return
	 * @throws JSONException
	 */
	public boolean insertDocumentRoot(String appId, JSONObject data, String location) throws JSONException;
	/**
	 * Creates the Application Document tree.
	 * @param appId
	 * @return
	 */
	public boolean createDocumentForApplication(String appId);
	/**
	 * Inserts into the document data.
	 * @param appId
	 * @param url
	 * @param data
	 * @param location 
	 * @return
	 * @throws JSONException
	 */
	public boolean insertIntoDocument(String appId, String url, JSONObject data, String location) throws JSONException;
	/**
	 * Retrieves the data associated with the url.
	 * @param url
	 * @return
	 */
	public String getDataInDocument(String url);
	/**
	 * Deletes the data associated with the url (and the childs of this element).
	 * @param url
	 * @return
	 */
	public boolean deleteDataInDocument(String url);
	/**
	 * Verifies if a document exists for the application.
	 * @param appId
	 * @return
	 */
	public boolean docExistsForApp(String appId);
	/**
	 * Verifies if the url element exists in the Document.
	 * @param url
	 * @return
	 */
	public boolean elementExistsInDocument(String url);
	/**
	 * Verifies if data exists for the element.
	 * @param path
	 * @return
	 */
	public boolean dataExistsForElement(String path);
	/**
	 * Updates the data for the element.
	 * @param url
	 * @param data
	 * @return
	 */
	public boolean updateDataInDocument(String url, String data);
	/**
	 * Partially updates the element.
	 * @param url
	 * @param inputJson
	 * @param location 
	 * @return
	 * @throws JSONException
	 */
	public String patchDataInElement(String url, JSONObject inputJson, String location) throws JSONException;
	/**
	 * Gets all the documents starting from the root.
	 * @param appId
	 * @return
	 */
	public String getAllDocInApp(String appId);
	/**
	 * Creates a non publishable Document (special character ~).
	 * @param appId
	 * @param data
	 * @param url 
	 * @param location 
	 * @return
	 */
	public boolean createNonPublishableDocument(String appId, JSONObject data, String url, String location);
	//User Documents
	/**
	 * Creates a user Document in its url.
	 * @param appId
	 * @param data
	 * @param url
	 * @param location 
	 * @return
	 * @throws JSONException 
	 */
	public boolean insertIntoUserDocument(String appId, String userId, JSONObject data,
			String url, String location) throws JSONException;
	/**
	 * Retrieves the element in the given url of the user.
	 * @param appId
	 * @param userId
	 * @param url
	 * @return
	 */
	public String getElementInUserDocument(String appId, String userId,
			String url);
	/**
	 * Creates a document in the user data root.
	 * @param appId
	 * @param userId
	 * @param data
	 * @param location 
	 * @return
	 * @throws JSONException 
	 */
	public boolean insertUserDocumentRoot(String appId, String userId,
			JSONObject data, String location) throws JSONException;
	/**
	 * Creates a non publishable user Document.
	 * @param appId
	 * @param userId
	 * @param data
	 * @param location 
	 * @return
	 */
	public boolean createNonPublishableUserDocument(String appId,
			String userId, JSONObject data, String url, String location);
	public ArrayList<String> getAllDocsInRadius(String appId, double latitude, double longitude, double radius);
	public ArrayList<String> getDataInDocumentInRadius(String appId, String url,
			double latitude, double longitude, double radius);
	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId,
			double latitude, double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType);
	public String getAllUserDocs(String appId, String userId);
	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude,double longitude, double radius);
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType);
}