package infosistema.openbaas.middleLayer;

import infosistema.openbaas.dataaccess.models.Model;
import infosistema.openbaas.model.media.image.Image;
import infosistema.openbaas.model.media.image.ImageInterface;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import com.sun.jersey.core.header.FormDataContentDisposition;

public class ImageMiddleLayer extends MediaMiddleLayer {

	// *** MEMBERS *** ///

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
	
	public String uploadImage(InputStream uploadedInputStream, FormDataContentDisposition fileDetail, String appId, String location) {
		String fileNameWithType = null;
		String fileType = new String();
		String fileName = new String();
		boolean uploadOk = false;
		fileNameWithType = fileDetail.getFileName();
		String fileDirectory = "apps/"+appId+"/media/images/";
		char[] charArray = fileNameWithType.toCharArray();
		boolean pop = false;
		int i = 0;
		while (!pop) {
			fileName += charArray[i];
			if (charArray[i++] == '.')
				pop = true;
		}
		for (int k = 0; k < charArray.length - 1; k++) {
			if (charArray[k] == '.') {
				for (int j = k + 1; j < charArray.length; j++)
					fileType += charArray[j];
			}
		}
		String id = MiddleLayerFactory.getStorageMiddleLayer().createLocalFile(uploadedInputStream, fileDetail, appId, fileType, fileDirectory);
		uploadOk = uploadImageFileToServer(appId, location, fileType, fileName, id);
		if(id != null && uploadOk)
			return id;
		else
			return null;
	}
	
	public boolean uploadImageFileToServer(String appId, String location, String fileType, String fileName, String imageId) {
		return this.model.uploadFileToServer(appId, imageId, MEDIAFOLDER, IMAGETYPE, "apps/"+appId+IMAGESFOLDER, location, fileType, fileName);
	}

	
	// *** UPDATE *** ///
	
	
	// *** DELETE *** ///
	
	
	// *** GET LIST *** ///
	
	public ArrayList<String> getAllImageIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		return this.model.getAllImageIdsInApp(appId, pageNumber, pageSize, orderBy, orderType);
	}

	
	// *** GET *** ///
	
	public byte[] downloadImageInApp(String appId, String imageId,String ext) {
		return this.model.downloadImageInApp(appId, imageId,ext);
	}


	// *** OTHERS *** ///
	
	public Integer countAllImages(String appId) {
		return this.model.countAllImagesInApp(appId);
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
