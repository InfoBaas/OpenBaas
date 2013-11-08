package infosistema.openbaas.rest;

import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Utils;

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
	
	Jedis jedis;
	
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
		jedis = new Jedis(Const.getRedisSessionServer(), Const.getRedisSessionPort());
		for(long j = 0; j < 1000000; j++){
			jedis.lpush("columns", Utils.getRandomString(32));
			
		}
		return  Response.status(Status.OK).entity("Invalid Session Token.").build();
	}
}
