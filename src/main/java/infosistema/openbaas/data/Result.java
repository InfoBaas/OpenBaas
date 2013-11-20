package infosistema.openbaas.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Result {

    private Object data;
    private Object metadata;

	public Result(){
	}
	
	public Result(Object data, Object metadata) {
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
