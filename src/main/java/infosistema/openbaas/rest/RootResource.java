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

import infosistema.openbaas.utils.Log;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


@Path("/")
public class RootResource {

	@Path("/apps")
	public AppResource appsResource() {
		try {
			return new AppResource();
		} catch (IllegalArgumentException e) {
			Log.error("", this, "appsResource", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

	@Path("/{pathId:.+}/~ACL")
	public AclResource aclResource(@PathParam("pathId") List<PathSegment> path) {
		try {
			return new AclResource(path);
		} catch (IllegalArgumentException e) {
			Log.error("", this, "aclResource", "Illegal Arguments.", e); 
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}

}
