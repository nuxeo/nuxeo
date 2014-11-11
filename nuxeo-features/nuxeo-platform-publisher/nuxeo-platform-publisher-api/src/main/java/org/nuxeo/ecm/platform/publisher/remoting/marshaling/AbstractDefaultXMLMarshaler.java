package org.nuxeo.ecm.platform.publisher.remoting.marshaling;

/**
 * Base class for XML marshalers
 * 
 * @author tiry
 * 
 */
public abstract class AbstractDefaultXMLMarshaler {

    protected static final String publisherSerializerNS = "http://www.nuxeo.org/publisher";

    protected static final String publisherSerializerNSPrefix = "nxpub";

    protected String cleanUpXml(String data) {
        if (data == null)
            return null;
        if (data.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"))
            data = data.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n",
                    "");
        return data;
    }
}
