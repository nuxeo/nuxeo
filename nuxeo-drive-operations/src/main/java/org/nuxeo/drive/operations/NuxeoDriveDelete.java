/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.nuxeo.drive.adapter.FileSystemItem;
import org.nuxeo.drive.service.FileSystemItemManager;
import org.nuxeo.ecm.automation.InvalidOperationException;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.runtime.api.Framework;

/**
 * Deletes the {@link FileSystemItem} with the given id for the currently
 * authenticated user.
 *
 * @author Antoine Taillefer
 */
@Operation(id = NuxeoDriveDelete.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Delete")
public class NuxeoDriveDelete {

    public static final String ID = "NuxeoDrive.Delete";

    @Context
    protected OperationContext ctx;

    @Param(name = "id")
    protected String id;

    /**
     * @since 5.9.6
     */
    @Param(name = "parentId", required = false)
    protected String parentId;

    @OperationMethod
    public void run() throws ClientException, InvalidOperationException {

        FileSystemItemManager fileSystemItemManager = Framework.getLocalService(FileSystemItemManager.class);
        try {
            if (parentId == null) {
                fileSystemItemManager.delete(id, ctx.getPrincipal());
            } else {
                fileSystemItemManager.delete(id, parentId, ctx.getPrincipal());
            }

        } catch (UnsupportedOperationException e) {
            throw new InvalidOperationException(e);
        }

        // Commit transaction explicitly to ensure client-side consistency
        // TODO: remove when https://jira.nuxeo.com/browse/NXP-10964 is fixed
        NuxeoDriveOperationHelper.commitAndReopenTransaction();
    }

}
