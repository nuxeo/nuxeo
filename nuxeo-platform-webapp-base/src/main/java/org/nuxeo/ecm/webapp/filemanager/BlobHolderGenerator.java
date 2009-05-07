package org.nuxeo.ecm.webapp.filemanager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

@Name("blobHolderGenerator")
@Scope(ScopeType.EVENT)
public class BlobHolderGenerator {

    @In(create = true, required=false)
    private NavigationContext navigationContext;

    @Factory(value="currentDocumentAsBlobHolder", scope = ScopeType.EVENT)
    public BlobHolder getCurrentBlobHolder() {
        if (navigationContext==null) {
            return null;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (currentDocument!=null) {
            return currentDocument.getAdapter(BlobHolder.class);
        }
        else {
            return null;
        }
    }
}
