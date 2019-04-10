package org.nuxeo.ecm.webdav.jaxrs;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Microsoft Folder Item property. http://msdn.microsoft.com/en-us/library/bb787821(VS.85).aspx 
 *
 * @author Organization: Gagnavarslan ehf
 */
@XmlRootElement(name = "isFolder")
public final class IsFolder {

	@XmlValue
	private String folder;

    public IsFolder() {
    }

    public IsFolder(final String folder) {
		this.folder = folder;
	}

    public String getFolder() {
        return folder;
    }
}
