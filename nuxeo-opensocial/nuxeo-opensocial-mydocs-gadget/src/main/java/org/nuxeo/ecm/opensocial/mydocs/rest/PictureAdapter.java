package org.nuxeo.ecm.opensocial.mydocs.rest;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.rest.DocumentObject;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

@WebAdapter(name = "picture", type = "pictureAdapter", targetType = "Document")
public class PictureAdapter extends DefaultAdapter {

    @GET
    public Object doGet() throws ClientException {

        DocumentModel doc = ((DocumentObject) getTarget()).getDocument();

        BlobHolder blobHolder = new SimpleBlobHolder(doc.getProperty(
                "picture:views/view[0]/content")
                .getValue(Blob.class));

        Blob blob = blobHolder.getBlob();

        return Response.ok(blob)
                .header("Content-Disposition",
                        "inline;filename=" + blob.getFilename())
                .type(blob.getMimeType())
                .build();
    }
}
