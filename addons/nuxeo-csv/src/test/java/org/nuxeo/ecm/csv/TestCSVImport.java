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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.runtime.datasource")
public class TestCSVImport {

    @Inject
    protected CoreSession session;

    private Blob getCSVFile(String name) {
        File file = new File(FileUtils.getResourcePathFromContext(name));
        Blob blob = new FileBlob(file);
        blob.setFilename(file.getName());
        return blob;
    }

    @Test
    public void shouldCreateAllDocuments() throws InterruptedException,
            ClientException {
        CSVImporterOptions options = CSVImporterOptions.DEFAULT_OPTIONS;
        CSVImporter importer = new CSVImporter(options);
        importer.run(session, "/", getCSVFile("docs_ok.csv"));

        Framework.getLocalService(WorkManager.class).awaitCompletion(10,
                TimeUnit.SECONDS);
        session.save();

        List<CSVImportLog> importLogs = importer.getWork().getImportLogs();
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
        session.save();

        CSVImporterOptions options = new CSVImporterOptions.Builder().updateExisting(
                false).build();
        CSVImporter importer = new CSVImporter(options);
        importer.run(session, "/", getCSVFile("docs_ok.csv"));

        Framework.getLocalService(WorkManager.class).awaitCompletion(10,
                TimeUnit.SECONDS);
        session.save();

        List<CSVImportLog> importLogs = importer.getWork().getImportLogs();
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
        CSVImporter importer = new CSVImporter(options);
        importer.run(session, "/", getCSVFile("docs_not_ok.csv"));

        Framework.getLocalService(WorkManager.class).awaitCompletion(10,
                TimeUnit.SECONDS);
        session.save();

        List<CSVImportLog> importLogs = importer.getWork().getImportLogs();
        assertEquals(4, importLogs.size());

        CSVImportLog importLog = importLogs.get(0);
        assertEquals(1, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("Unable to convert field 'dc:issued' with value '10012010'", importLog.getMessage());
        importLog = importLogs.get(1);
        assertEquals(2, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());
        importLog = importLogs.get(2);
        assertEquals(3, importLog.getLine());
        assertEquals(CSVImportLog.Status.ERROR, importLog.getStatus());
        assertEquals("The type 'NotExistingType' does not exist", importLog.getMessage());
        importLog = importLogs.get(3);
        assertEquals(4, importLog.getLine());
        assertEquals(CSVImportLog.Status.SUCCESS, importLog.getStatus());
        assertEquals("Document created", importLog.getMessage());

        assertFalse(session.exists(new PathRef("/myfile")));
        assertTrue(session.exists(new PathRef("/mynote")));
        assertFalse(session.exists(new PathRef("/nonexisting")));
        assertTrue(session.exists(new PathRef("/mynote2")));
    }

}
