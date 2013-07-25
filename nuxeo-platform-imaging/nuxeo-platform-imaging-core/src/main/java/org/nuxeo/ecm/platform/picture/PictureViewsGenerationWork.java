package org.nuxeo.ecm.platform.picture;

import java.util.ArrayList;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;

/**
 * Work generating the different picture views for a Picture.
 *
 * @since 5.7
 */
public class PictureViewsGenerationWork extends AbstractWork {

    protected final String repositoryName;

    protected final DocumentRef docRef;

    protected final String xpath;

    public static final String CATEGORY_PICTURE_GENERATION = "pictureViewsGeneration";

    @Override
    public String getCategory() {
        return CATEGORY_PICTURE_GENERATION;
    }

    @Override
    public String getTitle() {
        return "Picture views generation " + docRef;
    }

    public PictureViewsGenerationWork(String repositoryName,
            DocumentRef docRef, String xpath) {
        this.repositoryName = repositoryName;
        this.docRef = docRef;
        this.xpath = xpath;
    }

    @Override
    public void work() throws Exception {
        DocumentModel workingDocument = null;

        setProgress(Progress.PROGRESS_INDETERMINATE);
        setStatus("Extracting");
        try {
            initSession(repositoryName);
            workingDocument = session.getDocument(docRef);
            if (workingDocument != null) {
                workingDocument.detach(true);
            }
            commitOrRollbackTransaction();
        } finally {
            cleanUp(true, null);
        }

        if (workingDocument != null) {
            setStatus("Generating views");
            Property fileProp = workingDocument.getProperty(xpath);
            ArrayList<Map<String, Object>> pictureTemplates = null;
            PictureResourceAdapter picture = workingDocument.getAdapter(PictureResourceAdapter.class);
            Blob blob = (Blob) fileProp.getValue();
            String filename = blob == null ? null : blob.getFilename();
            String title = workingDocument.getTitle();
            picture.fillPictureViews(blob, filename, title, pictureTemplates);
        } else {
            setStatus("Nothing to process");
            return;
        }

        startTransaction();
        setStatus("Saving");
        initSession(repositoryName);
        session.saveDocument(workingDocument);
        setStatus(null);
    }

}
