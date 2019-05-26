/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */

package org.nuxeo.ecm.platform.groups.audit.operation;

import java.io.File;
import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.groups.audit.ExcelExportConstants;
import org.nuxeo.ecm.platform.groups.audit.service.ExcelExportService;

/**
 * Export Groups returns the Nuxeo groups excel report listing subgroups/users as a blob
 *
 * @since 5.7
 */
@Operation(id = ExportGroups.ID, category = Constants.CAT_USERS_GROUPS, label = "ExportGroups", description = "Export Groups returns the Nuxeo groups excel report listing subgroups/users")
public class ExportGroups {

    public static final String ID = "UserManager.ExportGroups";

    @Context
    ExcelExportService service;

    @OperationMethod
    public Blob run() throws IOException {
        File export = service.getExcelReport(ExcelExportConstants.EXCEL_EXPORT_ALL_GROUPS);
        if (export != null) {
            return Blobs.createBlob(export);
        }
        return null;
    }
}
