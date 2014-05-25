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
package infosistema.openbaas.rest;

import infosistema.openbaas.middleLayer.UsersMiddleLayer;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;


public class UserConfirmationResource {

	private UsersMiddleLayer usersMid;
	private String appId;
	private String userId;

	@Context
	UriInfo uriInfo;

	public UserConfirmationResource(UriInfo uriInfo, String appId, String userId) {
		this.usersMid = UsersMiddleLayer.getInstance();
		this.appId = appId;
		this.uriInfo = uriInfo;
		this.userId = userId;
	}

	// *** CREATE *** //
	
	// *** UPDATE *** //
	
	@GET
	public Response confirmEmail(@QueryParam("registrationCode") String registrationCode) {
		Response response = null;
		if (registrationCode != null) {
			String registrationCodeFromDB = usersMid.getUrlUserId(appId, userId);
			if (registrationCodeFromDB != null) {
				if (registrationCodeFromDB.equalsIgnoreCase(registrationCode)) {
					usersMid.removeUrlToUserId(appId, userId);
					usersMid.confirmUserEmail(appId, userId, null);
					response = Response.status(Status.OK).entity("User confirmed, you can now perform operations").build();
				}
			}
		} else {
			response = Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		}
		return response;
	}

	// *** DELETE *** //
	
	// *** GET LIST *** //
	
	// *** GET *** //
	
	// *** RESOURCES *** //

	// *** OTHERS *** //
	
}
