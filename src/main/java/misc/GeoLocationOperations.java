package misc;

public interface GeoLocationOperations {

	public void createGridCache(double latPrecision, double longPrecision);
	public String determinePointInGrid(double latitude, double longitude) ;
	public boolean insertObjectInGrid(double latitude, double longitude,
			String type, String objectId);
	
	
}
