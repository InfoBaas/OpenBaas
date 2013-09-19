package ACLs;
//pp
import java.util.List;

import javax.ws.rs.core.PathSegment;

public interface AclInterface {

	public String readPermissions(String id, String pathAncestors,
			String parent, String userId);
	public boolean checkIfExists(String id, String pathAncestors, String parent, String userId);
	public boolean writePermissions(String path, String permissions, String parent, String ancestors);
}
