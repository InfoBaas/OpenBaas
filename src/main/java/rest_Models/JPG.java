package rest_Models;

import javax.xml.bind.annotation.XmlRootElement;


import modelInterfaces.Image;
import modelInterfaces.Media;

import org.codehaus.jettison.json.JSONObject;


@XmlRootElement
public class JPG extends Media implements Image{
	private String type;
	private String defaultResolution;
	public JPG(String id, String dir, long size, String location) {
		super(id, dir, size, location);
		// TODO Auto-generated constructor stub
	}
	 
	public JPG() {
		
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
	public String validateType(JSONObject json){
		return this.type;
	}
	public void setDir(String directory){
		super.setDir(directory);
	}

	@Override
	public void setImageType(String type) {
		this.type = type;
	}
	public void setSize(long size){
		super.setSize(size);
	}
	public void setCreationDate(String creationDate){
		super.setCreationDate(creationDate);
	}
	public void setFileName(String fileName){
		super.setFileName(fileName);
	}
	public void setId(String id){
		super.setId(id);
	}

	@Override
	public void setPixelsSize(String res) {
		this.defaultResolution = res;
	}

	@Override
	public void setResolution(String res) {
		this.defaultResolution = res;
	}

	@Override
	public String getResolution() {
		return this.defaultResolution;
	}

	@Override
	public void setType(String type) {
		this.type=type;		
	}

	@Override
	public String getType() {
		return type;
	}
	
}
