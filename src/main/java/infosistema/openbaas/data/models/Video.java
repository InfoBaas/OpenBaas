package infosistema.openbaas.data.models;

import java.io.FileOutputStream;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Video extends Media {

	public final static String RESOLUTION = "resolution";

	private String resolution;

	public Video(String id, String dir, long size, String fileExtension, String location) {
		super(id, dir, size, fileExtension, location);
	}

	public Video() {
	}

	public FileOutputStream getVideoByResolution(String resolution) {
		return null;
	}

	public String getAccessibleResolutions() {
		return null;
	}

	public String setDefaultResolution() {
		return null;
	}

	public String convertVideoTypeToDefault() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}
	public String getResolution(){
		return this.resolution;
	}

}
