package infosistema.openbaas.model.media.video;

import infosistema.openbaas.model.media.MediaAbstract;

import java.io.FileOutputStream;

import javax.xml.bind.annotation.XmlRootElement;


import org.codehaus.jettison.json.JSONObject;

@XmlRootElement
public class Video extends MediaAbstract implements VideoInterface {
	private String type;
	private String resolution;
	public Video(String id, String dir, long size, String location) {
		super(id, dir, size, location);
		// TODO Auto-generated constructor stub
	}

	public Video() {
		// TODO Auto-generated constructor stub
	}

	public FileOutputStream getVideoByResolution(String resolution) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getAccessibleResolutions() {
		// TODO Auto-generated method stub
		return null;
	}

//	public FileOutputStream getVideo() {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public String setDefaultResolution() {
		// TODO Auto-generated method stub
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

	@Override
	public void setType(String type) {
		this.type = type;
		
	}

	@Override
	public void setId(String id) {
		super.setId(id);
	}
	@Override
	public void setSize(long size) {
		super.setSize(size);
	}
	public void setCreationDate(String date){
		super.setCreationDate(date);
	}

	@Override
	public void setResolution(String resolution) {
		this.resolution = resolution;
	}
	public String getResolution(){
		return this.resolution;
	}

	@Override
	public String getType() {
		return type;
	}
}
