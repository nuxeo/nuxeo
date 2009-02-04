package org.nuxeo.ecm.platform.transform.compat;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;

public class BlobHolderWrappingTransformDocuments extends SimpleBlobHolder {

    public BlobHolderWrappingTransformDocuments(List<TransformDocument> tdocs) {
        super();
        List<Blob> newBlobs = new ArrayList<Blob>();
        for (TransformDocument tdoc : tdocs) {
            newBlobs.add(tdoc.getBlob());
        }
        init(newBlobs);
    }

}
