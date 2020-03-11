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
 *     Arnaud Kervern
 */
package org.nuxeo.snapshot.operation;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.snapshot.Snapshotable;

import static org.nuxeo.ecm.automation.core.Constants.CAT_DOCUMENT;
import static org.nuxeo.ecm.core.api.VersioningOption.MINOR;

@Operation(id = CreateTreeSnapshot.ID, category = CAT_DOCUMENT, label = "Create snapshot", description = "Create a tree snapshot, input document must be eligible to Snapshotable adapter and output will the snapshot")
public class CreateTreeSnapshot {
    public static final String ID = "Document.CreateTreeSnapshot";

    @Param(name = "versioning option", required = false)
    String versioningOption = MINOR.name();

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel doc) {
        Snapshotable adapter = doc.getAdapter(Snapshotable.class);
        if (adapter == null) {
            throw new NuxeoException("Unable to get Snapshotable adapter with document: " + doc.getPathAsString());
        }
        return adapter.createSnapshot(VersioningOption.valueOf(versioningOption)).getDocument();
    }
}
