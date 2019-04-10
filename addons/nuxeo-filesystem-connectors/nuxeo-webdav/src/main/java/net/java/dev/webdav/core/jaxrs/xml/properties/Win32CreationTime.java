package net.java.dev.webdav.core.jaxrs.xml.properties;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Microsoft Extension property. http://msdn.microsoft.com/en-us/library/cc250142(PROT.10).aspx
 *
 * @author Organization: Gagnavarslan ehf
 */
@XmlRootElement(name = "Win32CreationTime")
public final class Win32CreationTime {

    @XmlValue
	private String value;

    public Win32CreationTime() {
    }

    public Win32CreationTime(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
