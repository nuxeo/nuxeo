/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.automation.InvalidOperationException;
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
 * Moves the document backing the {@link FileSystemItem} with the given source id to the document backing the
 * {@link FileSystemItem} with the given destination id.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveMove.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Move", description = "Move the document backing the file system item with the given source id to the document backing the file system item with the given destination id." //
        + " Return the moved file system item as a JSON blob.")
public class NuxeoDriveMove {

    public static final String ID = "NuxeoDrive.Move";

    @Context
    protected OperationContext ctx;

    @Param(name = "srcId", description = "Id of the source file system item.")
    protected String srcId;

    @Param(name = "destId", description = "Id of the destination file system item.")
    protected String destId;

    @OperationMethod
    public Blob run() throws InvalidOperationException, IOException {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        FileSystemItem fsItem;
        try {
            fsItem = fileSystemItemManager.move(srcId, destId, ctx.getPrincipal());
        } catch (UnsupportedOperationException e) {
            throw new InvalidOperationException(e);
        }

        return Blobs.createJSONBlobFromValue(fsItem);
    }

}
