package org.nuxeo.ecm.platform.filemanager.api.blobholder.tests;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.filemanager.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.filemanager.api.blobholder.BlobHolderFactory;

public class BlobHolderTestFactory implements BlobHolderFactory {

    public BlobHolder getBlobHolder(DocumentModel doc) {
        return new BlobHolderTestImpl();
    }

}
