package rest_resources;

import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import redis.clients.jedis.Jedis;
import resourceModelLayer.AppsMiddleLayer;


/*This class is yet to be used for anything, the ideia is that you could perform certain openbaas
 * administration thru here (emergency operations like restarting a database or backing it up).
 * Right now it isn't being used.
*/
@Path("/management")
public class ManagementResource {
	
	private AppsMiddleLayer appsMid;
	private static byte [] adminSalt ; //you will be in the database
	private static byte [] adminHash ;
	private static final int RedisSessionsPORT = 6381;
	Jedis jedis;
	private final static String server = "localhost";
	
	public ManagementResource(){
		appsMid = new AppsMiddleLayer();
	}
	/**
	 * Welcome reply.
	 * @return
	 */
	@GET
	public Response test(){
		//list com 
		//sadd "column:" + i + ":rows"
		jedis = new Jedis(server, RedisSessionsPORT);
		System.out.println("---------------------TESTING--------------------------");
		for(long j = 0; j < 1000000; j++){
			jedis.lpush("columns", new String(getRandomString(32)));
			
		}
		return  Response.status(Status.OK).entity("Invalid Session Token.").build();
	}
	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}
}
