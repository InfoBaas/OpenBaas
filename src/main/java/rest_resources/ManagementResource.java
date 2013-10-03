package rest_resources;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import redis.clients.jedis.Jedis;

/*This class is yet to be used for anything, the ideia is that you could perform certain openbaas
 * administration thru here (emergency operations like restarting a database or backing it up).
 * Right now it isn't being used.
*/
@Path("/management")
public class ManagementResource {
	
	private static final int RedisSessionsPORT = 6381;
	Jedis jedis;
	private final static String server = "localhost";
	
	public ManagementResource(){
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
