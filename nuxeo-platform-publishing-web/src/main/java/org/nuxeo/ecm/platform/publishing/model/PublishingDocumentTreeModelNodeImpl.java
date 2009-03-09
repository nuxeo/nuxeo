package org.nuxeo.ecm.platform.publishing.model;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelTreeNodeImpl;

public class PublishingDocumentTreeModelNodeImpl extends
        DocumentModelTreeNodeImpl {
    private static final long serialVersionUID = 8464402496588179552L;

    private boolean unpublishable = false;

    public PublishingDocumentTreeModelNodeImpl(DocumentModel doc, int level) {
        super(doc, level);
    }

    public PublishingDocumentTreeModelNodeImpl(DocumentModel doc, int level,
            boolean unpublishable) {
        super(doc, level);
        this.unpublishable = unpublishable;
    }

    public boolean isUnpublishable() {
        return unpublishable;
    }

    public void setUnpublishable(boolean unpublishable) {
        this.unpublishable = unpublishable;
    }
}
