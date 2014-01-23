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

import javax.ws.rs.core.PathSegment;

import org.codehaus.jettison.json.JSONObject;

import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.enums.OperatorEnum;
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
	

	// *** PROTECTED *** //
	
	protected List<String> convertPath(List<PathSegment> path) {
		List<String> retObj = new ArrayList<String>();
		if (path != null) {
			for(PathSegment pathSegment: path) {
				retObj.add(pathSegment.getPath());
			}
		}
		return retObj;
	}
	
	
	// *** GET LIST *** //

	public ListResult find(QueryParameters qp) throws Exception {
		List<String> listRes = new ArrayList<String>();
		List<String> list1 = getAllSearchResults(qp.getAppId(), qp.getUrl(), qp.getQuery(), qp.getOrderType(), qp.getType());
		List<String> list2 = new ArrayList<String>();
		if (qp.getLatitude() != null && qp.getLongitude() != null && qp.getRadius()!= null){
			list2 = geo.getObjectsInDistance(qp.getLatitude(), qp.getLongitude(), qp.getRadius(), qp.getAppId(), qp.getType());
			listRes = and(list1, list2);
		}else {
			listRes = list1;
		}
		
		return paginate(qp.getAppId(), listRes, qp.getOrderBy(), qp.getOrderType(), qp.getPageNumber(),
				qp.getPageSize(), qp.getType());
	}

	protected List<String> getAllSearchResults(String appId, String url, JSONObject query, String orderType, ModelEnum type) throws Exception {
		if(query!=null){
			OperatorEnum oper = OperatorEnum.valueOf(query.getString(OperatorEnum.oper.toString())); 
			if (oper == null)
				throw new Exception("Error in query."); 
			else if (oper.equals(OperatorEnum.and)) {
				List<String> listOper1 = getAllSearchResults(appId, url, (JSONObject)(query.get(OperatorEnum.op1.toString())), orderType, type);
				List<String> listOper2 = getAllSearchResults(appId, url, (JSONObject)(query.get(OperatorEnum.op2.toString())), orderType, type);
				return and(listOper1, listOper2);
			} else if (oper.equals(OperatorEnum.or)) {
				List<String> listOper1 = getAllSearchResults(appId, url, (JSONObject)(query.get(OperatorEnum.op1.toString())), orderType, type);
				List<String> listOper2 = getAllSearchResults(appId, url, (JSONObject)(query.get(OperatorEnum.op2.toString())), orderType, type);
				return or(listOper1, listOper2);
			} else if (oper.equals(OperatorEnum.not)) {
				return not(appId, url, query, orderType, type);
			} else {
				String value = null; 
				try { value = query.getString(QueryParameters.ATTR_VALUE); } catch (Exception e) {}
				String attribute = null;
				try { attribute = query.getString(QueryParameters.ATTR_ATTRIBUTE); } catch (Exception e) {}
				String path = null;
				try { path = query.getString(QueryParameters.ATTR_PATH); } catch (Exception e) {}
				if (oper.equals(OperatorEnum.contains) || oper.equals(OperatorEnum.equals) ||
						oper.equals(OperatorEnum.greater) || oper.equals(OperatorEnum.lesser)) {
					return getOperation(oper, appId, url, path, attribute, value, type);
				} else {
					throw new Exception("Error in query.");
					
				}
			}
		}
		else{
			return getAll(appId,type);
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
	
	protected List<String> not(String appId, String url, JSONObject query, String orderType, ModelEnum type) throws Exception {
		OperatorEnum oper = OperatorEnum.valueOf(query.getString(OperatorEnum.oper.toString())); 
		JSONObject newQuery = new JSONObject(); 
		if (oper == null)
			throw new Exception("Error in query."); 
		else if (oper.equals(OperatorEnum.and)) {
			newQuery.append(OperatorEnum.oper.toString(), OperatorEnum.not);
			newQuery.append(OperatorEnum.op1.toString(), query.get(OperatorEnum.op1.toString()));
			List<String> listOper1 = getAllSearchResults(appId, url, newQuery, orderType, type);
			newQuery.put(OperatorEnum.op1.toString(), query.get(OperatorEnum.op2.toString()));
			List<String> listOper2 = getAllSearchResults(appId, url, newQuery, orderType, type);
			return or(listOper1, listOper2);
		} else if (oper.equals(OperatorEnum.or)) {
			newQuery.append(OperatorEnum.oper.toString(), OperatorEnum.not);
			newQuery.append(OperatorEnum.op1.toString(), query.get(OperatorEnum.op1.toString()));
			List<String> listOper1 = getAllSearchResults(appId, url, newQuery, orderType, type);
			newQuery.put(OperatorEnum.op1.toString(), query.get(OperatorEnum.op2.toString()));
			List<String> listOper2 = getAllSearchResults(appId, url, newQuery, orderType, type);
			return and(listOper1, listOper2);
		} else if (oper.equals(OperatorEnum.not)) {
			return getAllSearchResults(appId, url, (JSONObject)(query.get(OperatorEnum.op1.toString())), orderType, type);
		} else {
			String path = null;
			try { path = query.getString(QueryParameters.ATTR_PATH); } catch (Exception e) {}
			String attribute = null; 
			try { attribute = query.getString(QueryParameters.ATTR_ATTRIBUTE); } catch (Exception e) {}
			String value = null; 
			try { value = query.getString(QueryParameters.ATTR_VALUE); } catch (Exception e) {}
			if (oper.equals(OperatorEnum.contains)) {
				oper = OperatorEnum.notContains;
			} else if (oper.equals(OperatorEnum.equals)) {
				oper = OperatorEnum.diferent;
			} else if (oper.equals(OperatorEnum.greater)) {
				oper = OperatorEnum.lesserOrEqual;
			} else if (oper.equals(OperatorEnum.lesser)) {
				oper = OperatorEnum.greaterOrEqual;
			} else {
				throw new Exception("Error in query.");
			}
			return getOperation(oper, appId, url, path, attribute, value, type);
		}
	}
	
	protected List<String> getOperation(OperatorEnum oper, String appId, String url, String path, String attribute, String value, ModelEnum type) throws Exception {
		return new ArrayList<String>();
	}
	
	protected List<String> getAll(String appId, ModelEnum type) throws Exception {
		return new ArrayList<String>();
	}
	
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
				if(orderBy.equals("_id")&&type.compareTo(ModelEnum.audio)==0)
					orderBy="audioId";
				if(orderBy.equals("_id")&&type.compareTo(ModelEnum.video)==0)
					orderBy="videoId";
				if(orderBy.equals("_id")&&type.compareTo(ModelEnum.storage)==0)
					orderBy="storageId";
				if(orderBy.equals("_id")&&type.compareTo(ModelEnum.image)==0)
					orderBy="imageId";
				Map<String, String> temp = mediaModel.getMedia(appId, type, key);
				value = temp.get(orderBy);
			}
			if(type.compareTo(ModelEnum.users)==0){
				if(orderBy.equals("_id"))
					orderBy="userId";
				Map<String, String> temp = userModel.getUser(appId, key);
				value = temp.get(orderBy);
			}
			if(type.compareTo(ModelEnum.data)==0){
				//TODO Nota JM: Nao esta implementado nem me parece possivel.
			}
			if (value == null) value = "_id";
			hash.put(key, value.toString());
		}

		
		Map<String, String> hashSorted = sortByValues(hash);
		
		Iterator<Entry<String,String>> entries = hashSorted.entrySet().iterator();
		while (entries.hasNext()) {
		  Entry<String,String> thisEntry = entries.next();
		  String key = thisEntry.getKey();
		  listIdsSorted.add(key);
		}
		if(orderType.equals("desc")){
			Collections.reverse(listIdsSorted);
		}

		Integer iniIndex = (pageNumber-1)*pageSize;
		Integer finIndex = (((pageNumber-1)*pageSize)+pageSize);
		
		if(finIndex>listIdsSorted.size())
			try{listRes  = listIdsSorted.subList(iniIndex, listIdsSorted.size());}catch(Exception e){}
		else{
			try{listRes = listIdsSorted.subList(iniIndex, finIndex);}catch(Exception e){}
		}
			
		Integer totalElems = (int) Utils.roundUp(listIdsSorted.size(),pageSize);
		ListResult listResultRes = new ListResult(listRes, pageNumber, pageSize, lst.size(),totalElems);
		return listResultRes;
	}
	
	private static Map<String, String> sortByValues(Map<String, String> map){
        List<Map.Entry<String, String>> entries = new LinkedList<Map.Entry<String, String>>(map.entrySet());
        
        Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Entry<String, String> o1, Entry<String, String> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
     
        //LinkedHashMap will keep the keys in the order they are inserted
        //which is currently sorted on natural ordering
        Map<String, String> sortedMap = new LinkedHashMap<String, String>();
     
        for(Map.Entry<String, String> entry: entries){
            sortedMap.put(entry.getKey(), entry.getValue());
        }
     
        return sortedMap;
    }


	// *** METADATA *** //

	public String getMetaKey(String appId, String userId, String id, ModelEnum type) {
		String appIdStr = "apps." + appId;
		String userIdStr = (userId == null ? "" : ".users." + userId);
		String typeStr = ((type == null || userId != null) ? "" : "." + type.toString());
		String idStr = id != null ? "." + id : "";
		return appIdStr + userIdStr + typeStr + idStr;
	}

	public Metadata getMetadata(String appId, String userId, String id, ModelEnum type) {
		String key = getMetaKey(appId, userId, id, type);
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
	
	public Metadata createMetadata(String appId, String userId, String id, String creatorId, ModelEnum type, String location) {
		String key = getMetaKey(appId, userId, id, type);
		metadataModel = new MetadataModel(); 
		Map<String, String> fields = new HashMap<String, String>();
		fields.put(Metadata.CREATE_DATE, (new Date()).toString());
		fields.put(Metadata.CREATE_USER, creatorId);
		fields.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		fields.put(Metadata.LAST_UPDATE_USER, creatorId);
		fields.put(Metadata.LOCATION, location);
		if (metadataModel.createUpdateMetadata(key, fields))
			return getMetadata(appId, userId, id, type);
		else
			return null;
	}
	
	public Metadata updateMetadata(String appId, String userId, String id, String creatorId, ModelEnum type, String location) {
		String key = getMetaKey(appId, userId, id, type);
		metadataModel = new MetadataModel(); 
		Map<String, String> fields = new HashMap<String, String>();
		fields.put(Metadata.LAST_UPDATE_DATE, (new Date()).toString());
		fields.put(Metadata.LAST_UPDATE_USER, creatorId);
		if (location != null && !"".equals(location))
			fields.put(Metadata.LOCATION, location);
		if (metadataModel.createUpdateMetadata(key, fields))
			return getMetadata(appId, userId, id, type);
		else
			return null;
	}
	
	public Boolean deleteMetadata(String appId, String userId, String id, ModelEnum type) {
		String key = getMetaKey(appId, userId, id, type);
		metadataModel = new MetadataModel(); 
		return metadataModel.deleteMetadata(key, true);
	}
	
}
