/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.csv.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.csv.core.CSVImporterWork.NUXEO_CSV_IMPORTER_MAX_LINES_PROP_NAME;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.csv.core.CSVImporterOptions.ImportMode;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 2025.21
 */
@RunWith(FeaturesRunner.class)
public class TestCSVImporterFailOnNbLinesExceeded extends AbstractCSVImporterTest {

    private static final String DOCS_NB_LINES_EXCEEDED_CSV = "docs_ok_big.csv";

    private static final String LIMIT = "10";

    @Test
    @WithFrameworkProperty(name = NUXEO_CSV_IMPORTER_MAX_LINES_PROP_NAME, value = LIMIT)
    public void shouldFail() throws Exception {
        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.IMPORT).build();
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_NB_LINES_EXCEEDED_CSV), options);

        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        CSVImportStatus status = csvImporter.getImportStatus(importId);
        assertTrue(status.isComplete());
        assertEquals(0L, status.getNumberOfProcessedDocument());
        assertEquals(0L, status.getTotalNumberOfDocument());

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(1, importLogs.size());
        CSVImportLog importLog = importLogs.getFirst();
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        String error = importLog.getLocalizedMessage();
        assertEquals(CSVImporterWork.LABEL_CSV_IMPORTER_ERROR_NB_LINES_EXCEEDED, error);
    }
}
