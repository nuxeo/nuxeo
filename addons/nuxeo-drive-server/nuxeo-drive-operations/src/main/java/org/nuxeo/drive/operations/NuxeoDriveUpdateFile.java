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

import javax.mail.internet.ParseException;

import org.nuxeo.drive.adapter.FileItem;
import org.nuxeo.drive.adapter.FileSystemItem;
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
 * Updates the document backing the {@link FileSystemItem} with the given id with the input blob.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveUpdateFile.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Update file", description = "Update the document backing the file system item with the given id with the input blob." //
        + " Return the file system item backed by the updated document as a JSON blob.")
public class NuxeoDriveUpdateFile {

    public static final String ID = "NuxeoDrive.UpdateFile";

    @Context
    protected OperationContext ctx;

    @Param(name = "id", description = "Id of the file system item backed by the document to update.")
    protected String id;

    /**
     * @since 6.0
     */
    @Param(name = "parentId", required = false, description = "Optional id of the file system item backed by the parent container of the document to update." //
            + " For optimization purpose.")
    protected String parentId;

    @OperationMethod
    public Blob run(Blob blob) throws ParseException, IOException {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        NuxeoDriveOperationHelper.normalizeMimeTypeAndEncoding(blob);
        FileItem fileItem;
        if (parentId == null) {
            fileItem = fileSystemItemManager.updateFile(id, blob, ctx.getPrincipal());
        } else {
            fileItem = fileSystemItemManager.updateFile(id, parentId, blob, ctx.getPrincipal());
        }
        return Blobs.createJSONBlobFromValue(fileItem);
    }

}
