/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.runtime.api.Framework;

/**
 * Gets a summary of document changes in the synchronization roots of the currently authenticated user.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveGetChangeSummary.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get change summary")
public class NuxeoDriveGetChangeSummary {

    public static final String ID = "NuxeoDrive.GetChangeSummary";

    @Context
    protected OperationContext ctx;

    @Param(name = "lowerBound", required = false)
    protected Long lowerBound = -1L;

    /**
     * Expect a String structure with form: repo-1:root-ref-1,repo-1:root-ref-2,repo-2:root-ref-3
     *
     * @deprecated since 10.3
     */
    @Deprecated
    @Param(name = "lastSyncActiveRootDefinitions", required = false)
    protected String lastSyncActiveRootDefinitions;

    @OperationMethod
    public Blob run() throws IOException {
        NuxeoDriveManager driveManager = Framework.getService(NuxeoDriveManager.class);
        Map<String, Set<IdRef>> lastActiveRootRefs = RootDefinitionsHelper.parseRootDefinitions(
                lastSyncActiveRootDefinitions);
        FileSystemChangeSummary docChangeSummary;
        docChangeSummary = driveManager.getChangeSummary(ctx.getPrincipal(), lastActiveRootRefs, lowerBound);
        return Blobs.createJSONBlobFromValue(docChangeSummary);
    }

}
