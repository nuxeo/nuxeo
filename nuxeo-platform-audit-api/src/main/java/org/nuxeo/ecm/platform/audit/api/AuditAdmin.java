package org.nuxeo.ecm.platform.audit.api;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Interface for Administration of Audit service
 *
 *
 * @author tiry
 *
 */
public interface AuditAdmin {

    /**
     * Forces log Synchronisation for a branch of the repository. This can be
     * useful to add the create entries if DB was initialized from a bulk
     * import.
     *
     * @param repoId
     * @param path
     * @param recurs
     * @return
     * @throws AuditException
     * @throws ClientException
     */
    long syncLogCreationEntries(String repoId, String path, Boolean recurs)
            throws AuditException, ClientException;
}
