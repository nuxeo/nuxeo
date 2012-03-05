package org.nuxeo.platform.scanimporter.tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.scanimporter.processor.ScannedFileImporter;
import org.nuxeo.ecm.platform.scanimporter.service.ImporterConfig;

public class TestImport extends SQLRepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestImport.class);

    protected List<File> tmpDirectories = new ArrayList<File>();

    protected String deployTestFiles(String name) throws IOException {

        File directory = new File(FileUtils.getResourcePathFromContext("data/"
                + name));

        String tmpDirectoryPath = System.getProperty("java.io.tmpdir");
        File dst = new File(tmpDirectoryPath);

        FileUtils.copy(directory, dst);
        tmpDirectories.add(dst);

        return dst.getPath() + "/" + name;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        openSession();

        deployContrib("org.nuxeo.ecm.platform.scanimporter", "OSGI-INF/importerservice-framework.xml");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        for (File dir : tmpDirectories) {
            if (dir.exists()) {
                FileUtils.deleteTree(dir);
            }
        }
    }

    public void testImport() throws Exception {



        String testPath = deployTestFiles("test3");
        File xmlFile = new File(testPath + "/descriptor.xml");
        assertTrue(xmlFile.exists());

        deployContrib("org.nuxeo.ecm.platform.scanimporter.test", "OSGI-INF/importerservice-test-contrib3.xml");


        ScannedFileImporter importer = new ScannedFileImporter();

        ImporterConfig config = new ImporterConfig();
        config.setTargetPath("/");
        config.setNbThreads(1);
        config.setBatchSize(10);
        config.setUseXMLMapping(true);

        importer.doImport(new File(testPath), config);

        session.save();

        DocumentModelList alldocs = session
                .query("select * from File order by ecm:path");

        for (DocumentModel doc : alldocs) {
            log.info("imported : " + doc.getPathAsString() + "-"
                    + doc.getType());
        }

        assertEquals(1, alldocs.size());

        DocumentModel doc = alldocs.get(0);

        assertEquals("SFC", doc.getPropertyValue("dc:source"));
        assertEquals("3-77-2", doc.getPropertyValue("dc:title"));
        assertEquals("12345", doc.getPropertyValue("dc:coverage"));

        assertEquals("testFile.txt", ((Blob)doc.getPropertyValue("file:content")).getFilename());
        assertEquals("This is a test.", ((Blob)doc.getPropertyValue("file:content")).getString());

        assertFalse(new File(testPath + "/descriptor.xml").exists());
    }

}
