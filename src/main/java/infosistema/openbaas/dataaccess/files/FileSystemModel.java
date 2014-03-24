package infosistema.openbaas.dataaccess.files;

import infosistema.openbaas.data.enums.ModelEnum;
import infosistema.openbaas.data.models.Image;
import infosistema.openbaas.dataaccess.models.AppModel;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

public class FileSystemModel implements FileInterface {

	private static final String DIR_PATH_FORMAT = "%sapps/%s/media/%s";
	private static final String FILE_PATH_FORMAT = "%s/%s.%s";
	private static final String FILE_PATH_QUAL_FORMAT = "%s/%s_%s_%s.%s";
	private static final String ORIGINAL = "original";
	private static FileSystemModel instance;
	private static AppModel appModel =null;

	public static FileSystemModel getInstance() {
		if (instance == null) instance = new FileSystemModel();
		appModel = new AppModel();
		return instance;
	}

	private FileSystemModel() {
	}

	// *** PRIVATE *** //

	private String getDirPath(String appId, ModelEnum type) {
		return String.format(DIR_PATH_FORMAT, Const.getLocalStoragePath(), appId, type);
	}
	
	private String getFilePath(String dirPath, String id, String extension) {
		return String.format(FILE_PATH_FORMAT, dirPath, id, extension);
	}
	
	private String getFilePathWithQuality(String dirPath, String id,String quality, String extension,String bars) {
		return String.format(FILE_PATH_QUAL_FORMAT, dirPath, id,bars, quality, extension);
	}
	
	// *** CREATE *** //
	
	@Override
	public boolean createApp(String appId) throws Exception {
		return true;
	}
	
	@Override
	public boolean createUser(String appId, String userId, String userName) throws Exception {
		return false;
	}

	
	// *** UPLOAD *** //

	@Override
	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream) throws Exception {
		String dirPath = getDirPath(appId, type);
		File dirFolder = new File(dirPath);
		if (!dirFolder.exists()) dirFolder.mkdirs();
		String filePath = getFilePath(dirPath, id, extension);
		File file = new File(filePath);
		try {
			OutputStream out = new FileOutputStream(file);
			IOUtils.copy(stream, out);
			out.close();
			stream.close();
		} catch (FileNotFoundException e) {
			Log.error("", this, "upload", "File not found.", e); 
			return null;
		} catch (Exception e) {
			Log.error("", this, "upload", "An error ocorred.", e); 
			return null;
		}
		return filePath;
	}

	
	// *** DOWNLOAD *** //
	
	@Override
	public byte[] download(String appId, ModelEnum type, String id, String extension, String quality, String bars) throws IOException {
		byte[] byteArrayRes = null;
		String filePath = null;
		if(quality.equals("") || quality==null) quality=ORIGINAL;
		String filePathOriginal = getFilePath(getDirPath(appId, type), id, extension);
		
		if(quality.equals(ORIGINAL)){
			filePath = getFilePath(getDirPath(appId, type), id, extension);
		}else{
			if(type.equals(ModelEnum.image)) extension = Image.EXTENSION;
			filePath = getFilePathWithQuality(getDirPath(appId, type), id, quality, extension,bars);
		}
		File file = new File(filePath);
		try {
			if(file.exists()){
				InputStream in = new FileInputStream(file);
				byteArrayRes = IOUtils.toByteArray(in);
				in.close();
			}else{
				String qualityRes = appModel.getFileQuality(appId, type, quality);
				File fileAux = new File(filePathOriginal);
				byte[] byteArray = null;
				InputStream in = new FileInputStream(filePathOriginal);
				FileOutputStream fos = new FileOutputStream(filePath);
				byteArray = IOUtils.toByteArray(in);
				if(qualityRes!=null) qualityRes.toUpperCase();
				byteArrayRes= resizeFile(appId,byteArray, qualityRes, type, fileAux, extension,filePath,bars);
				fos.write(byteArrayRes);
				fos.close();
				in.close();
			}
		} catch (FileNotFoundException e) {
			Log.error("", this, "download", "File not found.", e); 
			return null;
		} catch (Exception e) {
			Log.error("", this, "download", "An error ocorred.", e);
			//file.delete();
			return null;
		}
		return byteArrayRes;
	}

	
	private byte[] resizeFile(String appId, byte[] byteArray, String quality, ModelEnum type, File file, String extension, String filePath, String bars) {
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
					BufferedImage originalImage = ImageIO.read(file);
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
	
	// *** DELETE *** //
	@Override
	public Boolean delFilesResolution(String appId, ModelEnum type, List<String> filesRes) {
		Boolean res = false;
		File folder = new File(getDirPath(appId, type));
		File[] listOfFiles = folder.listFiles();
		for(int i = 0; i<listOfFiles.length; i++){
			File curr = listOfFiles[i];
			if(curr.isFile()){
				String extension = FilenameUtils.getExtension(curr.getAbsolutePath());
				Iterator<String> it = filesRes.iterator();
				while(it.hasNext()){
					String fileRes = it.next();
					if(curr.getName().endsWith(fileRes+"."+extension)){
						try {
							curr.delete();
							res = true;
						} catch (Exception e) {
							Log.error("", this, "delete", "An error ocorred.", e); 
							res = false;
						}
					}
				}
			}
		}
		return res;
	}
	
	@Override
	public boolean deleteFile(String appId, ModelEnum type, String id, String extension) {
		String filePath = getFilePath(getDirPath(appId, type), id, extension);
		try {
			File file = new File(filePath);
			return file.delete();
		} catch (Exception e) {
			Log.error("", this, "delete", "An error ocorred.", e); 
		}
		return false;
	}
	
	@Override
	public void deleteUser(String appId, String userId) throws Exception {
	}
	
}
