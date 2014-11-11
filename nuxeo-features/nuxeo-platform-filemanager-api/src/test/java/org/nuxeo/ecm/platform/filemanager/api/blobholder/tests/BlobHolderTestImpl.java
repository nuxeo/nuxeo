package org.nuxeo.ecm.platform.filemanager.api.blobholder.tests;

import java.util.Calendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.filemanager.api.blobholder.AbstractBlobHolder;

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

}
