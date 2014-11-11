package org.nuxeo.ecm.platform.picture;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.work.AbstractWork;

/**
 * Work generating the different picture views for a Picture.
 *
 * @since 5.7
 */
public class PictureViewsGenerationWork extends AbstractWork {

    protected final String repositoryName;

    protected final DocumentRef docRef;

    public static final String CATEGORY_PICTURE_GENERATION = "pictureViewsGeneration";

    @Override
    public String getCategory() {
        return CATEGORY_PICTURE_GENERATION;
    }

    @Override
    public String getTitle() {
        return "Picture views generation " + docRef;
    }

    public PictureViewsGenerationWork(String repositoryName, DocumentRef docRef) {
        this.repositoryName = repositoryName;
        this.docRef = docRef;
    }

    @Override
    public void work() throws Exception {
        initSession(repositoryName);
        DocumentModel doc = session.getDocument(docRef);
        Property fileProp = doc.getProperty("file:content");
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        bh.setBlob(fileProp.getValue(Blob.class));
        session.saveDocument(doc);
        session.save();
    }

}
