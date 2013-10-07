package infosistema.openbaas.modelInterfaces;

import java.util.ArrayList;
import javax.ws.rs.core.Response;

/**
 * Audio Interface, specifies each get and possible actions performed for images.
 * @author maniceto
 *
 */
public interface Audio {

	//******************************GETS*****************************
//	public FileOutputStream getAudioByBitRate(String bitrate);
	/**
	 * If the file was uploaded in a rate of 128 kbits/s then streaming at 320 kbits/s provides no benefits.
	 * This method allows the client to check what bitrates exist and choose the adequate one.	
	 * @return String with all the BitRates accessible.
	 */
	public ArrayList<Integer> getAccessibleBitRates();
	/**
	 * Returns a Stream with the Audio in the default BitRate.
	 * @return Audio stream.
	 */
//	public FileOutputStream getAudio();
	/**
	 * Evaluates if the Bitrate "bitrate" is allowed for audio and returns a Stream if it is.
	 * @param bitrate 
	 * @return FileOutputStream
	 */
//	public FileOutputStream getAudioByBitRate(int bitrate);
	public String getDir();
	public String getFileName();
	//******************************ACTIONS*****************************
	/**
	 * Sets the bitRate for the value that comes as parameter.
	 * @return
	 */
	public boolean setDefaultBitRate(String bitrate);
	/**
	 * Converts the Audio to the default system type, mp3. 
	 * @return The type of the file. If it failed returns a null String.
	 */
	public boolean convertAudioTypeToDefault();
	boolean convertAudioTypeToDefault(String dir, long size,
			int defaultBitRate, String type);
	public boolean setAudioType(String type);
	public void setMaxBitRate(String bitRate);
	public Response download(String appId, String audioId, String dir);
	public void setDir(String dir);
	public void setSize(long size);
	public void setCreationDate(String creationDate);
	public void setFileName(String fileName);
	public void setLocation(String location);
	public void setType(String type);
	public String getType();
}
