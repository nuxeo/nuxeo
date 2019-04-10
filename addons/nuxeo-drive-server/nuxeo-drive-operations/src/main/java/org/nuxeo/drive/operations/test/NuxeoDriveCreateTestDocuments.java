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
package org.nuxeo.drive.operations.test;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
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
            StreamingBlob content = StreamingBlob.createFromString(String.format(contentPattern, i));
            content.setFilename(name);
            fileManager.createDocumentFromBlob(session, content, parent.getPathAsString(), false, name);
            if (delay > 0) {
                Thread.sleep(delay);
            }
        }
        return StreamingBlob.createFromString(number.toString(), "text/plain");
    }
}
