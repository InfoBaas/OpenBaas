package infosistema.openbaas.rest_resources;


import infosistema.openbaas.resourceModelLayer.AppsMiddleLayer;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;


public class UserConfirmationResource {

	private AppsMiddleLayer appsMid;
	private String appId;
	private String userId;

	@Context
	UriInfo uriInfo;

	public UserConfirmationResource(UriInfo uriInfo, AppsMiddleLayer appsMid,
			String appId, String userId) {
		this.appsMid = appsMid;
		this.appId = appId;
		this.uriInfo = uriInfo;
		this.userId = userId;
	}

	@GET
	public Response confirmEmail(@QueryParam("registrationCode") String registrationCode) {
		Response response = null;
		if (registrationCode != null) {
			String registrationCodeFromDB = appsMid.getUrlUserId(appId, userId);
			if (registrationCodeFromDB != null) {
				if (registrationCodeFromDB.equalsIgnoreCase(registrationCode)) {
					appsMid.removeUrlToUserId(appId, userId);
					appsMid.confirmUserEmail(appId, userId);
					response = Response.status(Status.OK).entity("User confirmed, you can now perform operations").build();
				}
			}
		} else {
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		}
		return response;
	}
}
