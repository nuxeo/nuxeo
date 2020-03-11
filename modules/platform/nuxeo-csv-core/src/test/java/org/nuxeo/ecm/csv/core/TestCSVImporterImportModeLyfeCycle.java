/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.csv.core.CSVImporterOptions.ImportMode;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
public class TestCSVImporterImportModeLyfeCycle extends AbstractCSVImporterTest {

    private static final String DOCS_WITH_LIFECYCLE_CSV = "docs_with_lifecycle.csv";

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void shouldImportLifeCycle() throws InterruptedException, IOException {

        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.IMPORT).build();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_LIFECYCLE_CSV), options);

        txFeature.nextTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(1, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(2, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());

        DocumentModel doc = session.getDocument(new PathRef("/myfile"));

        assertEquals("obsolete", doc.getCurrentLifeCycleState());
    }
}
