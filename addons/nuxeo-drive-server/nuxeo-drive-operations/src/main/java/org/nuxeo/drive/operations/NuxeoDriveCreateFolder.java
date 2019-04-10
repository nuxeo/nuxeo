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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

/**
 * Creates a folder with the given name in the {@link FileSystemItem} with the given id for the currently authenticated
 * user.
 * 
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveCreateFolder.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Create folder")
public class NuxeoDriveCreateFolder {

    public static final String ID = "NuxeoDrive.CreateFolder";

    @Context
    protected OperationContext ctx;

    @Param(name = "parentId")
    protected String parentId;

    @Param(name = "name")
    protected String name;

    @Param(name = "overwrite", required = false)
    protected boolean overwrite;

    @OperationMethod
    public Blob run() throws ClientException, IOException {

        FileSystemItemManager fileSystemItemManager = Framework.getLocalService(FileSystemItemManager.class);
        FolderItem folderItem = fileSystemItemManager.createFolder(parentId, name, ctx.getPrincipal(), overwrite);

        return NuxeoDriveOperationHelper.asJSONBlob(folderItem);
    }

}
