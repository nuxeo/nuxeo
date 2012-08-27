package org.nuxeo.drive.seam;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;

@Name("nuxeoDriveActions")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class NuxeoDriveActions implements Serializable {

    protected static final String IS_UNDER_SYNCHRONIZATION_ROOT = "nuxeoDriveIsCurrentDocumentUnderSynchronizationRoot";

    protected static final String CURRENT_SYNCHRONIZATION_ROOT = "nuxeoDriveCurrentSynchronizationRoot";

    private static final long serialVersionUID = 1L;

    @In(required = false)
    NavigationContext navigationContext;

    @In(required = false)
    CoreSession documentManager;

    public DocumentModel getSynchronizationRoot(DocumentModel currentDocument) {
        // Use the even context as request cache
        Context cache = Contexts.getEventContext();
        Boolean isUnderSync = (Boolean) cache.get(IS_UNDER_SYNCHRONIZATION_ROOT);
        if (isUnderSync == null) {
            // TODO: call the NuxeoDriveService to check whether the current
            // document path is under of the synchronization root.
            DocumentModel root = null;
            cache.set(CURRENT_SYNCHRONIZATION_ROOT, root);
            cache.set(IS_UNDER_SYNCHRONIZATION_ROOT, root != null);
        }
        return (DocumentModel) cache.get(CURRENT_SYNCHRONIZATION_ROOT);
    }

    @Factory(value = CURRENT_SYNCHRONIZATION_ROOT, scope = ScopeType.EVENT)
    public DocumentModel getCurrentDocumentSynchronizationRoot() {
        if (navigationContext == null) {
            return null;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        return getSynchronizationRoot(currentDocument);
    }

    @Factory(value = "canSynchronizeCurrentDocument", scope = ScopeType.EVENT)
    public boolean getCanSynchronizeCurrentDocument() throws ClientException {
        if (navigationContext == null) {
            return false;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        if (!currentDocument.isFolder()) {
            return false;
        }
        boolean hasPermission = documentManager.hasPermission(
                currentDocument.getRef(), SecurityConstants.ADD_CHILDREN);
        if (!hasPermission) {
            return false;
        }
        return getSynchronizationRoot(currentDocument) == null;
    }

    @Factory(value = "canUnSynchronizeCurrentDocument", scope = ScopeType.EVENT)
    public boolean getCanUnSynchronizeCurrentDocument() throws ClientException {
        if (navigationContext == null) {
            return false;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRef currentDocRef = currentDocument.getRef();
        DocumentModel currentSyncRoot = getSynchronizationRoot(currentDocument);
        if (currentSyncRoot == null) {
            return false;
        }
        return currentDocRef.equals(currentSyncRoot.getRef());
    }

    @Factory(value = "canUnSynchronizeContainer", scope = ScopeType.EVENT)
    public boolean getCanUnSynchronizeContainer() throws ClientException {
        if (navigationContext == null) {
            return false;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRef currentDocRef = currentDocument.getRef();
        DocumentModel currentSyncRoot = getSynchronizationRoot(currentDocument);
        if (currentSyncRoot == null) {
            return false;
        }
        return !currentDocRef.equals(currentSyncRoot.getRef());
    }

    public void synchronizeCurrentDocument() {
        // TODO
    }

    public void unsynchronizeCurrentDocumentRoot() {
        // TODO
    }

}
