/*****************************************************************************************
Infosistema - OpenBaas
Copyright(C) 2002-2014 Infosistema, S.A.
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
www.infosistema.com
info@openbaas.com
Av. José Gomes Ferreira, 11 3rd floor, s.34
Miraflores
1495-139 Algés Portugal
****************************************************************************************/
package infosistema.openbaas.middleLayer;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.PathSegment;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.mongodb.DBObject;

import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.data.QueryParameters;
import infosistema.openbaas.data.enums.FileMode;
import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.dataaccess.files.AwsModel;
import infosistema.openbaas.dataaccess.files.FileInterface;
import infosistema.openbaas.dataaccess.files.FileSystemModel;
import infosistema.openbaas.dataaccess.files.FtpModel;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.dataaccess.models.DocumentModel;
import infosistema.openbaas.dataaccess.models.MediaModel;
import infosistema.openbaas.dataaccess.models.SessionModel;
import infosistema.openbaas.dataaccess.models.UserModel;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;

public abstract class MiddleLayerAbstract {

	// *** MEMBERS *** //

	protected AppModel appModel;
	protected UserModel userModel;
	protected DocumentModel docModel;
	protected SessionModel sessionsModel;
	protected MediaModel mediaModel;

	
	// *** INIT *** //
	
	protected MiddleLayerAbstract() {
		docModel = new DocumentModel();
		appModel = new AppModel();;
		userModel = new UserModel();
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
	

	// *** PROTECTED *** //
	
	protected List<String> convertPath(List<PathSegment> path) {
		List<String> retObj = new ArrayList<String>();
		if (path != null) {
			for(PathSegment pathSegment: path) {
				if (pathSegment.getPath() != null && !"".equals(pathSegment.getPath()))
					retObj.add(pathSegment.getPath());
			}
		}
		return retObj;
	}
	
	
	// *** GET LIST *** //

	public ListResult find(QueryParameters qp, JSONArray arrayToShow) throws Exception {
		List<String> toShow = new ArrayList<String>();
		toShow = convertJsonArray2ListString(arrayToShow);
		List<DBObject> listRes = getAllSearchResults(qp.getAppId(), qp.getUserId(), qp.getUrl(), qp.getLatitude(), qp.getLongitude(), qp.getRadius(),
				qp.getQuery(), qp.getOrderType(), qp.getOrderBy(), qp.getType(),toShow);
		if(qp.getPageIndex()!=null && qp.getPageCount()!=null)
			return paginate2(listRes, qp.getPageIndex(), qp.getPageCount());
		else
			return paginate1(listRes, qp.getPageNumber(), qp.getPageSize());
	}

	protected abstract List<DBObject> getAllSearchResults(String appId, String userId, String url, Double latitude, Double longitude, Double radius, JSONObject query, String orderType, String orderBy, ModelEnum type, List<String> toShow) throws Exception;

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
	
	protected List<String> getAll(String appId, ModelEnum type) throws Exception {
		return new ArrayList<String>();
	}
	
	private ListResult paginate1(List<DBObject> lst, Integer pageNumber, Integer pageSize) {
		List<DBObject> listRes = new ArrayList<DBObject>();
		Integer iniIndex = (pageNumber-1)*pageSize;
		Integer finIndex = (((pageNumber-1)*pageSize)+pageSize);
		if (finIndex > lst.size()) finIndex = lst.size();
		try { listRes = lst.subList(iniIndex, finIndex); } catch (Exception e) {}
		Integer totalElems = (int) Utils.roundUp(lst.size(),pageSize);
		ListResult listResultRes = new ListResult(listRes, pageNumber, pageSize, lst.size(),totalElems);
		return listResultRes;
	}
	
	private ListResult paginate2(List<DBObject> lst, Integer index, Integer count) {
		List<DBObject> listRes = null;
		if (index > lst.size()-1) 
			 listRes = new ArrayList<DBObject>();
		else{
			if((index+count)>lst.size())
				try { listRes = lst.subList(index, lst.size());} catch (Exception e) {}
			else
				try { listRes = lst.subList(index, index+count);} catch (Exception e) {}
		}
		ListResult listResultRes = new ListResult(listRes, lst.size());
		listResultRes.setTotalnumberpages(999999999);
		return listResultRes;
	}
	
	protected List<String> convertJsonArray2ListString(JSONArray arrayTo) {
		List<String> res = new ArrayList<String>();
		try{
			if(arrayTo!=null){
				if(arrayTo.length()>0){
					for(int i=0; i<arrayTo.length();i++){
						Object pos = arrayTo.get(i);
						if(pos instanceof String){
							String aux = (String)pos;
							aux = aux.replace("/", ".");
							res.add(aux);
						}
					}
				}
			}			
		}catch(Exception e){
			Log.error("", this, "convertJsonArray2ListString", "An error ocorred.", e);
		}
		return res;
	}
	
}
