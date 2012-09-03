package org.nuxeo.ecm.platform.importer.tests;

import java.io.File;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.api.Framework;

public class TestDefaultImporterServiceWithMeta extends SQLRepositoryTestCase {

    public static final String IMPORTER_CORE_TEST_BUNDLE = "org.nuxeo.ecm.platform.importer.core.test";

    public static final String IMPORTER_CORE_BUNDLE = "org.nuxeo.ecm.platform.importer.core";

    public TestDefaultImporterServiceWithMeta() {
        super();
    }

    public TestDefaultImporterServiceWithMeta(String name) {
        super(name);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        fireFrameworkStarted();
        openSession();
    }

    @Override
    protected void deployRepositoryContrib() throws Exception {
        super.deployRepositoryContrib();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle(IMPORTER_CORE_BUNDLE);
        deployContrib(IMPORTER_CORE_TEST_BUNDLE,
                "test-importer-service-contrib.xml");
    }

    @Test
    public void testImporterContribution() throws Exception {
        DefaultImporterService importerService = Framework.getService(DefaultImporterService.class);
        assertNotNull(importerService);

        File source = FileUtils.getResourceFileFromContext("import-src-with-metadatas");
        String targetPath = "/default-domain/workspaces/";

        importerService.importDocuments(targetPath, source.getPath(), false, 5,
                5);

        session.save();

        DocumentModel docContainer = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadatas"));
        assertNotNull(docContainer);
        assertEquals("Folder", docContainer.getType());

        DocumentModel folder = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadatas/branch1"));
        assertNotNull(folder);
        assertEquals("Folder", folder.getType());

        DocumentModel file = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadatas/hello.pdf"));
        assertNotNull(file);
        assertEquals("File", file.getType());
        assertEquals("src1", file.getPropertyValue("dc:source"));

        DocumentModel file1 = session.getChild(folder.getRef(), "hello1.pdf");
        assertEquals("src1-hello1", file1.getPropertyValue("dc:source"));

        DocumentModel folder11 = session.getChild(folder.getRef(), "branch11");

        DocumentModel file11 = session.getChild(folder11.getRef(),
                "hello11.pdf");
        String[] contributors = (String[]) file11.getPropertyValue("dc:subjects");
        assertNotNull(contributors);
        assertEquals("subject6", contributors[0]);
        assertEquals("subject7", contributors[1]);

    }

    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

}
