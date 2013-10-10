package infosistema.openbaas.model.media;


import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Media extends MediaAbstract{

	public Media(String id, String dir, long size, String location) {
		super(id, dir, size, location);
		// TODO Auto-generated constructor stub
	}

	public Media() {
		// TODO Auto-generated constructor stub
	}

	
	
}
