package org.nuxeo.ecm.platform.picture;

import static org.nuxeo.ecm.core.api.CoreSession.ALLOW_VERSION_WRITE;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.runtime.api.Framework;

/**
 * Work generating the different picture views for a Picture.
 *
 * @since 5.7
 */
public class PictureViewsGenerationWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    public static final String CATEGORY_PICTURE_GENERATION = "pictureViewsGeneration";

    public static final String PICTURE_VIEWS_GENERATION_DONE_EVENT = "pictureViewsGenerationDone";

    protected final String xpath;

    public PictureViewsGenerationWork(String repositoryName, String docId, String xpath) {
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
    public void work() {
        setProgress(Progress.PROGRESS_INDETERMINATE);
        setStatus("Extracting");

        openSystemSession();
        if (!session.exists(new IdRef(docId))) {
            setStatus("Nothing to process");
            return;
        }

        DocumentModel workingDocument = session.getDocument(new IdRef(docId));
        Property fileProp = workingDocument.getProperty(xpath);
        Blob blob = (Blob) fileProp.getValue();
        if (blob == null) {
            // do nothing
            return;
        }

        String title = workingDocument.getTitle();
        setStatus("Generating views");
        try {
            PictureResourceAdapter picture = workingDocument.getAdapter(PictureResourceAdapter.class);
            picture.fillPictureViews(blob, blob.getFilename(), title, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!session.exists(new IdRef(docId))) {
            setStatus("Nothing to process");
            return;
        }
        setStatus("Saving");
        if (workingDocument.isVersion()) {
            workingDocument.putContextData(ALLOW_VERSION_WRITE, Boolean.TRUE);
        }
        workingDocument.putContextData("disableNotificationService", Boolean.TRUE);
        workingDocument.putContextData("disableAuditLogger", Boolean.TRUE);
        workingDocument.putContextData(VersioningService.DISABLE_AUTO_CHECKOUT, Boolean.TRUE);
        session.saveDocument(workingDocument);

        firePictureViewsGenerationDoneEvent(workingDocument);

        setStatus("Done");
    }

    /**
     * Fire a {@code PICTURE_VIEWS_GENERATION_DONE_EVENT} event when no other PictureViewsGenerationWork is scheduled
     * for this document.
     *
     * @since 5.8
     */
    protected void firePictureViewsGenerationDoneEvent(DocumentModel doc) {
        WorkManager workManager = Framework.getLocalService(WorkManager.class);
        List<String> workIds = workManager.listWorkIds(CATEGORY_PICTURE_GENERATION, null);
        int worksCount = 0;
        for (String workId : workIds) {
            if (workId.equals(getId())) {
                if (++worksCount > 1) {
                    // another work scheduled
                    return;
                }
            }
        }
        DocumentEventContext ctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = ctx.newEvent(PICTURE_VIEWS_GENERATION_DONE_EVENT);
        Framework.getLocalService(EventService.class).fireEvent(event);
    }

}
