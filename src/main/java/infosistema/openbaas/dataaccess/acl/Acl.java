package infosistema.openbaas.dataaccess.acl;

import infosistema.openbaas.utils.Const;
import infosistema.openbaas.utils.Log;

import java.net.UnknownHostException;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

//test
public class Acl {

	private MongoClient mongoClient;
	private DB db;
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
	public Acl(){
		try {
			mongoClient = new MongoClient(Const.getMongoServer(), Const.getMongoPort());
		} catch (UnknownHostException e) {
			Log.error("", this, "Acl", "Unknown Host.#", e); 
		}
		
		db = mongoClient.getDB("openbaas");
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
