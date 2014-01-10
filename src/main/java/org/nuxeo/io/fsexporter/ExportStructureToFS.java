/**
 *
 */

package org.nuxeo.io.fsexporter;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * @author annejubert
 */
@Operation(id = ExportStructureToFS.ID, category = Constants.CAT_SERVICES, label = "ExportStructureToFS", description = "This operation enables to export the structure contained in the Root name path to the File System Target path. You can choose to export deleted documents or not and you can define your own Page Provider")
public class ExportStructureToFS {

    public static final String ID = "ExportStructureToFS";

    @Context
    FSExporterService service;

    @Context
    protected CoreSession session;

    @Param(name = "Root Name", required = true)
    protected String RootName;

    @Param(name = "File System Target", required = true)
    protected String FileSystemTarget;

    @Param(name = "Query", required = false)
    protected String PageProvider;

    @OperationMethod
    public void run() throws Exception {
        service.export(session, RootName, FileSystemTarget, PageProvider);
    }

}
