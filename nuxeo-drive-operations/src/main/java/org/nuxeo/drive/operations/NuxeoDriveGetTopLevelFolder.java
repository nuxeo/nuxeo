/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.runtime.api.Framework;

/**
 * Gets the top level {@link FolderItem} for the currently authenticated user.
 *
 * @author Olivier Grisel
 */
@Operation(id = NuxeoDriveGetTopLevelFolder.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get the top level folder")
public class NuxeoDriveGetTopLevelFolder {

    public static final String ID = "NuxeoDrive.GetTopLevelFolder";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public Blob run() throws IOException {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        FolderItem topLevelFolder = fileSystemItemManager.getTopLevelFolder(ctx.getPrincipal());
        return Blobs.createJSONBlobFromValue(topLevelFolder);
    }

}
