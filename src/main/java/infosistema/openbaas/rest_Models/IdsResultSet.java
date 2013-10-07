package infosistema.openbaas.rest_Models;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IdsResultSet {

	List<String> ids;
	Integer pageNumber;
	//@JsonIgnore
	Integer totalnumberpages;
	
	public IdsResultSet(){
		
	}
	
	public IdsResultSet(List<String> ids, Integer pageNumber) {
		super();
		this.ids = ids;
		this.pageNumber = pageNumber;
	}
	
	public IdsResultSet(List<String> ids, Integer pageNumber,Integer totalnumberpages) {
		super();
		this.ids = ids;
		this.pageNumber = pageNumber;
		this.totalnumberpages = totalnumberpages;
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
	
	
}
