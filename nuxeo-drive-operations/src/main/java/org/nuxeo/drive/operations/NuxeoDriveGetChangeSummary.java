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

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.nuxeo.drive.service.FileSystemChangeSummary;
import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.drive.service.impl.RootDefinitionsHelper;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

/**
 * Gets a summary of document changes in the synchronization roots of the
 * currently authenticated user.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveGetChangeSummary.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get change summary")
public class NuxeoDriveGetChangeSummary {

    public static final String ID = "NuxeoDrive.GetChangeSummary";

    @Context
    protected OperationContext ctx;

    @Param(name = "lastSyncDate", required = false)
    protected Long lastSyncDate = -1L;

    @Param(name = "lowerBound", required = false)
    protected Long lowerBound = -1L;

    // Expect a String structure with form:
    // repo-1:root-ref-1,repo-1:root-ref-2,repo-2:root-ref-3
    @Param(name = "lastSyncActiveRootDefinitions", required = false)
    protected String lastSyncActiveRootDefinitions;

    @OperationMethod
    public Blob run() throws ClientException, IOException {
        NuxeoDriveManager driveManager = Framework.getLocalService(NuxeoDriveManager.class);
        Map<String, Set<IdRef>> lastActiveRootRefs = RootDefinitionsHelper.parseRootDefinitions(lastSyncActiveRootDefinitions);
        FileSystemChangeSummary docChangeSummary;
        if (lastSyncDate >= 0) {
            docChangeSummary = driveManager.getChangeSummary(
                    ctx.getPrincipal(), lastActiveRootRefs, lastSyncDate);
        } else {
            docChangeSummary = driveManager.getChangeSummaryIntegerBounds(
                    ctx.getPrincipal(), lastActiveRootRefs, lowerBound);
        }
        return NuxeoDriveOperationHelper.asJSONBlob(docChangeSummary);
    }

}
