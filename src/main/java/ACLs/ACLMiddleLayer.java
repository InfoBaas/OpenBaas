package ACLs;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.PathSegment;

import org.codehaus.jettison.json.JSONArray;

/**
 * ACLs have two different views, the user one that goes like this:
 * -------------URL----------
 * --------------|-----------
 * ------------~ACL----------
 * ------------/   \---------
 * ---------User1--User2-----
 * --------------------------
 * 
 * And the implementation tree:
 * -------------ACL----------
 * --------------|-----------
 * ------------Apps----------
 * -----------/----\---------
 * --------User1--User2------
 * --------------------------
 * 
 * This class makes the conversion from one to the other
 */
public class ACLMiddleLayer {
	AclInterface acl;
	
	public ACLMiddleLayer(){
		acl = new AclDatabaseClass();
	}

	public boolean writePermissions(List<PathSegment> path, String permissions, String userId) {
		String pathAncestors = "acl:";
		
		JSONArray ancestors = new JSONArray();
		ancestors.put("acl");
		for(int i = 0; i < path.size(); i++){
			String pathTemp = path.get(i).getPath();
			if(i != path.size()-1){
				pathAncestors += path.get(i).getPath() + ":";
				ancestors.put(path.get(i).getPath());
			}else{
				pathAncestors += path.get(i).getPath();
			}
		}
		pathAncestors += ":~acl:"+userId;
		ancestors.put("~acl");
		
		String parent = "~acl";
		String r = null;
		acl.writePermissions(pathAncestors, permissions, parent, ancestors.toString());
		return true;
	}

	public String getPermissions(List<PathSegment> path, String userId){
		String permissions = null;
		int i = path.size()-1;
		char[] building = {'-','-','-','-'};
		while(permissions == null && i >= 0){ //goes down to the father until it finds it finds permissions
			permissions = readPermissions(path.get(i).getPath(), path.subList(0, i+1), 
					path.get(i).getPath(), userId);
 			if(permissions != null){
				boolean b = true;
				for(int j = 0; j <= 3; j++){
					if(building[j] == '-' && !(permissions.charAt(j) == '-')){
						building[j] = permissions.charAt(j);
						b = false;
					}else{ //buscar permissoes do pai
						building[j] = getPermissionAtCharAt(path, userId, j);
					}
					if(b)
						break;
				}
			}
 			i--;

		}
		return new String(building);
	}
	public char getPermissionAtCharAt(List<PathSegment> path, String userId, int charAt){
		int i = path.size()-2; //1 level above
		String permissions = readPermissions(path.get(i).getPath(), path.subList(0, i+1), 
				path.get(i).getPath(), userId);
		return permissions.charAt(charAt);
		
	}

	private String readPermissions(String id, List<PathSegment> path,
			String parent, String userId) {
		String pathAncestors = "acl:";
		for(int i = 0; i < path.size(); i++){
			String pathTemp = path.get(i).getPath();
			if(i != path.size()-1){
				pathAncestors += path.get(i).getPath() + ":";
			}else{
				pathAncestors += path.get(i).getPath();
			}
		}
		pathAncestors += ":~acl:"+userId;
		String r = null;
		if(acl.checkIfExists(id, pathAncestors, parent, userId)){
			r = acl.readPermissions(id, pathAncestors, parent, userId);
		}
		return r;
	}
}
