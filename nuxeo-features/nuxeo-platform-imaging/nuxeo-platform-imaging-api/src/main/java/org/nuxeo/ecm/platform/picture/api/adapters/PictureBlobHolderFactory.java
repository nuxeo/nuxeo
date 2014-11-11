package org.nuxeo.ecm.platform.picture.api.adapters;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderFactory;

public class PictureBlobHolderFactory implements BlobHolderFactory {

    public PictureBlobHolderFactory(){
        
    }
    
    public BlobHolder getBlobHolder(DocumentModel doc) {
        String docType = doc.getType();

        if (docType.equals("Picture")) {
            return new PictureBlobHolder(doc, "");
        } else if (docType.equals("PictureBook")) {
            return new PictureBookBlobHolder(doc, "");
        }
        return null;
    }

}
