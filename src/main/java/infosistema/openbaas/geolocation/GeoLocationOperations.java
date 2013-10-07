package infosistema.openbaas.geolocation;

public interface GeoLocationOperations {

	public void createGridCache(double latPrecision, double longPrecision);
	public boolean insertObjectInGrid(double latitude, double longitude,String type, String objectId);
	
	
}
