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
