/*
 * (C) Copyright 2012-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Olivier Grisel <ogrisel@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.impl.NuxeoDriveManagerImpl;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Generates a conflicted name for a {@link FileSystemItem} given its name, the currently authenticated user's first
 * name and last name. Doing so as an operation make it possible to override this part without having to fork the client
 * codebase.
 *
 * @author Olivier Grisel
 */
@Operation(id = NuxeoDriveGenerateConflictedItemName.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Generate Conflicted Item Name")
public class NuxeoDriveGenerateConflictedItemName {

    public static final String ID = "NuxeoDrive.GenerateConflictedItemName";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @OperationMethod
    public Blob run() throws IOException {
        String extension = "";
        if (name.contains(".")) {
            // Split on the last occurrence of . using a negative lookahead
            // regexp.
            String[] parts = name.split("\\.(?=[^\\.]+$)");
            name = parts[0];
            extension = "." + parts[1];
        }
        NuxeoPrincipal principal = ctx.getPrincipal();
        String userName = principal.getName(); // fallback
        if (!StringUtils.isBlank(principal.getLastName()) && !StringUtils.isBlank(principal.getFirstName())) {
            // build more user friendly name from user info
            userName = principal.getFirstName() + " " + principal.getLastName();
        }
        Calendar userDate = Calendar.getInstance(NuxeoDriveManagerImpl.UTC);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm");
        dateFormat.setCalendar(userDate);
        String formatedDate = dateFormat.format(userDate.getTime());
        String contextSection = String.format(" (%s - %s)", userName, formatedDate);
        String conflictedName = name + contextSection + extension;
        return Blobs.createJSONBlobFromValue(conflictedName);
    }

}
