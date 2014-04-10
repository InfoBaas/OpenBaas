
package infosistema.openbaas.rest;


import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import infosistema.openbaas.data.Error;
import infosistema.openbaas.data.Metadata;
import infosistema.openbaas.data.Result;
import infosistema.openbaas.data.models.Application;
import infosistema.openbaas.data.models.User;
import infosistema.openbaas.middleLayer.AppsMiddleLayer;
import infosistema.openbaas.middleLayer.SessionMiddleLayer;
import infosistema.openbaas.middleLayer.UsersMiddleLayer;
import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;
import infosistema.openbaas.utils.Utils;
import infosistema.openbaas.utils.ApplePushNotifications;

import javapns.communication.exceptions.CommunicationException;
import javapns.communication.exceptions.KeystoreException;
import javapns.devices.Device;
import javapns.devices.exceptions.InvalidDeviceTokenFormatException;
import javapns.devices.implementations.basic.BasicDevice;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class IntegrationResource {

	
	private UsersMiddleLayer usersMid;
	private String appId;
	private SessionMiddleLayer sessionMid;
	private AppsMiddleLayer appsMid;
 

    // iPhone's UDID (64-char device token)
     
     private static String CERTIFICATE = "/home/administrator/baas/apps/8917/media/storage/storage:d302.p12";
     private static String PASSWORD = "qkqd8ur9vfur";

	 
	@Context
	UriInfo uriInfo;
	
	public IntegrationResource(String appId) {
		this.usersMid = UsersMiddleLayer.getInstance();
		this.appId = appId;
		this.sessionMid = SessionMiddleLayer.getInstance();
		this.appsMid = AppsMiddleLayer.getInstance();
	}
	
    
	@Path("/test")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response test(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh){

		List<Device> list = new ArrayList<Device>();
		try {
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
			list.add(new BasicDevice("d287e62da2550ea8163dd0733711bb0a17743e678d3d5a637cb01d04ce33668e"));
		} catch (InvalidDeviceTokenFormatException e1) {
			Log.error("", this, "CommunicationException", "Error Communication Exception0.", e1); 
		}
		
		try {
			ApplePushNotifications.pushCombineNotification("test", 0, CERTIFICATE,PASSWORD,false,list);
		} catch (CommunicationException e) {
			Log.error("", this, "CommunicationException", "Error Communication Exception1.", e); 
		} catch (KeystoreException e) {
			Log.error("", this, "KeystoreException", "Error Keystore Exception.", e);
		}
		
		return Response.status(Status.OK).entity("DEL OK").build();
	}
	
	@Path("/test2")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response test2(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh){
		//Serve para apagar coisas do redis
		String delKey = null;
		JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisChatServer(),Const.getRedisChatPort());
		//JedisPool pool = new JedisPool(new JedisPoolConfig(), Const.getRedisSessionServer(),Const.getRedisSessionPort());
		try {
			delKey = inputJsonObj.getString("key");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Jedis jedis = pool.getResource();
		try {
			Set<String> a = jedis.keys(delKey+"*");
			Iterator<String> it =  a.iterator();
			while(it.hasNext()){
				String s = it.next();
				jedis.del(s);
				//System. out. println(s);
			}
		} finally {
			pool.returnResource(jedis);
		}

		
		return Response.status(Status.OK).entity("DEL OK").build();
	}
	
	@Path("/test3")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response test3(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh){
		
		OutputStream stream=null;
		try {
			StringBuffer str = new StringBuffer((String) inputJsonObj.get("str"));
			byte[] data = Base64.decodeBase64(str.toString());
			
			
			stream = new FileOutputStream("/home/aniceto/baas/test.png") ;
		
		
			stream.write(data);
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				stream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		return Response.status(Status.OK).entity("DEL OK").build();
	}
	
	
	@Path("/test4")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response test4(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) throws IOException{
		Date startDate = Utils.getDate();
		String everything="";
		 BufferedReader br = new BufferedReader(new FileReader("/home/aniceto/baas/file.txt"));
		    try {
		        StringBuilder sb = new StringBuilder();
		        String line = br.readLine();

		        while (line != null) {
		            sb.append(line);
		            sb.append("\n");
		            line = br.readLine();
		        }
		        everything = sb.toString();
		    } finally {
		        br.close();
		    }
		
		
		Socket echoSocket = null;
	    try {
	      echoSocket = new Socket("askme2.cl.infosistema.com", 4005);
	      PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
	      
	      //StringBuffer str = new StringBuffer((String) inputJsonObj.get("str"));
	      
	      //String s = str.toString();//"/// DEVES COLOCAR AQUI A IMAGEM SERIALIZADA COM Base64";
	      String s = everything;
	      byte[] c = s.getBytes();
	      echoSocket.getOutputStream().write(c);
	      out.flush();
	      s ="\n";
	      c = s.getBytes();
	      echoSocket.getOutputStream().write(c);
	      out.flush();
	      System.out.println("OK");
	    } catch (Exception e) {
	      e.printStackTrace();
	    } finally {
	      try {
	        echoSocket.close();
	      } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	      }
	    }
	    Date endDate = Utils.getDate();
	    System.out.println(Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
		
		return Response.status(Status.OK).entity("DEL OK").build();
	}
	
	/**
	 * Creates a user in the application. Necessary fields: "facebook id"
	 * and "email". if the user already register only signin. if not signup
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@Path("/facebook")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrLoginFacebookUser(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Date startDate = Utils.getDate();
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		String email = null;
		String name = null;
		String socialNetwork = null;
		String socialId = null;
		//String userSocialId = null;
		String userName = null;
		String userAgent = null;
		String location = null;
		String appKey = null;
		String fbToken = null;
		String lastLocation = null;
		User outUser = new User();
		String userId =null;
		try {
			location = headerParams.getFirst(Const.LOCATION);
		} catch (Exception e) { }
		try {
			userAgent = headerParams.getFirst(Const.USER_AGENT);
		} catch (Exception e) { }
		try {
			appKey = headerParams.getFirst(Application.APP_KEY);
		} catch (Exception e) { }
		if(appKey==null)
			return Response.status(Status.BAD_REQUEST).entity("App Key not found").build();
		if(!appsMid.authenticateApp(appId,appKey))
			return Response.status(Status.UNAUTHORIZED).entity("Wrong App Key").build();
		
		try {
			fbToken = (String) inputJsonObj.get("fbToken");
			JSONObject jsonReqFB = getFBInfo(fbToken);
			if(jsonReqFB == null)
				return Response.status(Status.BAD_REQUEST).entity("Bad FB Token!!!").build();
			email = (String) jsonReqFB.opt("email");
			socialNetwork = "Facebook";
			socialId = (String) jsonReqFB.get("id"); 
			userName = (String) jsonReqFB.opt("username");
			name = (String) jsonReqFB.opt("name");
		} catch (JSONException e) {
			Log.error("", this, "createOrLoginFacebookUser", "Error parsing the JSON.", e); 
			return Response.status(Status.BAD_REQUEST).entity(new Error("Error reading FB info")).build();
		}		
				
		if(email == null && userName != null){
			email = userName+"@facebook.com";
		}
		if(email == null && userName == null){
			email = socialId+"@facebook.com";
			userName = name;
			
		}
		userId = usersMid.getUserIdUsingEmail(appId, email);
		//userSocialId = usersMid.socialUserExists(appId, socialId, socialNetwork);
		if (userId == null) {
			Log.debug("", this, "signup with FB", "********signup with FB ************ email: "+email);
			if (uriInfo == null) uriInfo=ui;
			Result res = usersMid.createSocialUserAndLogin(headerParams, appId, userName,email, socialId, socialNetwork, Metadata.getNewMetadata(location));
			Date endDate = Utils.getDate();
			Log.info(((User)res.getData()).getReturnToken(), this, "signup fb", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
			response = Response.status(Status.CREATED).entity(res).build();
		} else {
			Log.debug("", this, "signin with FB", "********signin with FB ************ email: "+email);
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
			if(validation){
				sessionMid.refreshSession(sessionToken, location, userAgent);
				Result data = usersMid.getUserInApp(appId, userId);
				User user = (User) data.getData();
				outUser.setBaseLocation(user.getBaseLocation());
				outUser.setBaseLocationOption(user.getBaseLocationOption());
				if(location!=null){
					if(user.getBaseLocationOption().equals("true")){
						lastLocation = user.getBaseLocation();
					}
					else
						lastLocation = location;
					usersMid.updateUserLocation(userId, appId, lastLocation, Metadata.getNewMetadata(lastLocation));
				}else
					lastLocation = user.getLocation();
				outUser.setLocation(lastLocation);
				outUser.set_id(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setEmail(email);
				outUser.setUserName(userName);
				Result res = new Result(outUser, data.getMetadata());
				response = Response.status(Status.OK).entity(res).build();
				Date endDate = Utils.getDate();
				Log.info(((User)res.getData()).getReturnToken(), this, "signin fb", "Start: " + Utils.printDate(startDate) + " - Finish:" + Utils.printDate(endDate) + " - Time:" + (endDate.getTime()-startDate.getTime()));
			}
		}
		return response;
	}
	
	private JSONObject getFBInfo(String fbToken) {
		JSONObject res = null;
		try{
			ClientConfig config = new DefaultClientConfig();
			Client client = Client.create(config);
			WebResource service = client.resource(UriBuilder.fromUri("https://graph.facebook.com/me?access_token="+fbToken).build());
			res = new JSONObject(service.accept(MediaType.APPLICATION_JSON).get(String.class));
		}
		catch (Exception e) {
			Log.error("", this, "FB Conn", "FB Conn", e);
		}
		return res;
	}
	

	/**
	 * Creates a user in the application. Necessary fields: "linkedin id".
	 * if the user already register only signin. if not signup
	 * 
	 * @param inputJsonObj
	 * @return
	 */
	@Path("/linkedin")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createOrLoginLinkedInUser(JSONObject inputJsonObj, @Context UriInfo ui, @Context HttpHeaders hh) {
		Response response = null;
		MultivaluedMap<String, String> headerParams = hh.getRequestHeaders();
		String email = null;
		String socialNetwork = null;
		String socialId = null;
		String userSocialId = null;
		String userName = null;
		String userAgent = null;
		String location = null;
		User outUser = new User();
		String appKey = null;
		String userId =null;
		try {
			location = headerParams.getFirst(Const.LOCATION);
		} catch (Exception e) { }
		try {
			userAgent = headerParams.getFirst(Const.USER_AGENT);
		} catch (Exception e) { }
		try {
			appKey = headerParams.getFirst(Application.APP_KEY);
		} catch (Exception e) { }
		if(appKey==null)
			return Response.status(Status.BAD_REQUEST).entity("App Key not found").build();
		if(!appsMid.authenticateApp(appId,appKey))
			return Response.status(Status.UNAUTHORIZED).entity("Wrong App Key").build();
		try {
			email = (String) inputJsonObj.get("email");
			socialNetwork = "LinkedIn";
			socialId = ((Integer) inputJsonObj.get("socialId")).toString(); 
			userName = (String) inputJsonObj.opt("userName");
			
		} catch (JSONException e) {
			Log.error("", this, "createOrLoginLinkedInUser", "Error parsing the JSON.", e); 
			return Response.status(Status.BAD_REQUEST).entity("Error reading JSON").build();
		}
		if (userName == null) {
			userName = email;
		}
		userId = usersMid.getUserIdUsingEmail(appId, email);
		userSocialId = usersMid.socialUserExists(appId, socialId, socialNetwork);
		
		if(userId!=null && userSocialId==null)
			response =  Response.status(302).entity("User "+userId+" with email: "+email+" already exists in app.").build();
		
		
		if (userId==null) {
			if (uriInfo == null) uriInfo=ui;
			Result res = usersMid.createSocialUserAndLogin(headerParams, appId, userName,email, socialId, socialNetwork, Metadata.getNewMetadata(location));
			response = Response.status(Status.CREATED).entity(res).build();
		} else {
			String sessionToken = Utils.getRandomString(Const.getIdLength());
			boolean validation = sessionMid.createSession(sessionToken, appId, userId, socialId);
			if(validation){
				sessionMid.refreshSession(sessionToken, location, userAgent);
				outUser.set_id(userId);
				outUser.setReturnToken(sessionToken);
				outUser.setEmail(email);
				outUser.setUserName(userName);
				Result res = new Result(outUser, null);
				response = Response.status(Status.OK).entity(res).build();
			}
		}
		return response;
	}
}
