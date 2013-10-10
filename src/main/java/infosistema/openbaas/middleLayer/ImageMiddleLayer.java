package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.models.Model;
import infosistema.openbaas.model.media.image.Image;
import infosistema.openbaas.model.media.image.ImageInterface;

import java.util.ArrayList;
import java.util.Map;

public class ImageMiddleLayer {

	// *** MEMBERS *** ///

	private Model model;
	private static final String IMAGETYPE = "image";
	private static final String MEDIAFOLDER = "media";
	private static final String IMAGESFOLDER = "/media/images";
	
	// *** INSTANCE *** ///
	
	private static ImageMiddleLayer instance = null;

	protected static ImageMiddleLayer getInstance() {
		if (instance == null) instance = new ImageMiddleLayer();
		return instance;
	}
	
	private ImageMiddleLayer() {
		model = Model.getModel(); // SINGLETON
	}

	// *** CREATE *** ///
	
	// *** UPDATE *** ///
	
	// *** DELETE *** ///
	
	// *** GET *** ///
	
	// *** OTHERS *** ///
	
	public byte[] downloadImageInApp(String appId, String imageId,String ext) {
		return this.model.downloadImageInApp(appId, imageId,ext);
	}

	public ArrayList<String> getAllImageIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllImageIdsInApp(appId, pageNumber, pageSize, orderBy, orderType);
	}

	public Integer countAllImages(String appId) {
		return this.model.countAllImagesInApp(appId);
	}

	public boolean uploadImageFileToServerWithGeoLocation(String appId, String location, String fileType,
			String fileName, String imageId) {
		return this.model.uploadFileToServer(appId, imageId, MEDIAFOLDER, IMAGETYPE, "apps/"+appId+IMAGESFOLDER, location, fileType, fileName);
	}

	public boolean uploadImageFileToServerWithoutGeoLocation(String appId, String fileType,
			String fileName, String imageId) {
		return this.model.uploadFileToServer(appId, imageId, MEDIAFOLDER, IMAGETYPE, "apps/"+appId+IMAGESFOLDER, null, fileType, fileName);
	}

	public boolean imageExistsInApp(String appId, String imageId) {
		return this.model.imageExistsInApp(appId, imageId);
	}

	public ImageInterface getImageInApp(String appId, String imageId) {
		Map<String, String> imageFields = this.model.getImageInApp(appId,
				imageId);
		ImageInterface temp = new Image();
		for (Map.Entry<String, String> entry : imageFields.entrySet()) {
			if(entry.getKey().equalsIgnoreCase("location"))
				temp.setLocation(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("creationDate"))
				temp.setCreationDate(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("dir"))
				temp.setDir(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("id"))
				temp.setId(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("size"))
				temp.setSize(Long.parseLong(entry.getValue()));
			else if (entry.getKey().equalsIgnoreCase("fileName")) 
				temp.setFileName(entry.getValue());
			else if (entry.getKey().equalsIgnoreCase("type"))
				temp.setImageType(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("id"))
				temp.setId(entry.getValue());
			else if(entry.getKey().equalsIgnoreCase("resolution"))
				temp.setResolution(entry.getValue());

		}
		return temp;
	}

	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude,
			double longitude, double radius, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return model.getAllImagesIdsInRadius(appId, latitude, longitude, radius,pageNumber,pageSize,orderBy,orderType);
	}

	public void deleteImageInApp(String appId, String imageId) {
		this.model.deleteImageInApp(appId, imageId);
	}

}
