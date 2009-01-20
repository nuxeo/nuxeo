package org.nuxeo.ecm.platform.transform.compat;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;

public class TransformDocumensFactory {


     public static List<TransformDocument> wrap(BlobHolder holder) throws ClientException {

         List<TransformDocument> tdocs = new ArrayList<TransformDocument>();

         List<Blob> blobs = holder.getBlobs();

         for (Blob blob : blobs) {
             tdocs.add(new TransformDocumentImpl(blob));
         }
         return tdocs;
     }

}
