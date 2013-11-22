package infosistema.openbaas.rest;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.ListResult;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.MiddleLayerFactory;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;


//@Path("/apps/{appId}/media")
public class MediaResource {

	//	private Map<String, Media> media = new HashMap<String, Media>();
	private String appId;
	private AppsMiddleLayer appsMid;
	private SessionMiddleLayer sessionsMid;

	//static final int idGenerator = 3;

	public MediaResource(String appId) {
		this.appId = appId;
		this.appsMid = MiddleLayerFactory.getAppsMiddleLayer();;
		this.sessionsMid = MiddleLayerFactory.getSessionMiddleLayer();;
	}

	// *** CREATE *** //

	
	// *** UPDATE *** //

	
	// *** DELETE *** //

	
	// *** GET LIST *** //

	//TODO: LOCATION
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public Response findAll(@Context UriInfo ui, @Context HttpHeaders hh,
			@QueryParam(Const.PAGE_NUMBER) Integer pageNumber, @QueryParam(Const.PAGE_SIZE) Integer pageSize, 
			@QueryParam(Const.ORDER_BY) String orderBy, @QueryParam(Const.ORDER_BY) String orderType ) {
		if (pageNumber == null) pageNumber = Const.getPageNumber();
		if (pageSize == null) 	pageSize = Const.getPageSize();
		if (orderBy == null) 	orderBy = Const.getOrderBy();
		if (orderType == null) 	orderType = Const.getOrderType();
		Response response = null;
		Cookie sessionToken=null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		for (Entry<String, List<String>> entry : headerParams.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(Const.SESSION_TOKEN))
				sessionToken = new Cookie(Const.SESSION_TOKEN, entry.getValue().get(0));
		}
		String userId = sessionsMid.getUserIdUsingSessionToken(sessionToken.getValue());
		if(Utils.getAppIdFromToken(sessionToken.getValue(), userId)!=appId)
			return Response.status(Status.UNAUTHORIZED).entity(new Error("Action in wrong app: "+appId)).build();
		int code = Utils.treatParameters(ui, hh);
		if (code == 1) {
			if(appsMid.appExists(appId)){
				ArrayList<String> mediaIds = appsMid.getAllMediaIds(appId,pageNumber,pageSize,orderBy,orderType);
				ListResult res = new ListResult(mediaIds,pageNumber);
				response = Response.status(Status.OK).entity(res).build();
			}else{
				response = Response.status(Status.NOT_FOUND).entity(new Error(appId)).build();
			}

		}else if(code == -2){
			response = Response.status(Status.FORBIDDEN).entity(new Error("Invalid Session Token.")).build();
		}else if(code == -1)
			response = Response.status(Status.BAD_REQUEST).entity(new Error("Error handling the request.")).build();
		return response;
	}

	
	// *** GET *** //

	
	// *** DOWNLOAD *** //


	// *** RESOURCES *** //

	
	// *** OTHERS *** //

}
