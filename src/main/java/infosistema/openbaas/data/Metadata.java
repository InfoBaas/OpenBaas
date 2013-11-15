package infosistema.openbaas.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Metadata {

    private String data;

	public Metadata(){
	}
	
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
