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
package org.nuxeo.drive.operations.test;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Create batch of test documents in a single automation query
 *
 * @author Olivier Grisel
 */
@Operation(id = NuxeoDriveCreateTestDocuments.ID, category = Constants.CAT_SERVICES, label = "Nuxeo Drive: Create test documents")
public class NuxeoDriveCreateTestDocuments {

    public static final String ID = "NuxeoDrive.CreateTestDocuments";

    @Context
    protected CoreSession session;

    @Param(name = "namePattern", required = false)
    protected String namePattern = "file_%03d.txt";

    @Param(name = "contentPattern", required = false)
    protected String contentPattern = "Content for file_%03d.txt";

    @Param(name = "number", required = false)
    protected Integer number = 10;

    // delay in ms between two consecutive document creations to
    // artificially space the events in the logs (e.g. simulating long
    // operations such as S3BinaryManager blob uploads).
    @Param(name = "delay", required = false)
    protected long delay = 1000L;

    @OperationMethod
    public Blob run(DocumentModel parent) throws Exception {
        NuxeoDriveIntegrationTestsHelper.checkOperationAllowed();
        FileManager fileManager = Framework.getLocalService(FileManager.class);
        for (int i = 0; i < number; i++) {
            String name = String.format(namePattern, i);
            Blob content = new StringBlob(String.format(contentPattern, i));
            content.setFilename(name);
            fileManager.createDocumentFromBlob(session, content, parent.getPathAsString(), false, name);
            if (delay > 0) {
                Thread.sleep(delay);
            }
        }
        return new StringBlob(number.toString(), "text/plain");
    }
}
