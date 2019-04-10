/**
 *
 */

package org.nuxeo.drive.operations;

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Make it possible for the client to add / remove synchronization roots.
 */
@Operation(id = NuxeoDriveSetSynchronizationOperation.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Register / Unregister Synchronization Root")
public class NuxeoDriveSetSynchronizationOperation {

    public static final String ID = "NuxeoDrive.SetSynchronization";

    @Context
    protected CoreSession session;

    @Param(name = "enable")
    protected boolean enable;

    @OperationMethod
    public void run(DocumentModel doc) {
        NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
        if (enable) {
            driveManager.registerSynchronizationRoot(session.getPrincipal(), doc, session);
        } else {
            driveManager.unregisterSynchronizationRoot(session.getPrincipal(), doc, session);
        }
    }

}
