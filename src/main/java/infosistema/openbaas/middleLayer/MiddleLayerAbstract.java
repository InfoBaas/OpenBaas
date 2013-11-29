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

import org.codehaus.jettison.json.JSONObject;

import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.dataaccess.files.AwsModel;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.files.FileSystemModel;
import infosistema.openbaas.dataaccess.files.FtpModel;
import infosistema.openbaas.dataaccess.geolocation.Geolocation;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.dataaccess.models.MetadataModel;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.dataaccess.models.UserModel;
import infosistema.openbaas.utils.Utils;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AppModel appModel;
	protected UserModel userModel;
	protected DocumentModel docModel;
	protected MetadataModel metadataModel;
	protected SessionModel sessionsModel;
	protected MediaModel mediaModel;
	protected Geolocation geo;

	
	// *** INIT *** //
	
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
		appModel = new AppModel();;
		userModel = new UserModel();
		metadataModel = new MetadataModel(); 
		sessionsModel = new SessionModel();
		mediaModel = new MediaModel();
		geo = Geolocation.getInstance();
	}

	// *** FILESYSTEM *** //
	
	protected FileInterface getAppFileInterface(String appId) {
		FileMode appFileMode = appModel.getApplicationFileMode(appId);
		if (appFileMode == FileMode.aws) return AwsModel.getInstance();
		else if (appFileMode == FileMode.ftp) return FtpModel.getInstance();
		else return FileSystemModel.getInstance();
	}
	

	// *** GET LIST *** //

	public ListResult find(QueryParameters qp) throws Exception {
		//TODO: colocar a query em sessão???
		List<String> list1 = getAllSearchResults(qp.getAppId(), qp.getQuery(), qp.getOrderType(), qp.getType());
		List<String> list2 = new ArrayList<String>();
		if (qp.getLatitude() != null && qp.getLongitude() != null && qp.getRadius()!= null)
			geo.getObjectsInDistance(qp.getLatitude(), qp.getLongitude(), qp.getRadius(), qp.getAppId(), qp.getType());
		List<String> list = and(list1, list2);
		return paginate(qp.getAppId(), list, qp.getOrderBy(), qp.getOrderType(), qp.getPageNumber(),
				qp.getPageSize(), qp.getType());
	}

	private List<String> getAllSearchResults(String appId, JSONObject query, String orderType, ModelEnum type) throws Exception {
		String oper = query.getString(QueryParameters.OPER);
		if (oper == null)
			throw new Exception("Error in query."); 
		else if (oper.equals(QueryParameters.OPER_AND)) {
			List<String> listOper1 = getAllSearchResults(appId, (JSONObject)(query.get(QueryParameters.OP1)), orderType, type);
			List<String> listOper2 = getAllSearchResults(appId, (JSONObject)(query.get(QueryParameters.OP2)), orderType, type);
			return and(listOper1, listOper2);
		} else if (oper.equals(QueryParameters.OPER_OR)) {
			List<String> listOper1 = getAllSearchResults(appId, (JSONObject)(query.get(QueryParameters.OP1)), orderType, type);
			List<String> listOper2 = getAllSearchResults(appId, (JSONObject)(query.get(QueryParameters.OP2)), orderType, type);
			return or(listOper1, listOper2);
		} else if (oper.equals(QueryParameters.OPER_NOT)) {
			return not(appId, query, orderType, type);
		} else {
			String path = null;
			try { path = query.getString(QueryParameters.ATTR_PATH); } catch (Exception e) {}
			String attribute = null; 
			try { attribute = query.getString(QueryParameters.ATTR_ATTRIBUTE); } catch (Exception e) {}
			String value = null; 
			try { value = query.getString(QueryParameters.ATTR_VALUE); } catch (Exception e) {}
			if (oper.equals(QueryParameters.OPER_CONTAINS)) {
				return contains(appId, path, attribute, value);
			} else if (oper.equals(QueryParameters.OPER_EQUALS)) {
				return equals(appId, path, attribute, value);
			} else if (oper.equals(QueryParameters.OPER_GREATER)) {
				return greater(appId, path, attribute, value);
			} else if (oper.equals(QueryParameters.OPER_LESSER)) {
				return lesser(appId, path, attribute, value);
			} else {
				throw new Exception("Error in query.");
				
			}
		}
	}

	protected List<String> and(List<String> list1, List<String> list2) {
		List<String> lOrig = list1.size() > list2.size() ? list2 : list1; 
		List<String> lComp = list1.size() > list2.size() ? list1 : list2; 
		List<String> lDest = new ArrayList<String>(); 
		for (String id: lOrig) {
			if (lComp.contains(id))
				lDest.add(id);
		}
		return lDest;
	}
	
	protected List<String> or(List<String> list1, List<String> list2) {
		List<String> lOrig = list1.size() > list2.size() ? list2 : list1; 
		List<String> lDest = list1.size() > list2.size() ? list1 : list2;
		for (String id: lOrig) {
			if (!lDest.contains(id))
				lDest.add(id);
		}
		return lDest;
	}
	
	protected List<String> not(String appId, JSONObject query, String orderType, ModelEnum type) throws Exception {
		String oper = query.getString(QueryParameters.OPER);
		JSONObject newQuery = new JSONObject(); 
		if (oper == null)
			throw new Exception("Error in query."); 
		else if (oper.equals(QueryParameters.OPER_AND)) {
			newQuery.append(QueryParameters.OPER, QueryParameters.OPER_NOT);
			newQuery.append(QueryParameters.OP1, query.get(QueryParameters.OP1));
			List<String> listOper1 = getAllSearchResults(appId, newQuery, orderType, type);
			newQuery.put(QueryParameters.OP1, query.get(QueryParameters.OP2));
			List<String> listOper2 = getAllSearchResults(appId, newQuery, orderType, type);
			return or(listOper1, listOper2);
		} else if (oper.equals(QueryParameters.OPER_OR)) {
			newQuery.append(QueryParameters.OPER, QueryParameters.OPER_NOT);
			newQuery.append(QueryParameters.OP1, query.get(QueryParameters.OP1));
			List<String> listOper1 = getAllSearchResults(appId, newQuery, orderType, type);
			newQuery.put(QueryParameters.OP1, query.get(QueryParameters.OP2));
			List<String> listOper2 = getAllSearchResults(appId, newQuery, orderType, type);
			return and(listOper1, listOper2);
		} else if (oper.equals(QueryParameters.OPER_NOT)) {
			return getAllSearchResults(appId, (JSONObject)(query.get(QueryParameters.OP1)), orderType, type);
		} else {
			String path = null;
			try { path = query.getString(QueryParameters.ATTR_PATH); } catch (Exception e) {}
			String attribute = null; 
			try { attribute = query.getString(QueryParameters.ATTR_ATTRIBUTE); } catch (Exception e) {}
			String value = null; 
			try { value = query.getString(QueryParameters.ATTR_VALUE); } catch (Exception e) {}
			if (oper.equals(QueryParameters.OPER_CONTAINS)) {
				return notContains(appId, path, attribute, value);
			} else if (oper.equals(QueryParameters.OPER_EQUALS)) {
				return diferent(appId, path, attribute, value);
			} else if (oper.equals(QueryParameters.OPER_GREATER)) {
				return lesserOrEqual(appId, path, attribute, value);
			} else if (oper.equals(QueryParameters.OPER_LESSER)) {
				return greaterOrEqual(appId, path, attribute, value);
			} else {
				throw new Exception("Error in query.");
				
			}
		}
	}
	
	protected abstract List<String> contains(String appId, String path, String attribute, String value);
	
	protected abstract List<String> notContains(String appId, String path, String attribute, String value);
	
	protected abstract List<String> equals(String appId, String path, String attribute, String value);
	
	protected abstract List<String> diferent(String appId, String path, String attribute, String value);
	
	protected abstract List<String> greater(String appId, String path, String attribute, String value);
	
	protected abstract List<String> greaterOrEqual(String appId, String path, String attribute, String value);
	
	protected abstract List<String> lesser(String appId, String path, String attribute, String value);
	
	protected abstract List<String> lesserOrEqual(String appId, String path, String attribute, String value);

	private ListResult paginate(String appId, List<String> lst, String orderBy, String orderType, 
			Integer pageNumber, Integer pageSize, ModelEnum type) {
		
		ArrayList<String> listIdsSorted = new ArrayList<String>();
		List<String> listRes = new ArrayList<String>();
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

		
		Map<String, String> hashSorted = sortByValues(hash);
		
		Iterator entries = hashSorted.entrySet().iterator();
		while (entries.hasNext()) {
		  Entry<String,String> thisEntry = (Entry<String,String>) entries.next();
		  String key = thisEntry.getKey();
		  listIdsSorted.add(key);
		}
		if(orderType.equals("desc")){
			Collections.reverse(listIdsSorted);
		}

		Integer iniIndex = (pageNumber-1)*pageSize;
		Integer finIndex = (((pageNumber-1)*pageSize)+pageSize);
		
		if(finIndex>listIdsSorted.size())
			listRes  = listIdsSorted.subList(iniIndex, listIdsSorted.size());
		else
			listRes = listIdsSorted.subList(iniIndex, finIndex);
		Integer totalElems = (int) Utils.roundUp(listIdsSorted.size(),pageSize);
		ListResult listResultRes = new ListResult(listRes, pageNumber, pageSize, totalElems);
		return listResultRes;
	}
	
	private static <K extends Comparable,V extends Comparable> Map<K,V> sortByValues(Map<K,V> map){
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
	
}
