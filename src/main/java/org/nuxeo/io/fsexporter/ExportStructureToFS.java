/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
