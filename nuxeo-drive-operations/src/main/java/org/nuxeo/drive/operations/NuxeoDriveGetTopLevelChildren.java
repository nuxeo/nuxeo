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

import java.io.StringWriter;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.runtime.api.Framework;

/**
 * Gets the children of the top level {@link FolderItem} for the currently
 * authenticated user.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveGetTopLevelChildren.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Get top level children")
public class NuxeoDriveGetTopLevelChildren {

    public static final String ID = "NuxeoDrive.GetTopLevelChildren";

    @Context
    protected OperationContext ctx;

    @OperationMethod
    public Blob run() throws Exception {

        FileSystemItemManager fileSystemItemManager = Framework.getLocalService(FileSystemItemManager.class);
        List<FileSystemItem> children = fileSystemItemManager.getTopLevelChildren(ctx.getPrincipal());
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, children);
        return StreamingBlob.createFromString(writer.toString(),
                "application/json");
    }

}
