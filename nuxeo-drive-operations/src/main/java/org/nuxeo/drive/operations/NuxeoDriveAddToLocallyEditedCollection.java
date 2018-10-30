/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.drive.service.NuxeoDriveManager;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.api.Framework;

/**
 * Adds the given {@link DocumentModel} to the {@link NuxeoDriveManager#LOCALLY_EDITED_COLLECTION_NAME} collection.
 *
 * @author Antoine Taillefer
 * @since 6.0
 * @deprecated since 10.3
 */
@Deprecated
@Operation(id = NuxeoDriveAddToLocallyEditedCollection.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Add document to the 'Locally Edited' collection", deprecatedSince = "10.3")
@Operation(id = NuxeoDriveAddToLocallyEditedCollection.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Add document to the 'Locally Edited' collection")
public class NuxeoDriveAddToLocallyEditedCollection {

    public static final String ID = "NuxeoDrive.AddToLocallyEditedCollection";

    @Context
    protected CoreSession session;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        NuxeoDriveManager nuxeoDriveManager = Framework.getService(NuxeoDriveManager.class);
        nuxeoDriveManager.addToLocallyEditedCollection(session, doc);
        return doc;
    }

}
