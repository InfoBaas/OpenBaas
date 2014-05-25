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
