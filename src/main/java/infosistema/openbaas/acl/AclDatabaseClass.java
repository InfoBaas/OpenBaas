package infosistema.openbaas.acl;

import java.net.UnknownHostException;

import utils.Const;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

//test
public class AclDatabaseClass implements AclInterface{

	private MongoClient mongoClient;
	private DB db;
	//public static final String SERVER = "localhost";
	//public static final int PORT = 27017;
	private static final String ACLTREE = "acl";
	
	/*_id is unique in all collections of MongoDB. For this reason the tree design is potentially flawed.
	 * 
	 * Right now we have this:
	 * 
	 * -------ACL------ (_id : acl)
	 * --------|-------
     * ------Apps------ (_id : acl:apps)
     * -----/----\-----
     * ---3222--1223--- (_id : acl:apps:3222)(_id : acl:apps:1223)
     * --/---------\---
     * users------users(_id : acl:apps:3222:users)(_id : acl:apps:1223:users)
	 * 
	*/
	public AclDatabaseClass(){
		try {
			mongoClient = new MongoClient(Const.SERVER, Const.MONGO_PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		db = mongoClient.getDB("openbaas");
		//DBCollection coll = db.getCollection(ACLTREE);
	}
	public boolean checkIfExists(String id, String pathAncestors, String parent, String userId){
		boolean result = false;
		DBCollection coll = db.getCollection(ACLTREE);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", pathAncestors);
		DBObject temp = coll.findOne(searchQuery);
		if(temp != null)
			result = true;
		return result;
	}
	public String readPermissions(String id, String pathAncestors,
			String parent, String userId) {
		DBCollection coll = db.getCollection(ACLTREE);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", pathAncestors);
		DBObject temp = coll.findOne(searchQuery);
		return (String) temp.get("permissions");
	}
	@Override
	public boolean writePermissions(String path, String permissions, String parent, String ancestors) {
		DBCollection coll = db.getCollection(ACLTREE);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", path);
		
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.append("_id", path).append("permissions", permissions).append("parent", parent)
		.append("ancestors", ancestors);
		coll.update(searchQuery, newDocument);
		return false;
	}
}
