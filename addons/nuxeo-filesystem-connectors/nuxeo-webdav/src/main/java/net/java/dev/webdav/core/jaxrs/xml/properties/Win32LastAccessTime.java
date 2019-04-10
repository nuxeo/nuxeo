package net.java.dev.webdav.core.jaxrs.xml.properties;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Microsoft Extension property. http://msdn.microsoft.com/en-us/library/cc250144(v=PROT.10).aspx
 *
 * @author Organization: Gagnavarslan ehf
 */
@XmlRootElement(name = "Win32LastAccessTime")
public final class Win32LastAccessTime {

    @XmlValue
	private String value;

    public Win32LastAccessTime() {
    }

    public Win32LastAccessTime(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
