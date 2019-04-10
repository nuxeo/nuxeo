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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.RootlessItemException;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.api.Framework;

/**
 * Checks if the document backing the {@link FileSystemItem} with the given source id can be moved to the document
 * backing the {@link FileSystemItem} with the given destination id.
 *
 * @author Antoine Taillefer
 * @deprecated since 10.3
 */
@Deprecated
@Operation(id = NuxeoDriveCanMove.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Can move", description = "Check if the document backing the file system item with the given source id can be moved to the document backing the file system item with the given destination id." //
        + " Return the result as a JSON blob.", deprecatedSince = "10.3")
public class NuxeoDriveCanMove {

    private static final Logger log = LogManager.getLogger(NuxeoDriveCanMove.class);

    public static final String ID = "NuxeoDrive.CanMove";

    @Context
    protected OperationContext ctx;

    @Param(name = "srcId", description = "Id of the source file system item.")
    protected String srcId;

    @Param(name = "destId", description = "Id of the destination file system item.")
    protected String destId;

    @OperationMethod
    public Blob run() throws IOException {
        boolean canMove = false;
        try {
            FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
            canMove = fileSystemItemManager.canMove(srcId, destId, ctx.getPrincipal());
        } catch (RootlessItemException e) {
            // can happen if srcId or destId no longer match a document under an
            // active sync root: just return false in that case.
            log.debug("Cannot move {} to {}: {}", () -> srcId, () -> destId, e::getMessage, () -> e);
        }
        return Blobs.createJSONBlobFromValue(canMove);
    }

}
