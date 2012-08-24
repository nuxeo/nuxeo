package org.nuxeo.drive.seam;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

@Name("nuxeoDriveActions")
@Scope(ScopeType.CONVERSATION)
public class NuxeoDriveActions {

    @In(required = false)
    NavigationContext navigationContext;

    @Factory(value = "isCurrentDocumentUnderSynchronization", scope = ScopeType.EVENT)
    public boolean isCurrentDocumentUnderSynchronization() {
        if (navigationContext == null) {
            return false;
        }
        // TODO: call the NuxeoDriveService to check whether the current
        // document path is under of the synchronization root.
        return true;
    }

    @Factory(value = "isCurrentDocumentSynchronisationRoot", scope = ScopeType.EVENT)
    public boolean isCurrentDocumentSynchronisationRoot() {
        if (navigationContext == null) {
            return false;
        }
        DocumentRef currentDocRef = navigationContext.getCurrentDocument().getRef();
        return currentDocRef.equals(getCurrentDocumentSynchronizationRoot().getRef());
    }

    public DocumentModel getCurrentDocumentSynchronizationRoot() {
        // TODO: call the NuxeoDriveService to check whether the current
        // document path is under of the synchronization root.
        return null;
    }
}
