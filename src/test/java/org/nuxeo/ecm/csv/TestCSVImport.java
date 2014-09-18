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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@Deploy({ "org.nuxeo.ecm.csv", "org.nuxeo.runtime.datasource",
        "org.nuxeo.ecm.platform.types.api", "org.nuxeo.ecm.platform.types.core" })
@LocalDeploy("org.nuxeo.ecm.csv:test-ui-types-contrib.xml")
public class TestCSVImport {

    private static final String DOCS_OK_CSV = "docs_ok.csv";

    private static final String DOCS_WITH_FOLDERS_OK_CSV = "docs_with_folders_ok.csv";

    private static final String DOCS_NOT_OK_CSV = "docs_not_ok.csv";

    @Inject
    protected CoreSession session;

    @Inject
    protected CSVImporter csvImporter;

    @Inject
    protected WorkManager workManager;

    @Before
    public void clearWorkQueue() {
        workManager.clearCompletedWork(0);
    }

    private File getCSVFile(String name) {
        return new File(FileUtils.getResourcePathFromContext(name));
    }

    @Test
    public void shouldCreateAllDocuments() throws InterruptedException,
            ClientException {
        CSVImporterOptions options = CSVImporterOptions.DEFAULT_OPTIONS;
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/",
                getCSVFile(DOCS_OK_CSV), DOCS_OK_CSV, options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(2, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(1, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        importLog = importLogs.get(1);
        assertEquals(2, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());

        assertTrue(session.exists(new PathRef("/myfile")));
        DocumentModel doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("My File", doc.getTitle());
        assertEquals("a simple file", doc.getPropertyValue("dc:description"));
        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(3, contributors.size());
        assertTrue(contributors.contains("contributor1"));
        assertTrue(contributors.contains("contributor2"));
        assertTrue(contributors.contains("contributor3"));
        Calendar issueDate = (Calendar) doc.getPropertyValue("dc:issued");
        assertEquals(
                "10/01/2010",
                new SimpleDateFormat(options.getDateFormat()).format(issueDate.getTime()));

        assertTrue(session.exists(new PathRef("/mynote")));
        doc = session.getDocument(new PathRef("/mynote"));
        assertEquals("My Note", doc.getTitle());
        assertEquals("a simple note", doc.getPropertyValue("dc:description"));
        assertEquals("note content", doc.getPropertyValue("note:note"));
        contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(3, contributors.size());
        assertTrue(contributors.contains("bender"));
        assertTrue(contributors.contains("leela"));
        assertTrue(contributors.contains("fry"));
        issueDate = (Calendar) doc.getPropertyValue("dc:issued");
        assertEquals(
                "12/12/2012",
                new SimpleDateFormat(options.getDateFormat()).format(issueDate.getTime()));
    }

    @Test
    public void shouldSkipExistingDocuments() throws InterruptedException,
            ClientException {
        DocumentModel doc = session.createDocumentModel("/", "mynote", "Note");
        doc.setPropertyValue("dc:title", "Existing Note");
        session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();

        CSVImporterOptions options = new CSVImporterOptions.Builder().updateExisting(
                false).build();
        String importId = csvImporter.launchImport(session, "/",
                getCSVFile(DOCS_OK_CSV), DOCS_OK_CSV, options);

        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(2, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(1, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());
        importLog = importLogs.get(1);
        assertEquals(2, importLog.getLine());
        assertEquals(CSVImportLog.Status.SKIPPED, importLog.getStatus());
        assertEquals("Document already exists", importLog.getMessage());

        assertTrue(session.exists(new PathRef("/myfile")));
        doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("My File", doc.getTitle());
        assertEquals("a simple file", doc.getPropertyValue("dc:description"));

        assertTrue(session.exists(new PathRef("/mynote")));
        doc = session.getDocument(new PathRef("/mynote"));
        assertEquals("Existing Note", doc.getTitle());
        assertFalse("a simple note".equals(doc.getPropertyValue("dc:description")));
    }

    @Test
    public void shouldStoreLineWithErrors() throws InterruptedException,
            ClientException {
        CSVImporterOptions options = new CSVImporterOptions.Builder().updateExisting(
                false).build();
        TransactionHelper.commitOrRollbackTransaction();
        String importId = csvImporter.launchImport(session, "/",
                getCSVFile(DOCS_NOT_OK_CSV), DOCS_NOT_OK_CSV, options);
        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(5, importLogs.size());

        CSVImportLog importLog = importLogs.get(0);
        assertEquals(1, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals(
                "Unable to convert field 'dc:issued' with value '10012010'",
                importLog.getMessage());
        importLog = importLogs.get(1);
        assertEquals(2, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());
        importLog = importLogs.get(2);
        assertEquals(3, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("The type 'NotExistingType' does not exist",
                importLog.getMessage());
        importLog = importLogs.get(3);
        assertEquals(4, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());
        importLog = importLogs.get(4);
        assertEquals(5, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("'Domain' type is not allowed in 'Root'",
                importLog.getMessage());

        assertFalse(session.exists(new PathRef("/myfile")));
        assertTrue(session.exists(new PathRef("/mynote")));
        assertFalse(session.exists(new PathRef("/nonexisting")));
        assertTrue(session.exists(new PathRef("/mynote2")));
    }

    @Test
    public void shouldImportDirectoryStructure() throws InterruptedException,
            ClientException {
        CSVImporterOptions options = new CSVImporterOptions.Builder().updateExisting(
                false).build();
        TransactionHelper.commitOrRollbackTransaction();
        String importId = csvImporter.launchImport(session, "/",
                getCSVFile(DOCS_WITH_FOLDERS_OK_CSV), DOCS_WITH_FOLDERS_OK_CSV,
                options);
        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(5, importLogs.size());

        for (int i = 0; i < 4; i++) {
            assertEquals(CSVImportLog.Status.SUCCESS,
                    importLogs.get(i).getStatus());
        }
        CSVImportLog importLog = importLogs.get(4);
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("Parent document '/folder/folder' does not exist",
                importLog.getMessage());

        assertTrue(session.exists(new PathRef("/folder")));
        assertTrue(session.exists(new PathRef("/folder/doc1")));
        assertTrue(session.exists(new PathRef("/folder/subfolder")));
        assertTrue(session.exists(new PathRef("/folder/subfolder/doc2")));
        assertFalse(session.exists(new PathRef("/folder/folder/doc3")));

        DocumentModel doc = session.getDocument(new PathRef("/folder"));
        assertEquals("Folder", doc.getType());
        doc = session.getDocument(new PathRef("/folder/doc1"));
        assertEquals("File", doc.getType());
        doc = session.getDocument(new PathRef("/folder/subfolder"));
        assertEquals("Folder", doc.getType());
        doc = session.getDocument(new PathRef("/folder/subfolder/doc2"));
        assertEquals("File", doc.getType());
    }

}
