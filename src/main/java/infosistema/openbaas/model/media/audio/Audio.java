package infosistema.openbaas.model.media.audio;

import infosistema.openbaas.model.media.MediaAbstract;

import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;



import org.codehaus.jettison.json.JSONObject;

@XmlRootElement
public class Audio extends MediaAbstract implements AudioInterface{
	private static final int [] allBitRates = new int[]{32,40,48, 56, 64,80,96,112,128,144,160,192,224,256,320};
	
	private ArrayList<Integer> accessibleBitRates = new ArrayList<Integer>();;
	@SuppressWarnings("unused")
	private int defaultBitRate;
	private int maxBitRate;
	private String type;
	
	
	public Audio(String audioId, String dir, long size, int defaultBitRate, String type, String location) {
		super(audioId, dir, size, location);

		//de 0 até no maximo 15, complexidade temporal desprezavel
		for(int i = 0; i < allBitRates.length; i++){
			if(allBitRates[i] <= defaultBitRate){
				accessibleBitRates.add(allBitRates[i]);
			}
		}
		if(!type.equals("mp3")){
			this.convertAudioTypeToDefault(dir, size, defaultBitRate, type);
		}
	}
	//******************************SETS*****************************

	 
	public Audio(String audioId) {
		super(audioId);
		for(int i = 0; i < allBitRates.length; i++){
			this.accessibleBitRates.add(allBitRates[i]);
		}
	}
	public Audio(){
		
	}

	public FileOutputStream getAudioByBitRate(String bitrate) {
		// TODO Auto-generated method stub
		return null;
	}

	 
	public ArrayList<Integer> getAccessibleBitRates() {
		return this.accessibleBitRates;
	}

	 
	public FileOutputStream getAudio() {
		// TODO Auto-generated method stub
		return null;
	}

	 
	public FileOutputStream getAudioByBitRate(int bitrate) {
		// TODO Auto-generated method stub
		return null;
	}

	 
	public boolean setDefaultBitRate(String bitRateInput) {
		int bitRate = Integer.parseInt(bitRateInput);
		boolean sucess = false;
		if(accessibleBitRates.contains(bitRate) && bitRate < maxBitRate){
			this.defaultBitRate = bitRate;
			sucess = true;
		}
		return sucess;
	}

	 
	public boolean convertAudioTypeToDefault(String dir, long size, int defaultBitRate2, String type2) {
		// TODO Auto-generated method stub
		return false;
	}

	 
	public boolean convertAudioTypeToDefault() {
		// TODO Auto-generated method stub
		return false;
	}

	 
	public boolean setAudioType(String type) {
		this.type = type;
		return true;
	}
	public String validateType(JSONObject json){
		return this.type;
	}
	public void setMaxBitRate(String maxBitRate){
		this.maxBitRate = Integer.parseInt(maxBitRate);
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


	@Override
	public void setType(String type) {
		this.type=type;
	}


	@Override
	public String getType() {
		return type;
	}



}
