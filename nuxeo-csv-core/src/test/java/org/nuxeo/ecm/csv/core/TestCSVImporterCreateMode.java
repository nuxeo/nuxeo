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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.csv.core.CSVImporterOptions.ImportMode;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
public class TestCSVImporterCreateMode extends AbstractCSVImporterTest {

    private static final String DOCS_OK_CSV = "docs_ok.csv";

    private static final String DOCS_WITH_FOLDERS_OK_CSV = "docs_with_folders_ok.csv";

    private static final String DOCS_NOT_OK_CSV = "docs_not_ok.csv";

    private static final String DOCS_WITH_BOM_CSV = "docs_with_bom.csv";

    private static final String DOCS_WITH_INTERSPERSED_COMMENTS_CSV = "docs_with_interspersed_comments.csv";

    private static final String DOCS_WITH_LIFECYCLE_CSV = "docs_with_lifecycle.csv";

    private static final String DOCS_WITHOUT_CONTRIBUTORS_CSV = "docs_without_contributors.csv";

    private static final String DOCS_WITH_CREATOR_CSV = "docs_with_creator.csv";

    private static final String DOCS_WITHOUT_TYPE_CSV = "docs_without_type.csv";

    private static final String DOCS_WITH_MISSING_TYPE_CSV = "docs_with_missing_type.csv";

    private static final String DOCS_WITH_BLOBS_CSV = "docs_with_blobs.csv";

    @Inject
    protected CoreFeature coreFeature;

    @Before
    public void before() {
        String blobsFolder = FileUtils.getResourcePathFromContext("files");
        Framework.getProperties().put(CSVImporterWork.NUXEO_CSV_BLOBS_FOLDER, blobsFolder);
    }

    @After
    public void after() {
        Framework.getProperties().remove(CSVImporterWork.NUXEO_CSV_BLOBS_FOLDER);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateAllDocuments() throws InterruptedException, IOException {
        assertNotNull(csvImporter);

        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.CREATE).build();

        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_OK_CSV), options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        CSVImportStatus importStatus = csvImporter.getImportStatus(importId);
        assertNotNull(importStatus);
        assertTrue(importStatus.isComplete());

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
        assertEquals("My File", doc.getTitle());
        assertEquals("a simple file", doc.getPropertyValue("dc:description"));
        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(4, contributors.size());
        assertTrue(contributors.contains("contributor1"));
        assertTrue(contributors.contains("contributor2"));
        assertTrue(contributors.contains("contributor3"));
        assertTrue(contributors.contains("Administrator"));
        Calendar issueDate = (Calendar) doc.getPropertyValue("dc:issued");
        assertEquals("10/01/2010", new SimpleDateFormat(options.getDateFormat()).format(issueDate.getTime()));

        assertTrue(session.exists(new PathRef("/mynote")));
        doc = session.getDocument(new PathRef("/mynote"));
        assertEquals("My Note", doc.getTitle());
        assertEquals("a simple note", doc.getPropertyValue("dc:description"));
        assertEquals("note content", doc.getPropertyValue("note:note"));
        contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(4, contributors.size());
        assertTrue(contributors.contains("bender"));
        assertTrue(contributors.contains("leela"));
        assertTrue(contributors.contains("fry"));
        assertTrue(contributors.contains("Administrator"));
        issueDate = (Calendar) doc.getPropertyValue("dc:issued");
        assertEquals("12/12/2012", new SimpleDateFormat(options.getDateFormat()).format(issueDate.getTime()));

        assertTrue(session.exists(new PathRef("/mycomplexfile")));
        doc = session.getDocument(new PathRef("/mycomplexfile"));
        assertEquals("My Complex File", doc.getTitle());
        assertEquals("a complex file", doc.getPropertyValue("dc:description"));
        contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(2, contributors.size());
        assertTrue(contributors.contains("joe"));
        assertTrue(contributors.contains("Administrator"));
        issueDate = (Calendar) doc.getPropertyValue("dc:issued");
        assertEquals("12/21/2013", new SimpleDateFormat(options.getDateFormat()).format(issueDate.getTime()));
        HashMap<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("stringProp", "testString1");
        expectedMap.put("dateProp", null);
        expectedMap.put("boolProp", true);
        expectedMap.put("enumProp", null);
        expectedMap.put("arrayProp", new String[] { "1" });
        expectedMap.put("intProp", null);
        expectedMap.put("floatProp", null);
        expectedMap.put("contentProp", null);
        Map<String, Object> resultMap = (Map<String, Object>) doc.getPropertyValue("complexTest:complexItem");
        assertEquals("1", ((String[]) resultMap.get("arrayProp"))[0]);
        expectedMap.put("arrayProp", null);
        resultMap.put("arrayProp", null);
        assertEquals(expectedMap, resultMap);
        List<Map> resultMapList = (List<Map>) doc.getPropertyValue("complexTest:listItem");
        assertEquals(2, resultMapList.size());
        resultMap = resultMapList.get(0);
        assertEquals("1", ((String[]) resultMap.get("arrayProp"))[0]);
        resultMap.put("arrayProp", null);
        assertEquals(expectedMap, resultMap);
        resultMap = resultMapList.get(1);
        assertEquals("1", ((String[]) resultMap.get("arrayProp"))[0]);
        expectedMap.put("stringProp", "testString2");
        resultMap.put("arrayProp", null);
        assertEquals(expectedMap, resultMap);
    }

    @Test
    public void shouldSkipExistingDocuments() throws InterruptedException, IOException {
        DocumentModel doc = session.createDocumentModel("/", "mynote", "Note");
        doc.setPropertyValue("dc:title", "Existing Note");
        session.createDocument(doc);
        TransactionHelper.commitOrRollbackTransaction();

        CSVImporterOptions options = new CSVImporterOptions.Builder().updateExisting(false).build();
        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_OK_CSV), options);

        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(3, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(2, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());
        importLog = importLogs.get(1);
        assertEquals(3, importLog.getLine());
        assertEquals(CSVImportLog.Status.SKIPPED, importLog.getStatus());
        assertEquals("Document already exists", importLog.getMessage());
        importLog = importLogs.get(2);
        assertEquals(4, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());

        assertTrue(session.exists(new PathRef("/myfile")));
        doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("My File", doc.getTitle());
        assertEquals("a simple file", doc.getPropertyValue("dc:description"));

        assertTrue(session.exists(new PathRef("/mynote")));
        doc = session.getDocument(new PathRef("/mynote"));
        assertEquals("Existing Note", doc.getTitle());
        assertFalse("a simple note".equals(doc.getPropertyValue("dc:description")));

        assertTrue(session.exists(new PathRef("/mycomplexfile")));
        doc = session.getDocument(new PathRef("/mycomplexfile"));
        assertEquals("My Complex File", doc.getTitle());
        assertEquals("a complex file", doc.getPropertyValue("dc:description"));
    }

    @Test
    public void shouldUpdateDocumentsWithoutType() throws InterruptedException, IOException {
        doUpdateDocumentsWithNoType(DOCS_WITHOUT_TYPE_CSV);
    }

    @Test
    public void shouldUpdateDocumentsWithMissingType() throws InterruptedException, IOException {
        doUpdateDocumentsWithNoType(DOCS_WITH_MISSING_TYPE_CSV);
    }

    private void doUpdateDocumentsWithNoType(String csvFileName) throws InterruptedException, IOException {
        DocumentModel doc = session.createDocumentModel("/", "mynote", "Note");
        doc.setPropertyValue("dc:title", "Existing Note");
        session.createDocument(doc);

        doc = session.createDocumentModel("/", "myfile", "File");
        doc.setPropertyValue("dc:title", "Existing File");
        session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();

        CSVImporterOptions options = new CSVImporterOptions.Builder().updateExisting(true).build();
        String importId = csvImporter.launchImport(session, "/", getCSVBlob(csvFileName), options);

        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(2, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(2, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document updated", importLog.getMessage());
        importLog = importLogs.get(1);
        assertEquals(3, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document updated", importLog.getMessage());

        assertTrue(session.exists(new PathRef("/myfile")));
        doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("My File", doc.getTitle());
        assertEquals("a simple file", doc.getPropertyValue("dc:description"));

        assertTrue(session.exists(new PathRef("/mynote")));
        doc = session.getDocument(new PathRef("/mynote"));
        assertEquals("My Note", doc.getTitle());
        assertEquals("a simple note", doc.getPropertyValue("dc:description"));
    }

    @Test
    public void shouldStoreLineWithErrors() throws InterruptedException, IOException {
        CSVImporterOptions options = new CSVImporterOptions.Builder().updateExisting(false).build();
        TransactionHelper.commitOrRollbackTransaction();
        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_NOT_OK_CSV), options);
        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(7, importLogs.size());

        CSVImportLog importLog = importLogs.get(0);
        assertEquals(2, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("Unable to convert field 'dc:issued' with value '10012010'", importLog.getMessage());
        importLog = importLogs.get(1);
        assertEquals(3, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());
        importLog = importLogs.get(2);
        assertEquals(4, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("The type 'NotExistingType' does not exist", importLog.getMessage());
        importLog = importLogs.get(3);
        assertEquals(5, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());
        importLog = importLogs.get(4);
        assertEquals(6, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("'Domain' type is not allowed in 'Root'", importLog.getMessage());
        importLog = importLogs.get(5);
        assertEquals(7, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals(
                "Unable to convert field 'complexTest:complexItem' with value "
                        + "'{\"arrayProp\":[\"1\"],\"boolProp\":invalidBooleanValue,\"stringProp\":\"testString1\"}'",
                importLog.getMessage());
        importLog = importLogs.get(6);
        assertEquals(8, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals(
                "Unable to convert field 'complexTest:complexItem' with value "
                        + "'{\"dateProp\":\"2009-02-13BAD04:40:00.00Z\"],\"boolProp\":true,\"stringProp\":\"testString1\"}'",
                importLog.getMessage());

        assertFalse(session.exists(new PathRef("/myfile")));
        assertTrue(session.exists(new PathRef("/mynote")));
        assertFalse(session.exists(new PathRef("/nonexisting")));
        assertTrue(session.exists(new PathRef("/mynote2")));
        assertFalse(session.exists(new PathRef("/picture")));
        assertFalse(session.exists(new PathRef("/mycomplexfile")));
        assertFalse(session.exists(new PathRef("/mycomplexfile2")));
    }

    @Test
    public void shouldImportDirectoryStructure() throws InterruptedException, IOException {
        CSVImporterOptions options = new CSVImporterOptions.Builder().updateExisting(false).build();
        TransactionHelper.commitOrRollbackTransaction();
        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_FOLDERS_OK_CSV), options);
        workManager.awaitCompletion(10, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(5, importLogs.size());

        for (int i = 0; i < 4; i++) {
            assertEquals(CSVImportLog.Status.SUCCESS, importLogs.get(i).getStatus());
        }
        CSVImportLog importLog = importLogs.get(4);
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("Parent document '/folder/folder' does not exist", importLog.getMessage());

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

    @Test
    public void shouldImportCSVFileWithBOM() throws InterruptedException, IOException {
        CSVImporterOptions options = CSVImporterOptions.DEFAULT_OPTIONS;
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_BOM_CSV), options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(1, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());

        assertTrue(session.exists(new PathRef("/afile")));
        DocumentModel doc = session.getDocument(new PathRef("/afile"));
        assertEquals("Un été à Paris", doc.getTitle());
    }

    @Test
    public void shouldNotImportCSVFileWithInitialCommentByDefault() throws InterruptedException, IOException {
        CSVImporterOptions options = CSVImporterOptions.DEFAULT_OPTIONS;
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_INTERSPERSED_COMMENTS_CSV),
                options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(1, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
    }

    @Test
    public void shouldImportCSVFileWithCommentsWhenEnabled() throws InterruptedException, IOException {
        CSVImporterOptions options = new CSVImporterOptions.Builder().commentMarker('#').build();
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_INTERSPERSED_COMMENTS_CSV),
                options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(2, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());

        assertTrue(session.exists(new PathRef("/myfile")));
        DocumentModel doc = session.getDocument(new PathRef("/myfile"));

        assertTrue(session.exists(new PathRef("/myfile2")));
        doc = session.getDocument(new PathRef("/myfile2"));
    }

    @Test
    public void shouldCreateDocumentWithGivenLifeCycleState() throws InterruptedException, IOException {
        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.CREATE).build();
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_LIFECYCLE_CSV), options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(1, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());

        assertTrue(session.exists(new PathRef("/myfile")));
        DocumentModel doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("obsolete", doc.getCurrentLifeCycleState());
    }

    @Test
    @Ignore("NXP-22172")
    public void shouldSetCreatorToTheUserImporting() throws InterruptedException {
        // give access to leela
        DocumentModel root = session.getRootDocument();
        ACP acp = root.getACP();
        acp.addACE(ACL.LOCAL_ACL, ACE.builder("leela", "ReadWrite").build());
        session.setACP(root.getRef(), acp, true);

        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.CREATE).build();

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        try (CloseableCoreSession leelaSession = openSessionAs("leela")) {
            String importId = csvImporter.launchImport(leelaSession, "/", getCSVBlob(DOCS_WITHOUT_CONTRIBUTORS_CSV),
                    options);

            workManager.awaitCompletion(10000, TimeUnit.SECONDS);

            List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
            assertEquals(2, importLogs.size());
        } catch (IOException e) {
            e.printStackTrace();
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        assertTrue(session.exists(new PathRef("/myfile")));
        DocumentModel doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("leela", doc.getPropertyValue("dc:creator"));
        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(1, contributors.size());
        assertTrue(contributors.contains("leela"));
    }

    @Test
    public void shouldCreateDocumentWithDefinedCreator() throws InterruptedException, IOException {
        CSVImporterOptions options = new CSVImporterOptions.Builder().importMode(ImportMode.CREATE).build();
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_CREATOR_CSV), options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(2, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());

        assertTrue(session.exists(new PathRef("/myfile")));
        DocumentModel doc = session.getDocument(new PathRef("/myfile"));
        assertEquals("leela", doc.getPropertyValue("dc:creator"));
        List<String> contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(2, contributors.size());
        assertTrue(contributors.contains("leela"));
        assertTrue(contributors.contains("Administrator"));

        assertTrue(session.exists(new PathRef("/myfile2")));
        doc = session.getDocument(new PathRef("/myfile2"));
        assertEquals("leela", doc.getPropertyValue("dc:creator"));
        contributors = Arrays.asList((String[]) doc.getPropertyValue("dc:contributors"));
        assertEquals(4, contributors.size());
        assertTrue(contributors.contains("contributor1"));
        assertTrue(contributors.contains("contributor2"));
        assertTrue(contributors.contains("leela"));
        assertTrue(contributors.contains("Administrator"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldImportCSVFileWithBlobs() throws InterruptedException, IOException {
        CSVImporterOptions options = CSVImporterOptions.DEFAULT_OPTIONS;
        TransactionHelper.commitOrRollbackTransaction();

        String importId = csvImporter.launchImport(session, "/", getCSVBlob(DOCS_WITH_BLOBS_CSV), options);

        workManager.awaitCompletion(10000, TimeUnit.SECONDS);
        TransactionHelper.startTransaction();

        List<CSVImportLog> importLogs = csvImporter.getImportLogs(importId);
        assertEquals(2, importLogs.size());
        CSVImportLog importLog = importLogs.get(0);
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        importLog = importLogs.get(1);
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());

        assertTrue(session.exists(new PathRef("/textfile")));
        DocumentModel doc = session.getDocument(new PathRef("/textfile"));
        assertEquals("a simple file", doc.getTitle());
        Blob blob = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        assertEquals("text_file.txt", blob.getFilename());
        assertEquals("A simple text file", blob.getString());
        List<Map<String, Serializable>> files = (List<Map<String, Serializable>>) doc.getPropertyValue("files:files");
        assertNotNull(files);
        assertEquals(1, files.size());
        Map<String, Serializable> map = files.get(0);
        assertNotNull(map);
        blob = (Blob) map.get("file");
        assertEquals("text_file.txt", blob.getFilename());
        assertEquals("A simple text file", blob.getString());

        assertTrue(session.exists(new PathRef("/complexfile")));
        doc = session.getDocument(new PathRef("/complexfile"));
        assertEquals("a complex file", doc.getTitle());
        Map<String, Serializable> complexItem = (Map<String, Serializable>) doc.getPropertyValue(
                "complexTest:complexItem");
        assertNotNull(complexItem);
        blob = (Blob) complexItem.get("contentProp");
        assertNotNull(blob);
        assertEquals("foo.txt", blob.getFilename());
        assertEquals("A simple text file", blob.getString());
    }

    public CloseableCoreSession openSessionAs(String username) {
        return coreFeature.openCoreSession(username);
    }

}
