package org.nuxeo.drive.service;

import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.security.SecurityException;

/**
 * Manage list of NuxeoDrive synchronization roots and devices for a given nuxeo
 * user.
 */
public interface NuxeoDriveManager {

    /**
     * @param userName the id of the Nuxeo Drive user
     * @param newRootContainer the folderish document to be used as
     *            synchronization root: must be bound to an active session
     * @throws ClientException
     * @throws SecurityException if the user does not have write permissions to
     *             the container.
     */
    public void synchronizeRoot(String userName, DocumentModel newRootContainer)
            throws ClientException, SecurityException;

    /**
     * @param userName the id of the Nuxeo Drive user
     * @param rootContainer the folderish document that should no longer be used
     *            as a synchronization root
     */
    public void unsynchronizeRoot(String userName, DocumentModel rootContainer)
            throws ClientException;

    /**
     * Fetch the list of synchronization root ids for a given user. This list is
     * assumed to be short enough (in the order of 100 folder max) so that no
     * paging API is required.
     * 
     * @param userName the id of the Nuxeo Drive user
     * @param session active CoreSession instance to the repository hosting the
     *            roots.
     * @return the ordered set of non deleted synchronization roots for that
     *         user
     */
    public Set<IdRef> getSynchronizationRootReferences(String userName,
            CoreSession session) throws ClientException;

    /**
     * Method to be called by a CoreEvent listener monitoring documents
     * deletions to cleanup references to recently deleted documents and
     * invalidate the caches.
     */
    public void handleFolderDeletion(IdRef ref) throws ClientException;

}
