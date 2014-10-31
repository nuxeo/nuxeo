/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Adds the given {@link DocumentModel} to the
 * {@link NuxeoDriveManager#LOCALLY_EDITED_COLLECTION_NAME} collection.
 *
 * @author Antoine Taillefer
 * @since 6.0
 */
@Operation(id = NuxeoDriveAddToLocallyEditedCollection.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Add document to the 'Locally Edited' collection")
public class NuxeoDriveAddToLocallyEditedCollection {

    public static final String ID = "NuxeoDrive.AddToLocallyEditedCollection";

    @Context
    protected CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws ClientException {
        NuxeoDriveManager nuxeoDriveManager = Framework.getService(NuxeoDriveManager.class);
        nuxeoDriveManager.addToLocallyEditedCollection(session, doc);
        return doc;
    }

}
