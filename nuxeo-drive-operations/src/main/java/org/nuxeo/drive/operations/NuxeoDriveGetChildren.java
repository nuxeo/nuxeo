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
import java.util.List;

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
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
 * Gets the children of the document backing the {@link FolderItem} with the given id.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveGetChildren.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get children", description = "Get the children of the document backing the folder item with the given id." //
        + " Return the results as a JSON blob.")
public class NuxeoDriveGetChildren {

    public static final String ID = "NuxeoDrive.GetChildren";

    @Context
    protected OperationContext ctx;

    @Param(name = "id", description = "Id of the file system item backed by the document whose children to get.")
    protected String id;

    @OperationMethod
    public Blob run() throws IOException {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        List<FileSystemItem> children = fileSystemItemManager.getChildren(id, ctx.getPrincipal());
        return Blobs.createJSONBlobFromValue(children);
    }

}
