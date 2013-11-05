package infosistema.openbaas.utils;

import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Loads properties.
 */
public final class LoadProperties {

    /**
     * Hidden constructor.
     */
    private LoadProperties() {
    }

    /**
     * Loads properties from a resource.
     *
     * @param resource The resource with the properties
     * @return The Properties
     */
    public static Properties getProperties(String resource) {
        Properties properties = new Properties();
        try {
            ResourceBundle resourceBundle = ResourceBundle.getBundle(resource);
            Enumeration<String> keys = resourceBundle.getKeys();
            String key;
            while (keys.hasMoreElements()) {
                key = (String) keys.nextElement();
                properties.setProperty(key, resourceBundle.getString(key));
            }
        } catch (Throwable throwable) {}
        return properties;
    }

}
