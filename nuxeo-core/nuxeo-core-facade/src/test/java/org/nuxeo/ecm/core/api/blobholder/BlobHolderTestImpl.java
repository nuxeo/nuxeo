package org.nuxeo.ecm.core.api.blobholder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

public class BlobHolderTestImpl extends AbstractBlobHolder {

    @Override
    protected String getBasePath() {
        return "Test";
    }

    @Override
    public Blob getBlob() throws ClientException {
        return new StringBlob("Test");
    }

    @Override
    public Calendar getModificationDate() throws ClientException {
        return null;
    }

    public Serializable getProperty(String name) throws ClientException {
        return null;
    }

    public Map<String, Serializable> getProperties() throws ClientException {
        return null;
    }

}
