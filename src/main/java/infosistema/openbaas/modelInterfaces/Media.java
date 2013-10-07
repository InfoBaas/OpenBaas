package infosistema.openbaas.modelInterfaces;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Date;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.fileupload.FileItem;
import org.codehaus.jettison.json.JSONObject;

/**
 * Media Model object
 * 
 * Abstract class that implements several actions that audio/image and video require.
 * 
 * 
 * @author Miguel Aniceto
 * @version 0.0 
 */
public abstract class Media {
	private String id;
	private String creationDate;
	private long size;
	private String dir;
	private String fileName;
	private String location;
	/**
	 * Constructor
	 * 
	 */
	public Media(String id, String dir, long size, String location){
		this.id = id;
		this.dir = dir;
		this.size = size;
		creationDate = new Date().toString();
		this.location = location;
	}

	public Media(String id) {
		this.id = id;
	}
	public Media(){
		
	}
	/**
	 * Retrieves Audio/Video File in MB.
	 * @return audio File Size
	 */
	public long getSize() {
		return size;
	}
	
	/**
	 * Upload the File to the server.
	 * @return FileID
	 */
	public String uploadFile(){
		return null;
		//TO DO
	}
	/**
	 * Returns the imager identifier.
	 * @return String Image ID.
	 */
	public String getID() {
		return this.id;
	}
	public String getDir(){
		return this.dir;
	}
	/**
	 * Returns the time of creation for the image.
	 * @return Date Time
	 */
	public String getTimeOfCreation() {
		return creationDate;
	}
	
	public Response download(String appId, String id, String dir){
		String filePath = dir;
		System.out.println(dir);
		if (filePath != null && !"".equals(filePath)) {
			File file = new File(filePath);
			StreamingOutput stream = null;
			try {
				final InputStream in = new FileInputStream(file);
				stream = new StreamingOutput() {
					public void write(OutputStream out) throws IOException,
							WebApplicationException {
						try {
							int read = 0;
							byte[] bytes = new byte[1024];

							while ((read = in.read(bytes)) != -1) {
								out.write(bytes, 0, read);
							}
						} catch (Exception e) {
							throw new WebApplicationException(e);
						}
					}
				};
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
				return Response.status(404).entity(appId).build();
			}
			return Response
					.ok(stream)
					.header("content-disposition",
							"attachment; filename = " + file.getName()).build();
		}
		return Response.ok("file path null").build();
	}
	public String validateType(JSONObject json){
		return null;
	}
	public String upload(List<FileItem> items){
		String resultStatus = "";
        for (FileItem item : items) {
            if (item.isFormField()) {
                System.out.println(item.getFieldName() + "="
                        + item.getString());
            }
            if (!item.isFormField()) {
                try {
                    // System.out.println(item.getFieldName()+"="+item.getString());
                    //String filename = item.getName();
                    item.write(new File(dir + ".mp3"));
                    resultStatus = "fileupload success";
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return resultStatus;
	}
	public void setDir(String dir){
		this.dir = dir;
	}
	public void setSize(long size){
		this.size = size;
	}
	public void setCreationDate(String creationDate){
		this.creationDate = creationDate;
	}
	public void setFileName(String fileName){
		this.fileName = fileName;
	}
	public String getFileName(){
		return this.fileName;
	}
	public String getLocation(){
		return this.location;
	}
	public void setLocation(String location){
		this.location = location;
	}
	public void setId(String id){
		this.id = id;
	}
}
