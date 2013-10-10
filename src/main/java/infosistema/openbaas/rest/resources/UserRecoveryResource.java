package infosistema.openbaas.rest.resources;

import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.rest.resources.AppsResource.PATCH;
import infosistema.openbaas.utils.Const;
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
		this.usersMid = MiddleLayerFactory.getUsersMiddleLayer();
		this.appId = appId;
		this.uriInfo = uriInfo;
		this.userId = userId;
	}
	/*
	 * Returns a code corresponding to the sucess or failure Codes: -2 ->
	 * Forbidden -1 -> Bad request 1 -> sessionExists
	 * CHECK FILTERS
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response makeRecoveryRequest(JSONObject inputJson, @Context UriInfo ui, @Context HttpHeaders hh,
			@HeaderParam(value = "location") String location){
		Response response = null;
		String email = null;
			try {
				email = (String) inputJson.get("email");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			String newPass="aaa";
			boolean opOk = usersMid.recoverUser(appId, userId, email, ui,newPass,null,null);
			if(opOk)
				response = Response.status(Status.OK).entity(Const.EMAIL_CONFIRMATION_SENDED).build();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String dbRecoveryCode = usersMid.getRecoveryCode(appId, userId);
	
		if(dbRecoveryCode.equalsIgnoreCase(recoveryCode)){
			this.usersMid.updateUserPassword(appId, userId, password);
			response = Response.status(Status.OK).entity("Your password has been changed.").build();
		}else{
			Response.status(Status.BAD_REQUEST).entity("Error handling the request.").build();
		}
		return response;		
	}
}
