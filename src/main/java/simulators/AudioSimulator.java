package simulators;

import java.util.Map;
import java.util.UUID;

import rest_Models.MP3;

public class AudioSimulator {
	static final int idGenerator = 8;
	public AudioSimulator(Map<String, MP3> audio){
//		String id = getRandomString(idGenerator);
//		audio.put(id, new MP3(id, "temp", 2, 96, "mp3"));
//		id = getRandomString(idGenerator);
//		audio.put(id, new MP3(id, "temp", 3, 32, "mp3"));
//		id = getRandomString(idGenerator);
//		audio.put(id, new MP3(id, "temp", 1, 128, "mp3"));
	}
	private String getRandomString(int length) {
		return (String) UUID.randomUUID().toString().subSequence(0, length);
	}
}
