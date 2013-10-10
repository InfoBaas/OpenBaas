package infosistema.openbaas.rest;

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
	public AppsResource appsResource() {
		try {
			return new AppsResource();
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response
					.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}
	@Path("/{pathId:.+}/~ACL")
	public AclResource aclResource(@PathParam("pathId") List<PathSegment> path) {
		try {
			return new AclResource(path);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response
					.status(Status.BAD_REQUEST).entity("Parse error").build());
		}
	}
}
