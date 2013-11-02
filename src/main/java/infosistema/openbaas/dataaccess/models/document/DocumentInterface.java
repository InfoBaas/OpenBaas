package infosistema.openbaas.dataaccess.models.document;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public interface DocumentInterface {

	
	// *** CREATE *** //
	public Boolean insertDocumentInPath(String appId, String inPath,String userId, JSONObject data) throws JSONException;
	
	// *** UPDATE *** //
	public Boolean updateDocumentInPath(String appId, String inPath,String userId, JSONObject data) throws JSONException;
	
	
	// *** Delete *** //
	public Boolean deleteDocumentInPath(String appId, String path) throws JSONException;
	
	// *** GET LIST *** //
	/*public String getAllDocInApp(String appId);
	
	public ArrayList<String> getAllDocsInRadius(String appId, double latitude, double longitude, double radius);

	public ArrayList<String> getAllUserDocsInRadius(String appId, String userId, double latitude, double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType);
	
	public String getAllUserDocs(String appId, String userId);

	// *** GET *** //
	*/
	public String getDocumentInPath(String appId, String userId, String path);

	// *** EXISTS *** //
	
	/**
	 * Verifies if the path element exists in the Document.
	 * @param path
	 * @return
	 */
	public boolean existsDocumentInPath(String appId, String path);

	
	// *** OTHERS *** //
	
	// ************* Retirar daqui ************* //
	
	public ArrayList<String> getAllAudioIdsInRadius(String appId, double latitude,double longitude, double radius);
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType);
}