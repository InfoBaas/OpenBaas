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
        } catch (Throwable throwable) {
			Log.error("", "LoadProperties", "gtProperties", "An error ocorred loadin properties from " +
					resource + ".", throwable); 
        }
        return properties;
    }

}
