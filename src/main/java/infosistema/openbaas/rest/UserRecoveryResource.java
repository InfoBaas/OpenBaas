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
import infosistema.openbaas.rest.AppResource.PATCH;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class UserRecoveryResource {

	private UsersMiddleLayer usersMid;
	private String appId;
	private String userId;
	@Context
	UriInfo uriInfo;
	
	public UserRecoveryResource(UriInfo uriInfo, String appId, String userId) {
		this.usersMid = UsersMiddleLayer.getInstance();
		this.appId = appId;
		this.uriInfo = uriInfo;
		this.userId = userId;
	}

	// *** CREATE *** //
	
	// *** UPDATE *** //
	
	// *** DELETE *** //
	
	// *** GET LIST *** //
	
	// *** GET *** //
	
	// *** RESOURCES *** //

	
	// *** OTHERS *** //

	/*
	 * Returns a code corresponding to the sucess or failure Codes: -2 ->
	 * Forbidden -1 -> Bad request 1 -> sessionExists
	 * CHECK FILTERS
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response makeRecoveryRequest(JSONObject inputJson, @Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = Const.LOCATION) String location){
		Response response = null;
		String email = null;
			try {
				email = (String) inputJson.get("email");
			} catch (JSONException e) {
				Log.error("", this, "makeRecoveryRequest", "Error parsing the JSON.", e); 
			}
			String newPass="aaa";
			boolean opOk = usersMid.recoverUser(appId, userId, email, ui, newPass, null, null, null);
			if(opOk)
				response = Response.status(Status.OK).entity(Const.getEmailConfirmationSended()).build();
			else
				response = Response.status(Status.BAD_REQUEST).entity("Wrong email.").build();
		return response;
		
	}

	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response changePassword(JSONObject inputJson, @QueryParam("recoveryCode") String recoveryCode){
		String password = null;
		Response response = null;
		if(recoveryCode == null){
			return Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		}
		try {
			password = (String) inputJson.get("password");
		} catch (JSONException e) {
			Log.error("", this, "changePassword", "Error parsing the JSON.", e); 
		}
		String dbRecoveryCode = usersMid.getRecoveryCode(appId, userId);
	
		if(dbRecoveryCode.equalsIgnoreCase(recoveryCode)){
			this.usersMid.updateUserPassword(appId, userId, password, null);
			response = Response.status(Status.OK).entity("Your password has been changed.").build();
		}else{
			Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		}
		return response;		
	}
}
