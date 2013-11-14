package infosistema.openbaas.data.models;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Image extends Media {

	public final static String RESOLUTION = "resolution";
	public final static String PIXELSIZE = "pixelsSize";
	private String defaultResolution;

	public Image(String id, String dir, long size, String fileExtension, String location) {
		super(id, dir, size, fileExtension, location);
		// TODO Auto-generated constructor stub
	}
	 
	public Image() {
		
	}

	public Image getThumbnailSize() {
		// TODO Auto-generated method stub
		return null;
	}
	 
	public Image getSmallSize() {
		// TODO Auto-generated method stub
		return null;
	}

	public Image getMediumSize() {
		// TODO Auto-generated method stub
		return null;
	}
	 
	public Image getLargeSize() {
		// TODO Auto-generated method stub
		return null;
	}

	public Image getOriginalSize() {
		// TODO Auto-generated method stub
		return null;
	}

	 
	public String getExistingSizes() {
		// TODO Auto-generated method stub
		return null;
	}
	 
	public boolean setThumbnailSize() {
		// TODO Auto-generated method stub
		return false;
	}
	 
	public boolean setSmallSize() {
		// TODO Auto-generated method stub
		return false;
	}
	 
	public boolean setMediumSize() {
		// TODO Auto-generated method stub
		return false;
	}
	 
	public boolean setLargeSize() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setPixelsSize(String res) {
		this.defaultResolution = res;
	}

	public void setResolution(String res) {
		this.defaultResolution = res;
	}

	public String getResolution() {
		return this.defaultResolution;
	}

}
