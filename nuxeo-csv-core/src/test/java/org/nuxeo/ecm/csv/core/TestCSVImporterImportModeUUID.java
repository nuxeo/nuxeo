/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.csv.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.csv.core.CSVImporterOptions.ImportMode;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
public class TestCSVImporterImportModeUUID extends AbstractCSVImporterTest {

    private static final String DOCS_WITH_UUID = "docs_with_uuid.csv";

    @Test
    public void shouldImportAllDocuments() throws InterruptedException, IOException {

        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.IMPORT).build();
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_UUID), options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        assertTrue(session.exists(new PathRef("/myfile")));
        assertTrue(session.exists(new PathRef("/mynote")));
        assertTrue(session.exists(new PathRef("/mycomplexfile")));

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(3, importLogs.size());
        CSVImportLog importLog;
        for (int i = 0; i < 3; i++) {
            importLog = importLogs.get(i);
            assertEquals(i + 2, importLog.getLine());
            assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        }

        assertTrue(session.exists(new PathRef("/myfile")));
        DocumentModel doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("9ed0477f-46c6-4a31-bafd-177a0cdfa772", doc.getId());

        assertTrue(session.exists(new PathRef("/mynote")));
        doc = session.getDocument(new PathRef("/mynote"));
        assertNotEquals(null, doc.getId());

        assertTrue(session.exists(new PathRef("/mycomplexfile")));
        doc = session.getDocument(new PathRef("/mycomplexfile"));
        assertEquals("b2bd65d9-ed48-4d00-a926-21af2a5d9c12", doc.getId());
    }
}
