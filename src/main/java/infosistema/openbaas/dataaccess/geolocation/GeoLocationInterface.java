package infosistema.openbaas.dataaccess.geolocation;

public interface GeoLocationInterface {

	public void createGridCache(double latPrecision, double longPrecision);
	public boolean insertObjectInGrid(double latitude, double longitude,String type, String objectId);
	
	
}
