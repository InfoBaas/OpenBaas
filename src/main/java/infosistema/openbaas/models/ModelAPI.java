package infosistema.openbaas.models;

import java.util.Map;

public interface ModelAPI {

	//DataModel methods
	public boolean createApp(String id, String creationDate);
	public boolean deleteApp(String id);
	public boolean updateApp(String currentId, String newId);
	public Map<String, String> getApplication(String id);
	
	//FileSystem Methods
	//AWS in on proggress, to test first.
	
}
