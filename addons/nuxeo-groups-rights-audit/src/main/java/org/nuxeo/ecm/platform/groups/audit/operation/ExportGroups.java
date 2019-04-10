/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
