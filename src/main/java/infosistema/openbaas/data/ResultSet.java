package infosistema.openbaas.data;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ResultSet {

    private Object data;
    private Object metadata;

	public ResultSet(){
	}
	
	public ResultSet(Object data, Object metadata) {
		super();
		this.data = data;
		this.metadata = metadata;
	}
	
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Object getMetadata() {
		return metadata;
	}

	public void setMetadata(Object metadata) {
		this.metadata = metadata;
	}

}
