package simulators;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.UUID;

import modelInterfaces.Image;

import com.sun.jersey.core.header.FormDataContentDisposition;

import resourceModelLayer.AppsMiddleLayer;
import rest_Models.JPG;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		createImage("");
		//getImagesData();
	}

	private static void getImagesData() {
		try{
			AppsMiddleLayer appsMid = new AppsMiddleLayer();
			int i=1;
			PrintWriter out = new PrintWriter("/home/aniceto/test.txt");
			while(i!=201){
				String imageId = "IMAGE"+i;
				JPG temp = (JPG) appsMid.getImageInApp("296", imageId);
				String location = temp.getLocation();
				String[] loc = location.split(":");
				String latitude = loc[0];
				String longitude = loc[1];
				String text = latitude+";"+longitude+";"+imageId+";"+imageId;
				System.out.println(text);
				out.println(text);
				i++;
			}
			out.close();
		}catch(Exception e){
			System.out.println(e);
		}
	}

	private static void createImage(String locationRand) {
		try{
			DecimalFormat df2 = new DecimalFormat("00.00000");
			int i=15000;
			AppsMiddleLayer appsMid = new AppsMiddleLayer();
			InputStream is = new FileInputStream("/home/aniceto/IMG.jpg");
			while(i!=50000){
				String latitude  = getRandomLat().toString();
				String longitude = getRandomLong().toString();
				if(locationRand.equals("Iberia")){
					latitude  = df2.format(getRandomLatIberia());
					longitude = df2.format(getRandomLongIberia());
				}
				if(locationRand.equals("Lisboa")){
					latitude  = df2.format(getRandomLatLisboa());
					longitude = df2.format(getRandomLongLisboa());
				}
				String imageName = "IMAGE"+i;
				String location = latitude+":"+longitude;
				//System.out.println(latitude+";"+longitude+";"+imageName+";"+imageName+" - "+locationRand);
				System.out.println(i);
				String imageId = appsMid.createLocalFile2(is,null, "296", "jpg", "apps/296/media/images/",imageName);
				appsMid.uploadImageFileToServerWithGeoLocation("296",location, "jpg", imageName, imageId);
				i++;
			}
		}catch(Exception e){
			System.out.println(e);
		}
		
	}

	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}
	
	private static Integer getRandomLat() {
		int latitude = (int) (Math.random() * 180);
		return latitude -90;
	}
	
	private static Integer getRandomLong() {
		int longitude = (int) (Math.random() * 360);
		return longitude-180;
	}
	
	private static Double getRandomLatIberia() {
		Random r = new Random();
		double latitude = (90+36.93) + ((90+43.93) - (90+36.93)) * r.nextDouble();
		return latitude -90;
	}
	
	private static Double getRandomLongIberia() {
		Random r = new Random();
		double longitude = (170.37) + (183.38 - 170.37) * r.nextDouble();
		return longitude-180;
	}
	
	private static Double getRandomLatLisboa() {
		Random r = new Random();
		double latitude = (128.701) + ((128.80) - (128.701)) * r.nextDouble();
		
		return latitude -90;
	}
	
	private static Double getRandomLongLisboa() {
		Random r = new Random();
		double longitude = (170.708) + (170.90 - 170.708) * r.nextDouble();
		
		return longitude-180;
	}
	/*
	Random r = new Random();
	double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
	*/
	
}
