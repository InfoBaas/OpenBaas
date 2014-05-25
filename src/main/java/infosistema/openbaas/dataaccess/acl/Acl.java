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
