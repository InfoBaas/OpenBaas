package infosistema.openbaas.data.models;

import java.io.FileOutputStream;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Audio extends Media {

	public final static String BITRATE = "bitRate";

	private static final int [] allBitRates = new int[]{32,40,48, 56, 64,80,96,112,128,144,160,192,224,256,320};
	
	private ArrayList<Integer> accessibleBitRates = new ArrayList<Integer>();;
	@SuppressWarnings("unused")
	private int defaultBitRate;
	private int maxBitRate;
	
	public Audio(String id, String dir, long size, int defaultBitRate, String fileExtension, String location) {
		super(id, dir, size, fileExtension, location);

		//de 0 até no maximo 15, complexidade temporal desprezavel
		for(int i = 0; i < allBitRates.length; i++){
			if(allBitRates[i] <= defaultBitRate){
				accessibleBitRates.add(allBitRates[i]);
			}
		}
		if(!fileExtension.equals("mp3")){
			this.convertAudioTypeToDefault(dir, size, defaultBitRate, fileExtension);
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

	public void setMaxBitRate(String maxBitRate){
		this.maxBitRate = Integer.parseInt(maxBitRate);
	}

}
