package org.nuxeo.ecm.admin.operation;

import org.nuxeo.ecm.admin.permissions.PermissionsPurgeWork;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Schedule a {@link PermissionsPurgeWork} to archive ACEs based on permissions_purge page provider from the input
 * document.
 *
 * @since 9.1
 */
@Operation(id = PermissionsPurge.ID, category = Constants.CAT_SERVICES, label = "Archiving ACEs", description = "Schedule a work to archive ACEs based on permissions_purge page provider from the input document.")
public class PermissionsPurge {
    static final String ID = "PermissionsPurge";

    @OperationMethod
    public void purge(DocumentModel doc) {
        new PermissionsPurgeWork(doc).launch();
    }
}
