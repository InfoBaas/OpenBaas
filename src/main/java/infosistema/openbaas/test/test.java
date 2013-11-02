package infosistema.openbaas.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

import org.codehaus.jettison.json.JSONException;

import org.codehaus.jettison.json.JSONObject;

import infosistema.openbaas.dataaccess.models.document.DocumentModel;


import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.google.appengine.api.files.GSFileOptions.GSFileOptionsBuilder;
import com.google.appengine.api.files.LockException;


public class test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			testData();
			writeFile("aa","bb");
			readFile("aa","bb");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private static void readFile(String BUCKETNAME, String FILENAME) throws FileNotFoundException, LockException, IOException {
		FileService fileService = FileServiceFactory.getFileService();
		String filename = "/gs/" + BUCKETNAME + "/" + FILENAME;
	    AppEngineFile readableFile = new AppEngineFile(filename);
	    FileReadChannel readChannel = fileService.openReadChannel(readableFile, false);
	    // Again, different standard Java ways of reading from the channel.
	    BufferedReader reader = new BufferedReader(Channels.newReader(readChannel, "UTF8"));
	    String line = reader.readLine();
	    // line = "The woods are lovely, dark, and deep."
	    readChannel.close();		
	}

	private static void writeFile(String BUCKETNAME, String FILENAME)throws IOException {
		
		FileService fileService = FileServiceFactory.getFileService();
	    GSFileOptionsBuilder optionsBuilder = new GSFileOptionsBuilder()
	       .setBucket(BUCKETNAME)
	       .setKey(FILENAME)
	       .setMimeType("text/html")
	       .setAcl("public-read")
	       .addUserMetadata("myfield1", "my field value");
	    AppEngineFile writableFile = fileService.createNewGSFile(optionsBuilder.build());
	    // Open a channel to write to it
	     boolean lock = false;
	     FileWriteChannel writeChannel =
	         fileService.openWriteChannel(writableFile, lock);
	     // Different standard Java ways of writing to the channel
	     // are possible. Here we use a PrintWriter:
	     PrintWriter out = new PrintWriter(Channels.newWriter(writeChannel, "UTF8"));
	     out.println("The woods are lovely dark and deep.");
	     out.println("But I have promises to keep.");
	     // Close without finalizing and save the file path for writing later
	     out.close();
	     String path = writableFile.getFullPath();
	     // Write more to the file in a separate request:
	     writableFile = new AppEngineFile(path);
	     // Lock the file because we intend to finalize it and
	     // no one else should be able to edit it
	     lock = true;
	     writeChannel = fileService.openWriteChannel(writableFile, lock);
	     // This time we write to the channel directly
	     writeChannel.write(ByteBuffer.wrap("And miles to go before I sleep.".getBytes()));

	     // Now finalize
	     writeChannel.closeFinally();

		
	}

	private static void testData() throws JSONException {
		
		DocumentModel dm = new DocumentModel();
	    //dm.insertDocumentInPath("222", null, "data.pt.lx.restaurante.nome", new JSONObject("{'nome1':'xpto1','nome2':'xpto2'}"));
	    //dm.deleteDocumentInPath("222", "data.pt.lx.restaurante");
		dm.updateDocumentInPath("222", null, "data.pt.lx.restaurante.nome", new JSONObject("{'nome10':'xpto10','nome11':'xpto11'}"));
	
	}
	
	

}
