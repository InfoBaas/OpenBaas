package infosistema.openbaas.dataModels;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;

import infosistema.openbaas.geolocation.GeoLocationOperations;
import infosistema.openbaas.geolocation.Geolocation;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;


//Developer notes:
/*
 * EmailsInApp is a great way to save time while creating/deleting users and so on.
 * keep a collection like this {"appId": "X" , "list" : [{"userId": "Y", "email":"Y@gmail.com"}, {"userId": "z", "email:
 * "z@gmail.com"}]}
 * and go over the list as you do your stuff, its faster. 
 * 
 * I didn't have enough time to do it.
 * 
 * 
 * 
 * 
 * 
 * 
 */
public class MongoDBDataModel implements DatabaseInterface {

	private MongoClient mongoClient;
	private DB db;
	private static final String STORAGEFOLDER = "storage";
	private static final String MEDIAFOLDER = "media";
	private static final String UsersColl = "users";
	private static final String AppsColl = "apps";
	private static final String VideoColl = "videos";
	private static final String AudioColl = "audio";
	private static final String ImageColl = "images";
	private static final String StorageColl = "storage";
//	private static final String SessionColl = "sessions";
//	private static final String adminColl = "admin";
//	private static final String UsersInactive = "users:inactive";
//	private static final String EmailsInAppColl = "emailsInApp";
//	private static final String EmailsApp = "app:emails";
	public static final String CONFIRMUSERSEMAIL = "confirmUsersEmail";
//	private static final int EXPIRETIME = 86400; // 24hours in seconds
	
	
	GeoLocationOperations geo;
	public MongoDBDataModel(String server, int port) {
		geo = new Geolocation();
		mongoClient = null;
		try {
			mongoClient = new MongoClient(server, port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		db = mongoClient.getDB("openbaas");
	}

	@Override
	public ArrayList<String> getAllUserIdsForApp(String appId,Integer pageNumber, Integer pageSize,String orderBy, String orderType) {
		Integer order = -1;
		if(orderType.equals("desc")) order = 1;
		DBCollection coll = db.getCollection(UsersColl);
		DBCursor cursor = coll.find().skip(pageNumber * pageSize).limit(pageSize).sort(new BasicDBObject(orderBy,order));
		ArrayList<String> userIds = new ArrayList<String>();
		while (cursor.hasNext()) {
			DBObject user = cursor.next();
			if (user.get("appId").equals(appId))
				userIds.add((String) user.get("_id"));
		}
		return new ArrayList<String>(userIds);
	}

	@Override
	public ArrayList<String> getAllAppIds(Integer pageNumber,Integer pageSize,String orderBy,String orderType) {
		DBCollection coll = db.getCollection(AppsColl);
		Integer order = -1;
		if(orderType.equals("desc")) order = 1;
		DBCursor cursor = coll.find().skip(pageNumber * pageSize).limit(pageSize).sort(new BasicDBObject(orderBy,order));
		ArrayList<String> appIds = new ArrayList<String>();
		while (cursor.hasNext()) {
			appIds.add((String) cursor.next().get("_id"));
		}
		java.util.Iterator<String> it = appIds.iterator();
		while (it.hasNext())
			System.out.println(it.next());
		return appIds;
	}

	@Override
	public Boolean createApp(String appId, String appName, String creationDate, Boolean userEmailConfirmation) {
		if (this.appExists(appId)) {
			return false;
		}
		DBCollection coll = db.getCollection(AppsColl);
		BasicDBObject app = new BasicDBObject("_id", appId)
				.append("alive", "true").append("creationDate", creationDate)
				.append("appName", appName)
				.append("confirmUsersEmail", userEmailConfirmation);
		

		coll.insert(app);
		return true;
	}

	@Override
	public Boolean deleteApp(String appId) {
		DBCollection coll = db.getCollection(AppsColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", appId);
		coll.remove(searchQuery);
		return true;
	}

	@Override
	public Boolean updateAllAppFields(String currentId, String alive, String newAppName, Boolean confirmUsersEmail) {
		DBCollection coll = db.getCollection(AppsColl);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", currentId);

		BasicDBObject newDocument = new BasicDBObject();
		newDocument.append("appName", newAppName);
		newDocument.append("alive", alive);
		newDocument.append("confirmUsersEmail", confirmUsersEmail);
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.append("$set", newDocument);

		coll.update(query, updateObj);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getApplication(String appId) {
		DBCollection coll = db.getCollection(AppsColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", appId);
		return coll.findOne(searchQuery).toMap();

	}

	@Override
	public Boolean appExists(String appId) {
		DBCollection table = db.getCollection(AppsColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", appId);
		DBCursor cursor = table.find(searchQuery);
		boolean appExists = false;
		if (cursor.hasNext())
			appExists = true;
		return appExists;
	}

	@Override
	public Boolean userExistsInApp(String appId, String email) {
		// check if the system generated id exists in the app
		DBCollection table = db.getCollection(UsersColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("appId", appId);
		searchQuery.put("email", email);
		DBCursor cursor = table.find(searchQuery);

		if (cursor.hasNext()) {
			System.out.println(cursor.next());
			return true;
		}
		return false;

		// check if the email does not exist in the app (emails are unique in
		// each app)
		// DBCollection emailsApp = db.getCollection(EmailsInAppColl);
		// BasicDBObject searchEmail = new BasicDBObject();
		// searchQuery.put("_id", appId);
		// DBCursor cursorEmail = emailsApp.find(searchEmail);
		// while (cursorEmail.hasNext()) {
		// BasicDBObject result = (BasicDBObject) cursorEmail.next();
		//
		// BasicDBList list = (BasicDBList) result.get("list");
		// System.out.println(list.toString());
		// BasicDBObject[] emailsArray = list.toArray(new BasicDBObject[0]);
		// for(BasicDBObject dbObj : emailsArray) {
		// if(dbObj.get("email").equals(email))
		// return true;
		// }
		// }
	}

	@Override
	public Boolean createUserWithFlag(String appId, String userId, String userName,
			String email, byte[] salt, byte[] hash, String creationDate, String userFile)
			throws UnsupportedEncodingException {
		DBCollection coll = db.getCollection(UsersColl);
		BasicDBObject user = new BasicDBObject("_id", userId)
				.append("alive", "true").append("appId", appId)
				.append("userName", userName).append("email", email)
				.append("hash", new String(hash, "ISO-8859-1"))
				.append("salt", new String(salt, "ISO-8859-1"))
				.append("userFile", userFile)
				.append("creationDate", creationDate);
		coll.insert(user);
		return true;
	}
	@Override
	public Boolean createUserWithoutFlag(String appId, String userId, String userName,
			String email, byte[] salt, byte[] hash, String creationDate)
			throws UnsupportedEncodingException {
		DBCollection coll = db.getCollection(UsersColl);
		BasicDBObject user = new BasicDBObject("_id", userId)
				.append("alive", "true").append("appId", appId)
				.append("userName", userName).append("email", email)
				.append("hash", new String(hash, "ISO-8859-1"))
				.append("salt", new String(salt, "ISO-8859-1"))
				.append("creationDate", creationDate);
		coll.insert(user);
		return true;
	}
	@Override
	public void updateUser(String appId, String userId, String email,
			byte[] hash, byte[] salt, String alive)
			throws UnsupportedEncodingException {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", userId);
		query.put("appId", appId);
		BasicDBObject updateUser = new BasicDBObject();
		updateUser.append("$set", new BasicDBObject().append("email", email).append("hash", new String(
				hash, "ISO-8859-1")).append("salt", new String(
				salt, "ISO-8859-1")).append("alive", alive));
		
//		updateUser.append("$set", new BasicDBObject().append("email", email));
//		updateUser.append("$set", new BasicDBObject().append("hash", new String(
//				hash, "ISO-8859-1")));
//		updateUser.append("$set", new BasicDBObject().append("salt", new String(
//				salt, "ISO-8859-1")));
//		updateUser.append("$set", new BasicDBObject().append("alive", alive));
		System.out.println("*******************");
		System.out.println(updateUser.toString());
		users.update(query, updateUser);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getUser(String appId, String userId) {
		DBCollection coll = db.getCollection(UsersColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", userId);
		searchQuery.put("appId", appId);
		return coll.findOne(searchQuery).toMap();
	}

	@Override
	public Boolean deleteUser(String appId, String userId) {
		// set user alive field to false
		DBCollection user = db.getCollection(UsersColl);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", userId);
		query.put("appId", appId);
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.put("alive", "false");
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.put("$set", newDocument);
		user.update(query, updateObj);
		// Add to inactive users list
		DBCollection inactive = null;
		try {
			inactive = db.getCollection("inactive");
			BasicDBObject inactiveUsers = new BasicDBObject("_id",
					userId);
			inactive.insert(inactiveUsers);
		} catch (MongoException.DuplicateKey e) {
			System.out.println("Exception: Existing key USERSINACTIVE");
		}
		BasicDBObject queryList = new BasicDBObject();
		queryList.put("_id", "USERSINACTIVE");
		BasicDBObject updateCommand = new BasicDBObject();
		updateCommand.put("$push", new BasicDBObject("list", userId));
		inactive.update(queryList, updateCommand, true, true);
		return true;
	}

	@Override
	public ArrayList<String> getAllAudioIds(String appId,Integer pageNumber,Integer pageSize,String orderBy, String orderType) {
		DBCollection coll = db.getCollection(AudioColl);
		ArrayList<String> audioIds = new ArrayList<String>();
		
		Integer order = -1;
		if(orderType.equals("desc")) order = 1;
		DBCursor cursor = coll.find().skip(pageNumber * pageSize).limit(pageSize).sort(new BasicDBObject(orderBy,order));
		
		while (cursor.hasNext()) {
			audioIds.add((String) cursor.next().get("_id"));
		}
		return audioIds;
	}

	@Override
	public Boolean audioExistsInApp(String appId, String audioId) {
		DBCollection coll = db.getCollection(AudioColl);
		BasicDBObject query = new BasicDBObject();
		query.append("appId", appId);
		query.append("_id", audioId);
		DBCursor cursor = coll.find(query);
		boolean audioExistsInApp = false;
		if (cursor.hasNext())
			audioExistsInApp = true;
		return audioExistsInApp;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getAudioInApp(String appId, String audioId) {
		DBCollection coll = db.getCollection(AudioColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", audioId);
		searchQuery.put("appId", appId);
		return  coll.findOne(searchQuery).toMap();
	}

	@Override
	public void deleteAudioInApp(String appId, String audioId) {
		DBCollection coll = db.getCollection(AudioColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("appId", appId);
		searchQuery.put("_id", audioId);
		coll.remove(searchQuery);
	}

	@Override
	public Boolean createAudioInApp(String appId, String audioId,
			String directory, String type, String size, String bitRate,
			String creationDate, String fileName, String location) {
		DBCollection coll = db.getCollection(AudioColl);
		BasicDBObject audio = null;
		if (location != null) {
			audio = new BasicDBObject().append("_id", audioId)
					.append("appId", appId).append("dir", directory)
					.append("type", type).append("size", size)
					.append("bitRate", bitRate)
					.append("creationDate", creationDate)
					.append("fileName", fileName).append("location", location);
			String[] array = location.split(":");
			geo.insertObjectInGrid(Double.parseDouble(array[0]),
					Double.parseDouble(array[1]), type, audioId);
		} else {
			audio = new BasicDBObject().append("_id", audioId)
					.append("appId", appId).append("dir", directory)
					.append("type", type).append("size", size)
					.append("bitRate", bitRate)
					.append("creationDate", creationDate)
					.append("fileName", fileName);
		}
		coll.insert(audio);
		return true;
	}

	@Override
	public ArrayList<String> getAllVideoIdsInApp(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		DBCollection coll = db.getCollection(VideoColl);
		ArrayList<String> videoIds = new ArrayList<String>();
		
		Integer order = -1;
		if(orderType.equals("desc")) order = 1;
		DBCursor cursor = coll.find().skip(pageNumber * pageSize).limit(pageSize).sort(new BasicDBObject(orderBy,order));
	
		while (cursor.hasNext()) {
			videoIds.add((String) cursor.next().get("_id"));
		}
		return videoIds;
	}

	@Override
	public Boolean imageExistsInApp(String appId, String imageId) {
		DBCollection coll = db.getCollection(ImageColl);
		BasicDBObject query = new BasicDBObject();
		query.append("appId", appId);
		query.append("_id", imageId);
		DBCursor cursor = coll.find(query);
		boolean imageExistsInApp = false;
		if (cursor.hasNext())
			imageExistsInApp = true;
		return imageExistsInApp;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getImageInApp(String appId, String imageId) {
		DBCollection coll = db.getCollection(ImageColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("_id", imageId);
		searchQuery.put("appId", appId);
		return coll.findOne(searchQuery).toMap();
	}

	@Override
	public Boolean createImageInApp(String appId, String imageId,
			String directory, String type, String size, String pixelsSize,
			String creationDate, String fileName, String location) {
		DBCollection coll = db.getCollection(ImageColl);
		BasicDBObject image = null;
		if (location != null) {
			image = new BasicDBObject().append("_id", imageId)
					.append("appId", appId).append("dir", directory)
					.append("type", type).append("size", size)
					.append("pixelsSize", pixelsSize)
					.append("creationDate", creationDate)
					.append("fileName", fileName).append("location", location);
			String[] array = location.split(":");
			geo.insertObjectInGrid(Double.parseDouble(array[0]),Double.parseDouble(array[1]), type, imageId);
		} else
			image = new BasicDBObject().append("_id", imageId)
					.append("appId", appId).append("dir", directory)
					.append("type", type).append("size", size)
					.append("pixelsSize", pixelsSize)
					.append("creationDate", creationDate)
					.append("fileName", fileName);
		coll.insert(image);
		return true;
	}

	@Override
	public Boolean createVideoInApp(String appId, String videoId,
			String directory, String type, String size, String resolution,
			String creationDate, String fileName, String location) {
		DBCollection coll = db.getCollection(VideoColl);
		BasicDBObject video = null;
		if (location != null) {
			video = new BasicDBObject().append("_id", videoId)
					.append("appId", appId).append("dir", directory)
					.append("type", type).append("size", size)
					.append("resolution", resolution)
					.append("creationDate", creationDate)
					.append("fileName", fileName).append("location", location);
			String[] array = location.split(":");
			geo.insertObjectInGrid(Double.parseDouble(array[0]),
					Double.parseDouble(array[1]), type, videoId);
		} else
			video = new BasicDBObject().append("_id", videoId)
					.append("appId", appId).append("dir", directory)
					.append("type", type).append("size", size)
					.append("resolution", resolution)
					.append("creationDate", creationDate)
					.append("fileName", fileName);
		coll.insert(video);
		return true;
	}

	@Override
	public Boolean videoExistsInApp(String appId, String videoId) {
		DBCollection coll = db.getCollection(VideoColl);
		BasicDBObject query = new BasicDBObject();
		query.append("appId", appId);
		query.append("_id", videoId);
		DBCursor cursor = coll.find(query);
		boolean videoExistsInApp = false;
		if (cursor.hasNext())
			videoExistsInApp = true;
		return videoExistsInApp;
	}

	// @Override
	// public Set<String> getAllStorageIds(String appId) {
	// DBCollection coll = db.getCollection(StorageColl);
	// Set<String> storageIds = new HashSet<String>();
	// DBCursor cursor = coll.find();
	// while (cursor.hasNext()) {
	// storageIds.add((String) cursor.next().get("_id"));
	// }
	// return storageIds;
	// }

	@Override
	public Boolean createStorageInApp(String appId, String storageId,
			String directory, String type, String size, String creationDate,
			String fileName, String location) {
		DBCollection coll = db.getCollection(StorageColl);
		BasicDBObject storage = null;
		if (location != null){
			storage = new BasicDBObject().append("_id", storageId)
					.append("appId", appId).append("dir", directory)
					.append("type", type).append("size", size)
					.append("creationDate", creationDate)
					.append("fileName", fileName).append("location", location);
			String[] array = location.split(":");
			geo.insertObjectInGrid(Double.parseDouble(array[0]),
					Double.parseDouble(array[1]), type, storageId);
		}
		else
			storage = new BasicDBObject().append("_id", storageId)
					.append("appId", appId).append("dir", directory)
					.append("type", type).append("size", size)
					.append("creationDate", creationDate)
					.append("fileName", fileName);
		coll.insert(storage);
		return true;
	}

	@Override
	public Boolean deleteVideoInApp(String appId, String videoId) {
		DBCollection coll = db.getCollection(VideoColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("appId", appId);
		searchQuery.append("_id", videoId);
		coll.remove(searchQuery);
		return true;
	}

	@Override
	public String getFileDirectory(String appId, String id, String folderType,
			String requestType) {
		DBCollection coll = null;
		String type = null;
		// Switch performance is not guaranteed
		if(folderType.equalsIgnoreCase(STORAGEFOLDER)){
			coll = db.getCollection(StorageColl);
		}else if(folderType.equalsIgnoreCase(MEDIAFOLDER)){
			if (requestType.equals("audio")) {
				coll = db.getCollection(AudioColl);
			} else if (requestType.equals("video")) {
				coll = db.getCollection(VideoColl);
			} else if (requestType.equals("images")) {
				coll = db.getCollection(ImageColl);
			}
		}
		
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("appId", appId);
		searchQuery.append("_id", type + id);
		DBCursor cursor = coll.find(searchQuery);
		String dir = null;
		if (cursor.hasNext()) {
			DBObject object = cursor.next();
			dir = (String) object.get("dir");
		}
		return dir;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getVideoInApp(String appId, String videoId) {
		DBCollection coll = db.getCollection(VideoColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", videoId);
		searchQuery.append("appId", appId);
		return coll.findOne(searchQuery).toMap();
	}

	@Override
	public Boolean identifierInUseByUserInApp(String appId, String userId) {
		// check if the system generated exist exists in the app
		DBCollection table = db.getCollection(UsersColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", userId);
		searchQuery.append("appId", appId);
		DBCursor cursor = table.find(searchQuery);
		boolean userExists = false;
		if (cursor.hasNext())
			userExists = true;
		return userExists;
	}

	@Override
	public ArrayList<String> getAllImageIdsInApp(String appId,Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		DBCollection coll = db.getCollection(ImageColl);
		ArrayList<String> imageIds = new ArrayList<String>();
		//DBCursor cursor = coll.find();
		Integer order = -1;
		if(orderType.equals("desc")) order = 1;
		DBCursor cursor = coll.find().skip(pageNumber * pageSize).limit(pageSize).sort(new BasicDBObject(orderBy,order));
		
		while (cursor.hasNext()) {
			imageIds.add((String) cursor.next().get("_id"));
		}
		return new ArrayList<String>(imageIds);
	}

	@Override
	public String convertAppIdToAppName(String appId) {
		DBCollection apps = db.getCollection(AppsColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", appId);
		DBCursor cursor = apps.find(searchQuery);
		String appName = null;
		while (cursor.hasNext()) {
			DBObject temp = cursor.next();
			if (temp.get("_id").equals(appId)) {
				appName = (String) temp.get("appName");
				break;
			}
		}
		return appName;
	}

	@Override
	public String getUserNameUsingUserId(String appId, String userId) {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", userId);
		searchQuery.append("appId", appId);
		DBCursor cursor = users.find(searchQuery);
		String userName = null;
		if (cursor.hasNext()) {
			DBObject temp = cursor.next();
			userName = (String) temp.get("userName");
		}
		return userName;
	}

	@Override
	public String getEmailUsingUserId(String appId, String userId) {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", userId);
		searchQuery.append("appId", appId);
		DBCursor cursor = users.find(searchQuery);
		String email = null;
		if (cursor.hasNext()) {
			DBObject temp = cursor.next();
			email = (String) temp.get("email");
		}
		return email;
	}

	@Override
	public String getEmailUsingUserName(String appId, String userName) {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("userName", userName);
		searchQuery.append("appId", appId);
		DBCursor cursor = users.find(searchQuery);
		String email = null;
		if (cursor.hasNext()) {
			DBObject temp = cursor.next();
			email = (String) temp.get("email");
		}
		return email;
	}


	public String getImageLocationUsingImageId(String appId, String imageId) {
		DBCollection users = db.getCollection(ImageColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", imageId);
		searchQuery.append("appId", appId);
		DBCursor cursor = users.find(searchQuery);
		String location = null;
		if (cursor.hasNext()) {
			DBObject temp = cursor.next();
			location = (String) temp.get("location");
		}
		return location;
	}
	
	public String getAudioLocationUsingAudioId(String appId, String audioId) {
		DBCollection users = db.getCollection(AudioColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", audioId);
		searchQuery.append("appId", appId);
		DBCursor cursor = users.find(searchQuery);
		String location = null;
		if (cursor.hasNext()) {
			DBObject temp = cursor.next();
			location = (String) temp.get("location");
		}
		return location;
	}

	public String getVideoLocationUsingVideoId(String appId, String videoId) {
		DBCollection users = db.getCollection(VideoColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", videoId);
		searchQuery.append("appId", appId);
		DBCursor cursor = users.find(searchQuery);
		String location = null;
		if (cursor.hasNext()) {
			DBObject temp = cursor.next();
			location = (String) temp.get("location");
		}
		return location;
	}
	
	public String getUserIdUsingUserName(String appId, String userName) {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("userName", userName);
		searchQuery.append("appId", appId);
		DBCursor cursor = users.find(searchQuery);
		String userId = null;
		if (cursor.hasNext()) {
			DBObject temp = cursor.next();
			userId = (String) temp.get("_id");
		}
		return userId;
	}
	
	@Override
	public String getUserIdUsingEmail(String appId, String email) {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("email", email);
		searchQuery.append("appId", appId);
		DBCursor cursor = users.find(searchQuery);
		String userId = null;
		if (cursor.hasNext()) {
			DBObject temp = cursor.next();
			userId = (String) temp.get("_id");
		}
		return userId;
	}

	@Override
	public ArrayList<String> getAllStorageIdsInApp(String appId,Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		Integer order = -1;
		if(orderType.equals("desc")) order = 1;
		DBCollection coll = db.getCollection(StorageColl);
		DBCursor cursor = coll.find().skip(pageNumber * pageSize).limit(pageSize).sort(new BasicDBObject(orderBy,order));
		ArrayList<String> storageIds = new ArrayList<String>();
		while (cursor.hasNext()) {
			DBObject storage = cursor.next();
			if (storage.get("appId").equals(appId))
				storageIds.add((String) storage.get("_id"));
		}
		return storageIds;
	}

	@Override
	public void reviveApp(String appId) {
		DBCollection coll = db.getCollection(AppsColl);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", appId);
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.append("alive", "true");
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.append("$set", newDocument);
		coll.update(query, updateObj);
	}

	@Override
	public Boolean updateAppName(String appId, String newAppName) {
		DBCollection coll = db.getCollection(AppsColl);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", appId);
		BasicDBObject newDocument = new BasicDBObject();
		newDocument.append("appName", newAppName);
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.append("$set", newDocument);
		coll.update(query, updateObj);
		return true;
	}

	@Override
	public void updateUser(String appId, String userId, String email) {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject query = new BasicDBObject();
		query.append("_id", userId);
		query.append("appId", appId);
		BasicDBObject updateUser = new BasicDBObject();
		updateUser.append("$set", new BasicDBObject().append("email", email));
		users.update(query, updateUser);
	}
	@Override
	public Boolean updateUserPassword(String appId, String userId, byte [] hash, byte [] salt) throws UnsupportedEncodingException{
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject query = new BasicDBObject();
		query.append("_id", userId);
		query.append("appId", appId);
		BasicDBObject updateUser = new BasicDBObject();
		updateUser.append("$set", new BasicDBObject().append("hash", new String(
				hash, "ISO-8859-1")));
		updateUser.append("$set", new BasicDBObject().append("salt", new String(
				salt, "ISO-8859-1")));
		users.update(query, updateUser);
		return true;
	}

	@Override
	public void updateUser(String appId, String userId, String email,
			byte[] hash, byte[] salt) throws UnsupportedEncodingException {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject query = new BasicDBObject();
		query.append("_id", userId);
		query.append("appId", appId);
		BasicDBObject updateUser = new BasicDBObject();
		updateUser.append("$set", new BasicDBObject().append("email", email));
		updateUser.append("$set", new BasicDBObject().append("hash", new String(
				hash, "ISO-8859-1")));
		updateUser.append("$set", new BasicDBObject().append("salt", new String(
				salt, "ISO-8859-1")));
		users.update(query, updateUser);
	}
	
	@Override
	public ArrayList<String> getAllMediaIds(String appId, Integer pageNumber, Integer pageSize, String orderBy, String orderType) {
		//TODO JM Como tratar paginação aqui???
		// images
		DBCollection coll = db.getCollection(ImageColl);
		ArrayList<String> mediaIds = new ArrayList<String>();
		DBCursor cursor = coll.find();
		while (cursor.hasNext()) {
			mediaIds.add((String) cursor.next().get("_id"));
		}
		// audio
		coll = db.getCollection(AudioColl);
		cursor = coll.find();
		while (cursor.hasNext()) {
			mediaIds.add((String) cursor.next().get("_id"));
		}
		// video
		coll = db.getCollection(VideoColl);
		cursor = coll.find();
		while (cursor.hasNext()) {
			mediaIds.add((String) cursor.next().get("_id"));
		}
		return mediaIds;
	}

	@Override
	public void updateUserLocationAndDate(String userId, String appId,
			String sessionToken, String location, String date) {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject query = new BasicDBObject();
		query.append("_id", userId);
		query.append("appId", appId);
		BasicDBObject updateUser = new BasicDBObject();
		updateUser
				.append("$set", new BasicDBObject().append("location", location));
		updateUser.append("$set", new BasicDBObject().append("date", date));
		users.update(query, updateUser);

	}

	@Override
	public Boolean storageExistsInApp(String appId, String storageId) {
		DBCollection coll = db.getCollection(StorageColl);
		BasicDBObject query = new BasicDBObject();
		query.append("appId", appId);
		query.append("_id", storageId);
		DBCursor cursor = coll.find(query);
		boolean storageExistsInApp = false;
		if (cursor.hasNext())
			storageExistsInApp = true;
		return storageExistsInApp;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, String> getStorageInApp(String appId, String storageId) {
		DBCollection coll = db.getCollection(StorageColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", storageId);
		searchQuery.append("appId", appId);
		return coll.findOne(searchQuery).toMap();
	}

	@Override
	public Boolean deleteStorageInApp(String appId, String storageId){
		DBCollection coll = db.getCollection(StorageColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("appId", appId);
		searchQuery.append("_id", storageId);
		coll.remove(searchQuery);
		return true;
	}

	@Override
	public Boolean confirmUsersEmail(String appId) {
		DBCollection coll = db.getCollection(AppsColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("_id", appId);
		@SuppressWarnings("unchecked")
		Map<String, Object> temp = coll.findOne(searchQuery).toMap();
		for(Map.Entry<String, Object> entry : temp.entrySet()){
			if(entry.getKey().equalsIgnoreCase(CONFIRMUSERSEMAIL))
				return ((Boolean)(entry.getValue()));
		}
		return false;
	}

	@Override
	public Boolean deleteImageInApp(String appId, String imageId) {
		DBCollection coll = db.getCollection(ImageColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("appId", appId);
		searchQuery.put("_id", imageId);
		coll.remove(searchQuery);
		return true;
	}

	@Override
	public Boolean createUserWithFlagWithEmailConfirmation(String appId,
			String userId, String userName, String email, byte[] salt,
			byte[] hash, String creationDate, String userFile, Boolean emailConfirmed) throws UnsupportedEncodingException {
		DBCollection coll = db.getCollection(UsersColl);
		BasicDBObject user = new BasicDBObject("_id", userId)
				.append("alive", "true").append("appId", appId)
				.append("userName", userName).append("email", email)
				.append("hash", new String(hash, "ISO-8859-1"))
				.append("salt", new String(salt, "ISO-8859-1"))
				.append("userFile", userFile)
				.append("emailConfirmed", emailConfirmed)
				.append("creationDate", creationDate);
		coll.insert(user);
		return true;
	}

	@Override
	public Boolean createUserWithoutFlagWithEmailConfirmation(String appId,
			String userId, String userName, String email, byte[] salt,
			byte[] hash, String creationDate, Boolean emailConfirmed)
			throws UnsupportedEncodingException {
		DBCollection coll = db.getCollection(UsersColl);
		BasicDBObject user = new BasicDBObject("_id", userId)
				.append("alive", "true").append("appId", appId)
				.append("userName", userName).append("email", email)
				.append("hash", new String(hash, "ISO-8859-1"))
				.append("salt", new String(salt, "ISO-8859-1"))
				.append("emailConfirmed", emailConfirmed)
				.append("creationDate", creationDate);
		coll.insert(user);
		return true;
	}

	@Override
	public Boolean confirmUserEmail(String appId, String userId) {
		try{
			DBCollection users = db.getCollection(UsersColl);
			BasicDBObject query = new BasicDBObject();
			query.put("_id", userId);
			query.put("appId", appId);
			
			BasicDBObject updateUser = new BasicDBObject();
			updateUser.append("$set", new BasicDBObject().append("emailConfirmed", true));
			/*BasicDBObject updateObj = new BasicDBObject();
			updateObj.append("$set", updateUser);*/
			//{ "$set" : { "$set" : { "emailConfirmed" : true}}}
			users.update(query, updateUser);
		}catch(Exception e){
			System.err.println(e.toString());
		}		
		return true;
	}

	@Override
	public Boolean userEmailIsConfirmed(String appId, String userId) {
		DBCollection users = db.getCollection(UsersColl);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", userId);
		query.put("appId", appId);
		DBCursor cursor = users.find(query);
		boolean emailIsConfirmed = false;
		if(cursor.hasNext()){
			DBObject temp = cursor.next();
			if(temp.containsField("emailConfirmed"))
				emailIsConfirmed = (Boolean) temp.get("emailConfirmed");
		}
		return emailIsConfirmed;
	}

	@Override
	public Boolean updateConfirmUsersEmailOption(String appId,	Boolean confirmUsersEmail) {
		DBCollection coll = db.getCollection(AppsColl);
		BasicDBObject query = new BasicDBObject();
		query.put("_id", appId);

		BasicDBObject newDocument = new BasicDBObject();
		newDocument.append("confirmUsersEmail", confirmUsersEmail);
		
		BasicDBObject updateObj = new BasicDBObject();
		updateObj.append("$set", newDocument);
		coll.update(query, updateObj);
		return true;
	}

	@Override
	public Integer countAllImagesInApp(String appId) {
		DBCollection images = db.getCollection(ImageColl);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.append("appId", appId);
		long number = images.count(searchQuery);
		return (int) number;
	}

	

	
	
}
