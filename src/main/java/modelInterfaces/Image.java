package modelInterfaces;

/**
 * Image Interface, specifies each get and possible actions performed for images.
 * @author maniceto
 *
 */
public interface Image{
	//******************************GETS*****************************
	/**
	 * Allows to get the thumbnail size of this image.
	 * @return Image
	 */
//	public Image getThumbnailSize();
	/**
	 * Allows to get the small size of this image, if the original image allows it.
	 * @return Image
	 */
//	public Image getSmallSize();
	/**
	 * Allows to get the medium size of this image, if the original image allows it.
	 * @return Image
	 */
//	public Image getMediumSize();
	/**
	 * Allows to get the large size of this image, if the original image allows it.
	 * @return Image
	 */
//	public Image getLargeSize();
	/**
	 * Allows to get the original image.
	 * @return Image
	 */
//	public Image getSize();
	
	/**
	 * Returns the existing sizes of this image, it is not always thumbnail, small, medium and large.
	 * A small image will not be available in a large format due to pixel stretching.
	 * @return
	 */
	public String getExistingSizes();
	public String getFileName();
	//******************************ACTIONS*****************************
//	public boolean setThumbnailSize();
//	public boolean setSmallSize();
//	public boolean setMediumSize();
//	public boolean setLargeSize();

	public void setDir(String value);
	public void setSize(long size);
	public void setFileName(String fileName);
	public void setLocation(String location);
	public void setId(String id);
	public void setCreationDate(String date);
	public void setImageType(String type);
	public void setPixelsSize(String value);
	public void setResolution(String value);
	public String getResolution();
	public void setType(String type);
	public String getType();
}
