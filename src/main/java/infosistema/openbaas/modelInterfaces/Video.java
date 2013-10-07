package infosistema.openbaas.modelInterfaces;

import java.io.FileOutputStream;


/**
 * Video Interface, specifies each get and possible actions performed for videos.
 * @author maniceto
 *
 */
public interface Video {
	//******************************GETS*****************************
	/**
	 * If the file was uploaded in a resolution of 480 then streaming at 720p provides no benefits.
	 * This method allows the client to check what resolutions exist and choose the adequate one.	
	 * @return Stream
	 */
	public FileOutputStream getVideoByResolution(String resolution);
	
	/**
	 * This method allows the client to check what resolutions exist.
	 * @return String with all the Resolutions accessible for this video.
	 */
	public String getAccessibleResolutions();
	/**
	 * Get video File Name.
	 * @return
	 */
	public String getFileName();
	/**
	 * Returns a Stream with the Video in the default Resolution.
	 * @return Audio stream.
	 */
//	public FileOutputStream getVideo();
	
	//******************************ACTIONS*****************************
	public String setDefaultResolution();
	/**
	 * Converts the Video to the default system type, MPEG. 
	 * @return The type of the file. If it failed returns a null String.
	 */
	public String convertVideoTypeToDefault();
	/**
	 * Set video File Name.
	 * @param fileName
	 */
	public void setFileName(String fileName);
	public void setLocation(String location);
	public void setType(String type);
	public void setId(String id);
	public void setDir(String dir);
	public void setSize(long size);
	public void setCreationDate(String date);
	public String getResolution();
	public void setResolution(String value);
	public String getType();
}
