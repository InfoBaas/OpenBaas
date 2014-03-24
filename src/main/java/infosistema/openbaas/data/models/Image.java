package infosistema.openbaas.data.models;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Image extends Media {

	public final static String RESOLUTION = "resolution";
	public final static String PIXELSIZE = "pixelsSize";
	public final static String EXTENSION = "PNG";

	public Image(String id, String dir, long size, String fileExtension, String location) {
		super(id, dir, size, fileExtension, location);
		// TODO Auto-generated constructor stub
	}
	 
	public Image() {
		
	}
}
