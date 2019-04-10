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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
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
public class TestCSVImporterImportMode extends AbstractCSVImporterTest {

    private static final String DOCS_WITH_CREATOR_CSV = "docs_with_creator.csv";

    @Test
    public void shouldImportAllDocuments() throws InterruptedException, IOException {

        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.IMPORT).build();
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_CREATOR_CSV),
                options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(2, importLogs.size());
        CSVImportLog importLog;
        for (int i = 0; i < 2; i++) {
            importLog = importLogs.get(i);
            assertEquals(i + 2, importLog.getLine());
            assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        }

        assertTrue(session.exists(new PathRef("/myfile")));
        DocumentModel doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("leela", doc.getPropertyValue("dc:creator"));
        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(1, contributors.size());
        assertTrue(contributors.contains("leela"));
        assertFalse(contributors.contains("Administrator"));
        Calendar creationDate = (Calendar) doc.getPropertyValue("dc:created");
        assertEquals("12/12/2012", new SimpleDateFormat(options.getDateFormat()).format(creationDate.getTime()));
        assertEquals(null, doc.getPropertyValue("dc:modified"));

        assertTrue(session.exists(new PathRef("/myfile2")));
        doc = session.getDocument(new PathRef("/myfile2"));
        assertEquals("leela", doc.getPropertyValue("dc:creator"));
        contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(3, contributors.size());
        assertTrue(contributors.contains("contributor1"));
        assertTrue(contributors.contains("contributor2"));
        assertTrue(contributors.contains("leela"));
        assertFalse(contributors.contains("Administrator"));
        creationDate = (Calendar) doc.getPropertyValue("dc:created");
        assertEquals("12/12/2012", new SimpleDateFormat(options.getDateFormat()).format(creationDate.getTime()));
        Calendar modificationDate = (Calendar) doc.getPropertyValue("dc:modified");
        assertEquals("04/12/2015", new SimpleDateFormat(options.getDateFormat()).format(modificationDate.getTime()));
    }
}
