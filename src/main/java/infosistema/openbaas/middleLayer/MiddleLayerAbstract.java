package infosistema.openbaas.middleLayer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.dataaccess.files.AwsModel;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.files.FileSystemModel;
import infosistema.openbaas.dataaccess.files.FtpModel;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.dataaccess.models.MetadataModel;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.dataaccess.models.UserModel;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AppModel appModel;
	protected UserModel userModel;
	protected DocumentModel docModel;
	protected MetadataModel metadataModel;
	protected SessionModel sessionsModel;
	protected MediaModel mediaModel;

	
	// *** INIT *** //
	
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
		appModel = new AppModel();;
		userModel = new UserModel();
		metadataModel = new MetadataModel(); 
		sessionsModel = new SessionModel();
		mediaModel = new MediaModel();
	}

	// *** FILESYSTEM *** //
	
	protected FileInterface getAppFileInterface(String appId) {
		FileMode appFileMode = appModel.getApplicationFileMode(appId);
		if (appFileMode == FileMode.aws) return AwsModel.getInstance();
		else if (appFileMode == FileMode.ftp) return FtpModel.getInstance();
		else return FileSystemModel.getInstance();
	}
	
	// *** METADATA *** //
	
	public Metadata getMetadata(String key) {
		metadataModel = new MetadataModel(); 
		Metadata retObj = new Metadata();
		Map<String, String> fields = metadataModel.getMetadata(key);
		DateFormat df = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy");
		try { 
			retObj.setCreateDate(df.parse(fields.get(Metadata.CREATE_DATE)));
		} catch (Exception e) {}
		retObj.setCreateUser(fields.get(Metadata.CREATE_USER));
		try {
			retObj.setLastUpdateDate(df.parse(fields.get(Metadata.LAST_UPDATE_DATE)));
		} catch (Exception e) {}
		retObj.setLastUpdateUser(fields.get(Metadata.LAST_UPDATE_USER));
		retObj.setLocation(fields.get(Metadata.LOCATION));
		return retObj;
	}
	
	public Metadata createMetadata(String key, String userId, String location) {
		metadataModel = new MetadataModel(); 
		Map<String, String> fields = new HashMap<String, String>();
		fields.put(Metadata.CREATE_DATE, (new Date()).toString());
		fields.put(Metadata.CREATE_USER, userId);
		fields.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		fields.put(Metadata.LAST_UPDATE_USER, userId);
		fields.put(Metadata.LOCATION, location);
		if (metadataModel.createUpdateMetadata(key, fields))
			return getMetadata(key);
		else
			return null;
	}
	
	public Metadata updateMetadata(String key, String userId, String location) {
		metadataModel = new MetadataModel(); 
		Map<String, String> fields = new HashMap<String, String>();
		fields.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		fields.put(Metadata.LAST_UPDATE_USER, userId);
		if (location != null && !"".equals(location))
			fields.put(Metadata.LOCATION, location);
		if (metadataModel.createUpdateMetadata(key, fields))
			return getMetadata(key);
		else
			return null;
	}
	
	public Boolean deleteMetadata(String key) {
		metadataModel = new MetadataModel(); 
		return metadataModel.deleteMetadata(key, true);
	}
	
	public ArrayList<String> paginate(String appId, List<String> lst, String orderBy, String orderType, 
			Integer pageNumber, Integer pageSize, ModelEnum type) {
		
		ArrayList<String> res = new ArrayList<String>();

		Map<String, String> hash = new HashMap<String, String>();
		Iterator<String> it = lst.iterator();
		while(it.hasNext()){
			Object value = null;
			String key = it.next();
			if(type.compareTo(ModelEnum.audio)==0 ||type.compareTo(ModelEnum.video)==0 ||
			   type.compareTo(ModelEnum.storage)==0 ||type.compareTo(ModelEnum.image)==0){
				Map<String, String> temp = mediaModel.getMedia(appId, type, key);
				value = temp.get(orderBy);
			}
			if(type.compareTo(ModelEnum.users)==0){
				Map<String, String> temp = userModel.getUser(appId, key);
				value = temp.get(orderBy);
			}
			if(type.compareTo(ModelEnum.data)==0){
				//TODO Nota JM: Nao esta implementado nem me parece possivel.
			}
			hash.put(key, value.toString());
		}

		
		Map<String, String> sorted = sortByValues(hash);
		
		Iterator entries = sorted.entrySet().iterator();
		while (entries.hasNext()) {
		  Entry<String,String> thisEntry = (Entry<String,String>) entries.next();
		  String key = thisEntry.getKey();
		  res.add(key);
		}
		if(orderType.equals("desc")){
			Collections.reverse(res);
		}

		return res;
	}
	
	
	public static <K extends Comparable,V extends Comparable> Map<K,V> sortByValues(Map<K,V> map){
        List<Map.Entry<K,V>> entries = new LinkedList<Map.Entry<K,V>>(map.entrySet());
     
        Collections.sort(entries, new Comparator<Map.Entry<K,V>>() {

            @Override
            public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
     
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<K,V> sortedMap = new LinkedHashMap<K,V>();
     
        for(Map.Entry<K,V> entry: entries){
            sortedMap.put(entry.getKey(), entry.getValue());
        }
     
        return sortedMap;
    }
}
