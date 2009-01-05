package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;

public class SimpleBlobHolderWithProperties extends SimpleBlobHolder implements
        BlobHolder {

    protected Map<String, Serializable> properties;

    public SimpleBlobHolderWithProperties(Blob blob, Map<String, Serializable> properties) {
        super(blob);
        this.properties = properties;
    }

    public Serializable getProperty(String name) throws ClientException {
        if (properties==null) {
            return null;
        }
        return properties.get(name);
    }
}
