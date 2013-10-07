package infosistema.openbaas.rest_Models;

import infosistema.openbaas.modelInterfaces.Media;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Storage extends Media{

	public Storage(String id, String dir, long size, String location) {
		super(id, dir, size, location);
		// TODO Auto-generated constructor stub
	}

	public Storage() {
		// TODO Auto-generated constructor stub
	}

	
	
}
