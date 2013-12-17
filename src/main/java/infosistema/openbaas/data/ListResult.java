package infosistema.openbaas.data;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ListResult {

	List<String> ids;
	Integer pageNumber;
	
	Integer pageSize;
	Integer totalElems;
	//@JsonIgnore
	Integer totalnumberpages;
	
	public ListResult(){
		
	}
	//mandatory return
	public ListResult(List<String> ids, Integer pageNumber ,Integer pageSize, Integer totalElems) {
		super();
		this.ids = ids;
		this.pageNumber = pageNumber;
		this.totalElems = totalElems;
		this.pageSize = pageSize;
	}
	
	//all return
	public ListResult(List<String> ids, Integer pageNumber ,Integer pageSize, Integer totalElems, Integer totalnumberpages){
		super();
		this.ids = ids;
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
		this.totalElems = totalElems;
		this.totalnumberpages =totalnumberpages;
	}
	
	public ListResult(List<String> ids, Integer pageNumber) {
		super();
		this.ids = ids;
		this.pageNumber = pageNumber;
	}
	
	public ListResult(List<String> ids, Integer pageNumber,Integer totalnumberpages) {
		super();
		this.ids = ids;
		this.pageNumber = pageNumber;
		//this.totalnumberpages = totalnumberpages;
	}
	
	public List<String> getIds() {
		return ids;
	}
	public void setIds(List<String> ids) {
		this.ids = ids;
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
