package infosistema.openbaas.middleLayer;

import infosistema.openbaas.model.media.image.Image;
import infosistema.openbaas.model.media.image.ImageInterface;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.jersey.core.header.FormDataContentDisposition;

public class ImageMiddleLayer extends MediaMiddleLayer {

	// *** MEMBERS *** //

	private static final String IMAGETYPE = "image";
	private static final String MEDIAFOLDER = "media";
	private static final String IMAGESFOLDER = "/media/images";
	
	// *** INSTANCE *** //
	
	private static ImageMiddleLayer instance = null;

	protected static ImageMiddleLayer getInstance() {
		if (instance == null) instance = new ImageMiddleLayer();
		return instance;
	}
	
	private ImageMiddleLayer() {
	}

	// *** CREATE *** //
	
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
		return uploadFileToServer(appId, imageId, MEDIAFOLDER, IMAGETYPE, "apps/"+appId+IMAGESFOLDER, location, fileType, fileName);
	}

	
	// *** UPDATE *** //
	
	
	// *** DELETE *** //
	
	public void deleteImageInApp(String appId, String imageId) {
		String fileDirectory = getFileDirectory(appId, imageId, MEDIAFOLDER, IMAGES);
		deleteFile(fileDirectory);
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			mongoModel.deleteImageInApp(appId, imageId);
			if (redisModel.imageExistsInApp(appId, imageId)) {
				redisModel.deleteImageInApp(appId, imageId);
			}
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
	}

	
	// *** GET LIST *** //
	
	public ArrayList<String> getAllImageIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		if (auxDatabase.equalsIgnoreCase(MONGODB)) {
			return mongoModel.getAllImageIdsInApp(appId,pageNumber, pageSize, orderBy, orderType);
		} else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return null;
	}

	
	public ArrayList<String> getAllImagesIdsInRadius(String appId, double latitude, double longitude, double radius,
			Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		try{
			return docModel.getAllImagesIdsInRadius(appId, latitude, longitude, radius,pageNumber,pageSize,orderBy,orderType);
		} catch (Exception e) {
			return null;
		}
	}

	// *** GET *** //
	
	public ImageInterface getImageInApp(String appId, String imageId) {
		Map<String, String> imageFields = redisModel.getImageInApp(appId, imageId);

		if (imageFields == null || imageFields.size() == 0) {
			String dir = null;
			String type = null;
			String size = null;
			String pixelsSize = null;
			String creationDate = null;
			String fileName = null;
			String location = null;
			if (auxDatabase.equalsIgnoreCase(MONGODB)) {
				imageFields = mongoModel.getImageInApp(appId, imageId);
				if (redisModel.getCacheSize() <= MAXCACHESIZE) {
					for (Entry<String, String> entry : imageFields.entrySet()) {
						if (entry.getKey().equalsIgnoreCase("dir"))
							dir = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("type"))
							type = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("size"))
							size = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("pixelsSize"))
							pixelsSize = entry.getValue();
						else if (entry.getKey()
								.equalsIgnoreCase("creationDate"))
							creationDate = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("fileName"))
							fileName = entry.getValue();
						else if (entry.getKey().equalsIgnoreCase("location"))
							location = entry.getValue();

					}
					redisModel.createImageInApp(appId, imageId, dir, type,
							size, pixelsSize, creationDate, fileName, location);
				} else {
					System.out.println("Warning: Cache is full.");
				}
			} else {
				System.out.println("Database not implemented.");
			}
		} else {
			imageFields.put("id", imageId);
			imageFields.put("appId", appId);
		}

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


	// *** UPLOAD *** //

	// *** DOWNLOAD *** //

	public byte[] downloadImageInApp(String appId, String imageId,String ext) {
		return download(appId, MEDIAFOLDER, IMAGES, imageId,ext);
	}


	// *** EXISTS *** //

	public boolean imageExistsInApp(String appId, String imageId) {
		if (redisModel.imageExistsInApp(appId, imageId))
			return true;
		else if (auxDatabase.equalsIgnoreCase(MONGODB))
			return mongoModel.imageExistsInApp(appId, imageId);
		else if (!auxDatabase.equalsIgnoreCase(MONGODB))
			System.out.println("Database not implemented.");
		return false;
	}

	
	// *** OTHERS *** //
	
	public Integer countAllImages(String appId) {
		return mongoModel.countAllImagesInApp(appId);
	}
}
