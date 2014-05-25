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
package infosistema.openbaas.utils.geolocation;

import infosistema.openbaas.utils.Const;

public class Geo {

	//TODO: fazer por app
	private double latp = Const.getLatitudePrecision();
	private double longp = Const.getLongitudePrecision();

	private static Geo instance = null;
	
	private Geo() {
	}
	
	public static Geo getInstance() {
		if (instance == null) instance = new Geo();
		return instance;
	}

	// *** AUX *** //
	
	public double getGridLatitude(double latitude) {
		latitude += 90;
		return ((int)(latitude / latp)) * latp; 
	}

	public double getGridLongitude(double longitude) {
		longitude += 180;
		return ((int)(longitude / longp)) * longp; 
	}

	//Distances 
	public double transformMetersInDegreesLat(double meters) {
		return ((meters/1000)/110.54);
	}

	public double transformMetersInDegreesLong(double meters, double currentLat) {
		return Math.abs((meters/1000)/(111.320*Math.cos(currentLat)));
	}

	public double getDistanceFromLatLonInKm(double lat1,double lon1,double lat2,double lon2) {
		double R = 6371; // Radius of the earth in km
		  double dLat = deg2rad(lat2-lat1);  // deg2rad below
		  double dLon = deg2rad(lon2-lon1); 
		  double a = 
		    Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(deg2rad(lat1)) * 
		    Math.cos(deg2rad(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2); 
		  double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		  double d = R * c; // Distance in km
		  return d;
	}

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	private double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}

	public double distance(double lat1, double lon1, double lat2, double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
				* Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		dist = dist * 1.609344;
		return (dist);
	}
	
	public boolean isWithinDistance(Double objLatitude, Double objLongitude, Double latitude, Double longitude, Double radius) {
		double dist2Or = getDistanceFromLatLonInKm(latitude, longitude, objLatitude, objLongitude);
		return dist2Or <= (radius / 1000);
	}

}
