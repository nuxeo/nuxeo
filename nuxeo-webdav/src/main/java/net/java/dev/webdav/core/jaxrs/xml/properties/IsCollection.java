package net.java.dev.webdav.core.jaxrs.xml.properties;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * Microsoft Exchange Server 2003 item. http://msdn.microsoft.com/en-us/library/aa487549(v=EXCHG.65).aspx
 *
 * @author Organization: Gagnavarslan ehf
 */
@XmlRootElement(name = "iscollection")
public final class IsCollection {

    @XmlValue
	private Integer collection;

    public IsCollection() {
    }

    public IsCollection(Integer collection) {
        this.collection = collection;
    }

    public Integer getCollection() {
        return collection;
    }
}
