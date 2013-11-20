package infosistema.openbaas.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Error {

	private String errorMsg;

	public Error(){
		
	}
	
	public Error(String errorMsg) {
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
