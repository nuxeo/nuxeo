package org.nuxeo.ecm.platform.importer.tests;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.api.Framework;

public class TestDefaultImporterService extends SQLRepositoryTestCase {

    public static final String IMPORTER_CORE_TEST_BUNDLE = "org.nuxeo.ecm.platform.importer.core.test";

    public static final String IMPORTER_CORE_BUNDLE = "org.nuxeo.ecm.platform.importer.core";

    public TestDefaultImporterService(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
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

    public void testImporterContribution() throws Exception {
        DefaultImporterService importerService = Framework.getService(DefaultImporterService.class);
        assertNotNull(importerService);

        File source = FileUtils.getResourceFileFromContext("import-src");
        String targetPath = "/default-domain/workspaces/";

        importerService.importDocuments(targetPath, source.getPath(), false, 5,
                5);

        session.save();

        DocumentModel docContainer = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src"));
        assertNotNull(docContainer);
        assertEquals("Folder", docContainer.getType());

        DocumentModel folder = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src/branch1"));
        assertNotNull(folder);
        assertEquals("Folder", docContainer.getType());

        DocumentModel file = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src/hello-pdf"));
        assertNotNull(file);
        assertEquals("File", file.getType());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        closeSession();
    }

}