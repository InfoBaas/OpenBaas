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
package infosistema.openbaas.data;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.mongodb.DBObject;

@XmlRootElement
public class ListResult {

	List<DBObject> obj;
	Integer pageNumber;
	
	Integer pageSize;
	Integer totalElems;
	//@JsonIgnore
	Integer totalnumberpages;
	
	public ListResult(){
		
	}
	//mandatory return
	public ListResult(List<DBObject> obj, Integer pageNumber ,Integer pageSize, Integer totalElems) {
		super();
		this.obj = obj;
		this.pageNumber = pageNumber;
		this.totalElems = totalElems;
		this.pageSize = pageSize;
	}
	
	//all return
	public ListResult(List<DBObject> obj, Integer pageNumber ,Integer pageSize, Integer totalElems, Integer totalnumberpages){
		super();
		this.obj = obj;
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
		this.totalElems = totalElems;
		this.totalnumberpages =totalnumberpages;
	}
	/*
	public ListResult(List<DBObject> obj, Integer pageNumber) {
		super();
		this.obj = obj;
		this.pageNumber = pageNumber;
	}*/
	
	public ListResult(List<DBObject> obj, Integer totalElems) {
		super();
		this.obj = obj;
		this.totalElems = totalElems;
	}
	
	public ListResult(List<DBObject> obj, Integer pageNumber,Integer totalnumberpages) {
		super();
		this.obj = obj;
		this.pageNumber = pageNumber;
		//this.totalnumberpages = totalnumberpages;
	}
	
	public List<DBObject> getIds() {
		return obj;
	}
	public void setIds(List<DBObject> obj) {
		this.obj = obj;
	}
	public Integer getPageNumber() {
		return pageNumber;
	}
	public void setPageNumber(Integer pageNumber) {
		this.pageNumber = pageNumber;
	}
	public Integer getTotalnumberpages() {
		return totalnumberpages;
	}
	public void setTotalnumberpages(Integer totalnumberpages) {
		this.totalnumberpages = totalnumberpages;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public Integer getTotalElems() {
		return totalElems;
	}

	public void setTotalElems(Integer totalElems) {
		this.totalElems = totalElems;
	}
	
	
}
