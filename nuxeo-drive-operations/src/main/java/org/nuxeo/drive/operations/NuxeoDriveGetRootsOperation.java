/**
 * 
 */

package org.nuxeo.drive.operations;

import java.util.Set;

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

/**
 * Fetch the list of synchronization roots for the currently authenticated user.
 */
@Operation(id = NuxeoDriveGetRootsOperation.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get Roots")
public class NuxeoDriveGetRootsOperation {

    public static final String ID = "NuxeoDrive.GetRoots";

    @Context
    protected CoreSession session;
    
    @OperationMethod
    public DocumentModelList run() throws ClientException {
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        Set<IdRef> references = driveManager.getSynchronizationRootReferences(
                session.getPrincipal().getName(), session);
        return session.getDocuments(references.toArray(new DocumentRef[references.size()]));
    }

}
