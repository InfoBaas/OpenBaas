package infosistema.openbaas.data.models;


import java.io.FileOutputStream;

import javax.xml.bind.annotation.XmlRootElement;


import org.codehaus.jettison.json.JSONObject;

@XmlRootElement
public class Video extends Media {

	public final static String RESOLUTION = "resolution";

	private String type;
	private String resolution;
	public Video(String id, String dir, long size, String location) {
		super(id, dir, size, location);
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

	public String validateType(JSONObject json) {
		return this.type;
	}
	public void setFileName(String fileName){
		super.setFileName(fileName);
	}

	public void setType(String type) {
		this.type = type;
		
	}

	public void setId(String id) {
		super.setId(id);
	}
	public void setSize(long size) {
		super.setSize(size);
	}
	public void setCreationDate(String date){
		super.setCreationDate(date);
	}

	public void setResolution(String resolution) {
		this.resolution = resolution;
	}
	public String getResolution(){
		return this.resolution;
	}

	public String getType() {
		return type;
	}
}
