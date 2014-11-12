package infosistema.openbaas.dataaccess.files;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class FilesUtils {

	private static final String DIR_PATH_FORMAT = "%sapps/%s/media/%s";
	private static final String FILE_PATH_FORMAT = "%s/%s.%s";
	private static final String FILE_PATH_QUAL_FORMAT = "%s/%s_%s_%s.%s";
	private static FilesUtils instance;
	private static AppModel appModel =null;
	static final String ORIGINAL = "original";

	public static FilesUtils getInstance() {
		if (instance == null) instance = new FilesUtils();
		appModel = AppModel.getInstance();
		return instance;
	}

	private FilesUtils() {
	}
	
	public static String getDirPath(String appId, ModelEnum type) {
		return String.format(DIR_PATH_FORMAT, Const.getLocalStoragePath(), appId, type);
	}
	
	public static String getFilePath(String dirPath, String id, String extension) {
		return String.format(FILE_PATH_FORMAT, dirPath, id, extension);
	}
	
	public static String getFilePathWithQuality(String dirPath, String id,String quality, String extension,String bars) {
		return String.format(FILE_PATH_QUAL_FORMAT, dirPath, id,bars, quality, extension);
	}
	
	public byte[] resizeFile(String appId, byte[] byteArray, String quality, ModelEnum type, File file, String extension, String filePath, String bars) {
		byte[] res=null;
		try {
			if(quality.equals(ORIGINAL))
				return byteArray;
			else{
				if(type.equals(ModelEnum.image)){
					int IMG_WIDTH=100;
					int IMG_HEIGHT=100;
					String barsRes = appModel.getFileQuality(appId, ModelEnum.bars, bars);
					String[] qualityArray = quality.split("X");
					IMG_WIDTH = Integer.parseInt(qualityArray[0]);
					IMG_HEIGHT = Integer.parseInt(qualityArray[1]);
					
					InputStream in = new ByteArrayInputStream(byteArray);
					BufferedImage originalImage = ImageIO.read(in);
					
					//BufferedImage originalImage = ImageIO.read(file);
					int fileType = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
					res = resizeImage(originalImage, fileType, IMG_WIDTH, IMG_HEIGHT, Image.EXTENSION, filePath,barsRes);
				}
				if(type.equals(ModelEnum.video)){
					res = byteArray;
				}
				if(type.equals(ModelEnum.storage)){
					res = byteArray;
				}
				if(type.equals(ModelEnum.audio)){
					res = byteArray;
				}
			}
		} catch (Exception e) {
			Log.error("", this, "resize file", "An error ocorred.", e); 
		}
		return res;
	}

	private byte[] resizeImage(BufferedImage originalImage, int type, int finalWidth, int finalHeight, String extension, String filePath, String bars){
		byte[] imageInByte = null;
		try {
						
			int originalWidth = originalImage.getWidth();
		    int originalHeight = originalImage.getHeight();

		    int newWidth;
		    int newHeight;
		   
		    double aspectRatio = (double) originalWidth / (double) originalHeight;
		    double boundaryAspect = (double) finalWidth / (double) finalHeight;

		    if (aspectRatio > boundaryAspect) {
		        newWidth = finalWidth;
		        newHeight = (int) Math.round(newWidth / aspectRatio);
		    } else {
		        newHeight = finalHeight;
		        newWidth = (int) Math.round(aspectRatio * newHeight);
		    }

		    int xOffset = (finalWidth - newWidth) / 2;
		    int yOffset = (finalHeight - newHeight) / 2;
		    
		    BufferedImage intermediateImage=null;
		    if(bars!=null){
		    	intermediateImage = new BufferedImage(finalWidth, finalHeight, BufferedImage.TYPE_INT_ARGB);
			    Graphics2D gi = intermediateImage.createGraphics();
			    Color color1 = Color.decode("0x"+bars.substring(0, 6));
			    Color color2 = new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), Integer.parseInt(bars.substring(6),16));
			    gi.setColor(color2);
			    gi.fillRect(0, 0, finalWidth, finalHeight);
			    gi.drawImage(originalImage, xOffset, yOffset, xOffset + newWidth, yOffset + newHeight, 0, 0, originalWidth, originalHeight, null);
		    }else{
		    	intermediateImage = new BufferedImage(finalWidth, finalHeight, type);
		    	Graphics2D gi = intermediateImage.createGraphics();
		    	gi.drawImage(originalImage, 0, 0, finalWidth, finalWidth, null);
			    gi.dispose();
		    }
		    
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(intermediateImage, extension, baos);
			
			baos.flush();
			imageInByte = baos.toByteArray();
			baos.close();
		} catch (Exception e) {
			Log.error("", this, "resize image", "An error ocorred.", e); 
		}
		return imageInByte;
	}
}
