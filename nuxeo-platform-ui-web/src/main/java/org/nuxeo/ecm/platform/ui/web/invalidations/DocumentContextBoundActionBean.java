package org.nuxeo.ecm.platform.ui.web.invalidations;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Base class for Seam beans that use the Automatic invalidation system
 *
 * @author tiry
 */
public abstract class DocumentContextBoundActionBean {

    private DocumentModel currentDocument;

    protected DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    @DocumentContextInvalidation
    public void onContextChange(DocumentModel doc) {
        if (doc == null) {
            currentDocument = null;
            resetBeanCache(null);
            return;
        } else if (currentDocument == null) {
            currentDocument = doc;
            resetBeanCache(doc);
            return;
        }
        if (!doc.getRef().equals(currentDocument.getRef())) {
            currentDocument = doc;
            resetBeanCache(doc);
        }
    }

    protected abstract void resetBeanCache(DocumentModel newCurrentDocumentModel);

}
