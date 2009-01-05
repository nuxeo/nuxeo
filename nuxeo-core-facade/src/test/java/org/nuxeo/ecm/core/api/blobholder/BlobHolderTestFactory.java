package org.nuxeo.ecm.core.api.blobholder;

import org.nuxeo.ecm.core.api.DocumentModel;

public class BlobHolderTestFactory implements BlobHolderFactory {

    public BlobHolder getBlobHolder(DocumentModel doc) {
        return new BlobHolderTestImpl();
    }

}
