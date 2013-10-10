package infosistema.openbaas.middleLayer;

public class MiddleLayerFactory {

	public static AclMiddleLayer getAclMiddleLayer() {
		return AclMiddleLayer.getInstance();
	}

	public static AppsMiddleLayer getAppsMiddleLayer() {
		return AppsMiddleLayer.getInstance();
	}
	
	public static AudioMiddleLayer getAudioMiddleLayer() {
		return AudioMiddleLayer.getInstance();
	}
	
	public static DocumentMiddleLayer getDocumentMiddleLayer() {
		return DocumentMiddleLayer.getInstance();
	}
	
	public static ImageMiddleLayer getImageMiddleLayer() {
		return ImageMiddleLayer.getInstance();
	}

	public static SessionMiddleLayer getSessionMiddleLayer() {
		return SessionMiddleLayer.getInstance();
	}
	
	public static StorageMiddleLayer getStorageMiddleLayer() {
		return StorageMiddleLayer.getInstance();
	}
	
	public static UsersMiddleLayer getUsersMiddleLayer() {
		return UsersMiddleLayer.getInstance();
	}
	
	public static VideoMiddleLayer getVideoMiddleLayer() {
		return VideoMiddleLayer.getInstance();
	}
	
}
