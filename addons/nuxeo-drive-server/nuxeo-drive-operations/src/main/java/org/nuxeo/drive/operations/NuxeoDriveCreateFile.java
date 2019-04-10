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
 * Creates a document from the input blob in the container backing the {@link FileSystemItem} with the given id.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveCreateFile.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Create file", description = "Create a document from the input blob in the container backing the file system item with the given id." //
        + " Return the file system item backed by the created document as a JSON blob.")
public class NuxeoDriveCreateFile {

    public static final String ID = "NuxeoDrive.CreateFile";

    @Context
    protected OperationContext ctx;

    @Param(name = "parentId", description = "Id of the file system item backed by the parent container.")
    protected String parentId;

    /**
     * @since 9.1
     */
    @Param(name = "overwrite", required = false, description = "Optional, whether to overwrite an existing document with the same title.", values = "false")
    protected boolean overwrite;

    @OperationMethod
    public Blob run(Blob blob) throws ParseException, IOException {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        NuxeoDriveOperationHelper.normalizeMimeTypeAndEncoding(blob);
        FileItem fileItem = fileSystemItemManager.createFile(parentId, blob, ctx.getPrincipal(), overwrite);
        return Blobs.createJSONBlobFromValue(fileItem);
    }

}
