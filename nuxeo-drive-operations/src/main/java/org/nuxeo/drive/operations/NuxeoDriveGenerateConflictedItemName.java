/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.operations;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Renames the {@link FileSystemItem} with the given id with the given name for
 * the currently authenticated user.
 *
 * @author Olivier Grisel
 */
@Operation(id = NuxeoDriveGenerateConflictedItemName.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Generate Conflicted Item Name")
public class NuxeoDriveGenerateConflictedItemName {

    public static final String ID = "NuxeoDrive.GenerateConflictRename";

    @Context
    protected OperationContext ctx;

    @Param(name = "name")
    protected String name;

    @Param(name = "timezone", required = false)
    protected String timezone;

    @OperationMethod
    public Blob run() throws Exception {

        String extension = "";
        if (name.contains(".")) {
            // Split on the last occurrence of . using a negative lookahead
            // regexp.
            String[] parts = name.split("\\.(?=[^\\.]+$)");
            name = parts[0];
            extension = "." + parts[1];
        }
        NuxeoPrincipal principal = (NuxeoPrincipal) ctx.getPrincipal();
        String userName = principal.getName(); // fallback
        if (principal.getLastName() != null && principal.getFirstName() != null) {
            // build more user friendly name from user info
            userName = principal.getFirstName() + " " + principal.getLastName();
        }
        Calendar userDate;
        if (timezone != null) {
            userDate = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        } else {
            userDate = Calendar.getInstance();
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm");
        dateFormat.setCalendar(userDate);
        String formatedDate = dateFormat.format(userDate.getTime());
        String contextSection = String.format(" (%s - %s)", userName,
                formatedDate);
        String conflictedName = name + contextSection + extension;
        return NuxeoDriveOperationHelper.asJSONBlob(conflictedName);
    }

}
