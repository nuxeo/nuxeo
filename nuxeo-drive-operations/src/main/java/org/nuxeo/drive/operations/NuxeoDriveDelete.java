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

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.automation.InvalidOperationException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.runtime.api.Framework;

/**
 * Deletes the document backing the {@link FileSystemItem} with the given id.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveDelete.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Delete", description = "Delete the document backing the file system item with the given id.")
public class NuxeoDriveDelete {

    public static final String ID = "NuxeoDrive.Delete";

    @Context
    protected OperationContext ctx;

    @Param(name = "id", description = "Id of the file system item backed by the document to delete.")
    protected String id;

    /**
     * @since 6.0
     */
    @Param(name = "parentId", required = false, description = "Optional id of the file system item backed by the parent container of the document to delete." //
            + " For optimization purpose.")
    protected String parentId;

    @OperationMethod
    public void run() throws InvalidOperationException {
        FileSystemItemManager fileSystemItemManager = Framework.getService(FileSystemItemManager.class);
        try {
            if (parentId == null) {
                fileSystemItemManager.delete(id, ctx.getPrincipal());
            } else {
                fileSystemItemManager.delete(id, parentId, ctx.getPrincipal());
            }

        } catch (UnsupportedOperationException e) {
            throw new InvalidOperationException(e);
        }
    }

}
