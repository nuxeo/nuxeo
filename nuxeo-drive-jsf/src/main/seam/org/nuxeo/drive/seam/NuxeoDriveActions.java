package org.nuxeo.drive.seam;

import java.io.Serializable;
import java.util.Set;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.runtime.api.Framework;

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

    @Factory(value = CURRENT_SYNCHRONIZATION_ROOT, scope = ScopeType.EVENT)
    public DocumentModel getCurrentSynchronizationRoot() throws ClientException {
        if (navigationContext == null || documentManager == null) {
            return null;
        }
        // Use the event context as request cache
        Context cache = Contexts.getEventContext();
        Boolean isUnderSync = (Boolean) cache.get(IS_UNDER_SYNCHRONIZATION_ROOT);
        if (isUnderSync == null) {
            NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
            Set<IdRef> references = driveManager.getSynchronizationRootReferences(
                    documentManager.getPrincipal().getName(), documentManager);
            DocumentModelList path = navigationContext.getCurrentPath();
            DocumentModel root = null;
            // list is ordered such as closest synchronized ancestor is
            // considered the current synchronization root
            for (DocumentModel parent : path) {
                if (references.contains(parent.getRef())) {
                    root = parent;
                    break;
                }
            }
            cache.set(CURRENT_SYNCHRONIZATION_ROOT, root);
            cache.set(IS_UNDER_SYNCHRONIZATION_ROOT, root != null);
        }
        return (DocumentModel) cache.get(CURRENT_SYNCHRONIZATION_ROOT);
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
        return getCurrentSynchronizationRoot() == null;
    }

    @Factory(value = "canUnSynchronizeCurrentDocument", scope = ScopeType.EVENT)
    public boolean getCanUnSynchronizeCurrentDocument() throws ClientException {
        if (navigationContext == null) {
            return false;
        }
        DocumentModel currentDocument = navigationContext.getCurrentDocument();
        DocumentRef currentDocRef = currentDocument.getRef();
        DocumentModel currentSyncRoot = getCurrentSynchronizationRoot();
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
        DocumentModel currentSyncRoot = getCurrentSynchronizationRoot();
        if (currentSyncRoot == null) {
            return false;
        }
        return !currentDocRef.equals(currentSyncRoot.getRef());
    }

    public void synchronizeCurrentDocument() throws ClientException {
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        String userName = documentManager.getPrincipal().getName();
        DocumentModel newSyncRoot = navigationContext.getCurrentDocument();
        driveManager.synchronizeRoot(userName, newSyncRoot);
    }

    public void unsynchronizeCurrentDocument() throws ClientException {
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        String userName = documentManager.getPrincipal().getName();
        DocumentModel syncRoot = navigationContext.getCurrentDocument();
        driveManager.unsynchronizeRoot(userName, syncRoot);
    }

    public String navigateToCurrentSynchronizationRoot() throws ClientException {
        DocumentModel currentRoot = getCurrentSynchronizationRoot();
        if (currentRoot == null) {
            return "";
        }
        return navigationContext.navigateToDocument(currentRoot);
    }

}
