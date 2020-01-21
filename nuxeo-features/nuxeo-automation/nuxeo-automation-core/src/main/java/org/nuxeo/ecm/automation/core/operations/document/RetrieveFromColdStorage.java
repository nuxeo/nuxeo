/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.automation.core.operations.document;

import static org.nuxeo.ecm.core.blob.ColdStorageHelper.retrieveContentFromColdStorage;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Retrieve the cold storage content associated with the input {@link DocumentModel}.
 *
 * @since 11.1
 */
@Operation(id = RetrieveFromColdStorage.ID, category = Constants.CAT_BLOB, label = "Retrieve from Cold Storage", description = "Retrieve the cold storage content associated with the document.")
public class RetrieveFromColdStorage {

    public static final String ID = "Document.RetrieveFromColdStorage";

    @Param(name = "numberOfDaysOfAvailability", description = "The number of days that you want your cold storage content to be accessible.")
    protected int numberOfDaysOfAvailability;

    @Context
    protected CoreSession session;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        return retrieveContentFromColdStorage(session, doc.getRef(), numberOfDaysOfAvailability);
    }
}
