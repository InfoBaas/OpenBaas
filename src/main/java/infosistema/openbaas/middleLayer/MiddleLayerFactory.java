package infosistema.openbaas.middleLayer;

public class MiddleLayerFactory {

	public static AclMiddleLayer getAclMiddleLayer() {
		return AclMiddleLayer.getInstance();
	}

	public static AppsMiddleLayer getAppsMiddleLayer() {
		return AppsMiddleLayer.getInstance();
	}
	
	public static DocumentMiddleLayer getDocumentMiddleLayer() {
		return DocumentMiddleLayer.getInstance();
	}

	public static MediaMiddleLayer getMediaMiddleLayer() {
		return MediaMiddleLayer.getInstance();
	}

	public static SessionMiddleLayer getSessionMiddleLayer() {
		return SessionMiddleLayer.getInstance();
	}
	
	public static UsersMiddleLayer getUsersMiddleLayer() {
		return UsersMiddleLayer.getInstance();
	}
	
}
