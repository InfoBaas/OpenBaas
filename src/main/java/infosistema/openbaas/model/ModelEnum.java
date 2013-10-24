package infosistema.openbaas.model;

public enum ModelEnum {
	image,
	video,
	audio,
	users,
	data;

	public static ModelEnum getModelForExtension(String extension) {
		if (extension.equalsIgnoreCase("jpg")) return image;
		else if (extension.equalsIgnoreCase("wmv")) return video;
		else if (extension.equalsIgnoreCase("mp3")) return audio;
		else if (extension.equalsIgnoreCase("users")) return users;
		else return data;
	}
}
