/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.csv.core.CSVImporterOptions.ImportMode;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
public class TestCSVImporterFailOnMissingHeader extends AbstractCSVImporterTest {

    private static final String DOCS_WITH_MISSING_HEADER = "docs_with_missing_header.csv";

    @Test
    public void shouldFail() throws Exception {
        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.IMPORT).build();
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_MISSING_HEADER), options);

        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        CSVImportStatus status = csvImporter.getImportStatus(importId);
        assertTrue(status.isComplete());
        assertEquals(0L, status.getNumberOfProcessedDocument());
        assertEquals(0L, status.getTotalNumberOfDocument());

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(1, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        String error = importLog.getMessage();
        assertTrue(error, error.startsWith("Invalid CSV file: A header name is missing in"));
    }
}
