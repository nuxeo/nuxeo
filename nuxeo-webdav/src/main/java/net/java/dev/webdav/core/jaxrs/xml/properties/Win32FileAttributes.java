package net.java.dev.webdav.core.jaxrs.xml.properties;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Microsoft Extension property. http://msdn.microsoft.com/en-us/library/cc250143(v=PROT.10).aspx
 *
 * @author Organization: Gagnavarslan ehf
 */
@XmlRootElement(name = "Win32FileAttributes")
public final class Win32FileAttributes {

    @XmlValue
	private String value;

    public Win32FileAttributes() {
    }

    public Win32FileAttributes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
