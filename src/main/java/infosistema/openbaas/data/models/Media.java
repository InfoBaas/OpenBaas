/*****************************************************************************************
Infosistema - OpenBaas
Copyright(C) 2002-2014 Infosistema, S.A.
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
www.infosistema.com
info@openbaas.com
Av. José Gomes Ferreira, 11 3rd floor, s.34
Miraflores
1495-139 Algés Portugal
****************************************************************************************/
package infosistema.openbaas.data.models;

import infosistema.openbaas.utils.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.fileupload.FileItem;

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

	public final static String _ID = "_id";
	public final static String PATH = "dir";
	public final static String FILENAME = "fileName";
	public final static String FILEEXTENSION = "fileExtension";
	public final static String SIZE = "size";
	public final static String LOCATION = "location";

	private String _id;
	private String fileExtension;
	private long size;
	private String dir;
	private String fileName;
	private String location;
	/**
	 * Constructor
	 * 
	 */
	public Media(String _id, String dir, long size, String fileExtension, String location){
		this._id = _id;
		this.dir = dir;
		this.size = size;
		this.fileExtension = fileExtension;
		this.location = location;
	}

	public Media(String _id) {
		this._id = _id;
	}
	public Media(){
		
	}

	public long getSize() {
		return size;
	}
	
	public String uploadFile(){
		return null;
		//TO DO
	}

	public String get_id() {
		return this._id;
	}

	public String getDir(){
		return this.dir;
	}

	public String getFileExtension() {
		return fileExtension;
	}
	
	public Response download(String appId, String id, String dir){
		String filePath = dir;
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
		    				Log.error("", this, "download", "An error ocorred.", e); 
							throw new WebApplicationException(e);
						}
					}
				};
				in.close();
			} catch (Exception e) {
				Log.error("", this, "download", "Error downloading media.", e); 
				return Response.status(404).entity(appId).build();
			}
			return Response
					.ok(stream)
					.header("content-disposition",
							"attachment; filename = " + file.getName()).build();
		}
		return Response.ok("file path null").build();
	}

	public String upload(List<FileItem> items){
		String resultStatus = "";
        for (FileItem item : items) {
            if (!item.isFormField()) {
                try {
                    item.write(new File(dir + ".mp3"));
                    resultStatus = "fileupload success";
                } catch (Exception e) {
    				Log.error("", this, "upload", "An error ocorred.", e); 
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

	public void setFileExtension(String fileExtension){
		this.fileExtension = fileExtension;
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

	public void set_id(String _id){
		this._id = _id;
	}
}
