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
package infosistema.openbaas.dataaccess.files;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import infosistema.openbaas.data.enums.ModelEnum;

public interface FileInterface {

	// *** CREATE *** //
	
	public boolean createApp(String appId) throws Exception;
	
	// *** UPLOAD *** //

	public String upload(String appId, ModelEnum type, String id, String extension, InputStream stream) throws Exception;
	
	
	// *** DOWNLOAD *** //
	
	public byte[] download(String appId, ModelEnum type, String id, String extension, String quality, String bars) throws IOException;

	
	// *** DETETE *** //
	
	public boolean deleteFile(String appId, ModelEnum type, String id, String extension);
	
	public Boolean delFilesResolution(String appId, ModelEnum type, List<String> filesRes);
	
}
