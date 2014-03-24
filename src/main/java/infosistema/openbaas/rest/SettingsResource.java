package infosistema.openbaas.rest;

import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class SettingsResource {
	
	public SettingsResource(String appId) {
	}
	
	@Path("/notifications/APNS")
	public APNSResource apns(@PathParam(Const.APP_ID) String appId) {
		try {
			return new APNSResource(appId);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "storage", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}
	
}
