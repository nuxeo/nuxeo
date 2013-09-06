package org.nuxeo.ecm.platform.picture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;

/**
 * Work generating the different picture views for a Picture.
 *
 * @since 5.7
 */
public class PictureViewsGenerationWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY_PICTURE_GENERATION = "pictureViewsGeneration";

    protected final String xpath;

    public PictureViewsGenerationWork(String repositoryName,
            String docId, String xpath) {
        super(repositoryName + ':' + docId + ':' + xpath + ":pictureView");
        setDocument(repositoryName, docId);
        this.xpath = xpath;
    }

    @Override
    public String getCategory() {
        return CATEGORY_PICTURE_GENERATION;
    }

    @Override
    public String getTitle() {
        return "Picture views generation";
    }

    @Override
    public void work() throws Exception {
        DocumentModel workingDocument = null;

        setProgress(Progress.PROGRESS_INDETERMINATE);
        setStatus("Extracting");
        try {
            initSession();
            workingDocument = session.getDocument(new IdRef(docId));
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
        initSession();
        session.saveDocument(workingDocument);
        setStatus("Done");
    }

}
