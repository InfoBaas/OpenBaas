package infosistema.openbaas.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ErrorSet {

	private String errorMsg;

	public ErrorSet(){
		
	}
	
	public ErrorSet(String errorMsg) {
		super();
		this.errorMsg = errorMsg;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
}
