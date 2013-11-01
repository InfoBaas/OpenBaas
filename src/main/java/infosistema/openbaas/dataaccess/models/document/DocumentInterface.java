package infosistema.openbaas.dataaccess.models.document;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface DocumentInterface {

	// *** CREATE *** //
	/**
	 * Inserts a Document in the path.
	 * @param appId
	 * @param data
	 * @param location 
	 * @return
	 * @throws JSONException
	 */
	public boolean insertDocumentInPath(String appId, String userId, String inPath, JSONObject data, String location) throws JSONException;
	
	//XPTO: PARA QUE SERVE ISTO????
	/**
	 * Creates a non publishable Document (special character ~).
	 * @param appId
	 * @param data
	 * @param path 
	 * @param location 
	 * @return
	 */
	//public boolean createNonPublishableDocument(String appId, JSONObject data, String path, String location);
	
	//XPTO: PARA QUE SERVE ISTO????
	/**
	 * Creates a non publishable user Document.
	 * @param appId
	 * @param userId
	 * @param data
	 * @param location 
	 * @return
	 */
	//public boolean createNonPublishableUserDocument(String appId, String userId, JSONObject data, String path, String location);
	
	// *** UPDATE *** //

	public boolean updateDocumentInPath(String appId, String userId, String inPath, JSONObject data, String location) throws JSONException;
	
	// *** GET LIST *** //
	
	/**
	 * Gets all the documents starting from the root.
	 * @param appId
	 * @return
	 */
	public String getAllDocInApp(String appId);
	
	public ArrayList<String> getAllDocsInRadius(String appId, double latitude, double longitude, double radius);

	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId, double latitude, double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType);
	
	public String getAllUserDocs(String appId, String userId);

	// *** GET *** //
	
	public String getDocumentInPath(String appId, String userId, String path);

	// *** DELETE *** //
	
	/**
	 * Deletes the data associated with the path (and the childs of this element).
	 * @param path
	 * @return
	 */
	public boolean deleteDocumentInPath(String appId, String userId, String path);

	// *** EXISTS *** //
	
	/**
	 * Verifies if the path element exists in the Document.
	 * @param path
	 * @return
	 */
	public boolean existsDocumentInPath(String appId, String userId, String path);

	// *** OTHERS *** //
	
	// ************* Retirar daqui ************* //
	
	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude,double longitude, double radius);
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType);
}