/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     annejubert
 */

package org.nuxeo.io.fsexporter;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;

@Operation(id = ExportStructureToFS.ID, category = Constants.CAT_SERVICES, label = "ExportStructureToFS", description = "This operation enables to export the structure contained in the Root name path to the File System Target path. You can declare your own query to choose the document being exported.")
public class ExportStructureToFS {

    public static final String ID = "ExportStructureToFS";

    @Context
    FSExporterService service;

    @Context
    protected CoreSession session;

    @Param(name = "Root Path", required = true)
    protected String RootPath;

    @Param(name = "File System Target", required = true)
    protected String FileSystemTarget;

    @Param(name = "Query", required = false)
    protected String customQuery;

    @OperationMethod
    public void run() throws Exception {
        service.export(session, RootPath, FileSystemTarget, customQuery);
    }

}
