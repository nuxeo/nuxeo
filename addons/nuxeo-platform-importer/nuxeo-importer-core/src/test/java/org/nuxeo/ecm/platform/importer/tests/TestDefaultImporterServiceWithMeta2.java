package org.nuxeo.ecm.platform.importer.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.platform.importer.core", //
})
@LocalDeploy("org.nuxeo.ecm.platform.importer.core.test:test-importer-service-contrib2.xml")
public class TestDefaultImporterServiceWithMeta2 {

    @Inject
    protected CoreSession session;

    @Inject
    protected DefaultImporterService importerService;

    @Test
    public void testImporterContribution() throws Exception {
        File source = FileUtils.getResourceFileFromContext("import-src-with-metadatas");
        String targetPath = "/default-domain/workspaces/";

        importerService.importDocuments(targetPath, source.getPath(), false, 5, 5);

        session.save();

        DocumentModel docContainer = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadata"));
        assertNotNull(docContainer);
        assertEquals("Folder", docContainer.getType());

        DocumentModel folder = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadata/branch1"));
        assertNotNull(folder);
        assertEquals("Folder", folder.getType());

        DocumentModel file = session.getDocument(new PathRef(
                "/default-domain/workspaces/import-src-with-metadata/hello.pdf"));
        assertNotNull(file);
        assertEquals("File", file.getType());
        assertEquals("src1", file.getPropertyValue("dc:source"));

        DocumentModel file1 = session.getChild(folder.getRef(), "hello1.pdf");
        assertEquals("src1-hello1", file1.getPropertyValue("dc:source"));

        DocumentModel folder11 = session.getChild(folder.getRef(), "branch11");

        DocumentModel file11 = session.getChild(folder11.getRef(), "hello11.pdf");
        String[] contributors = (String[]) file11.getPropertyValue("dc:subjects");
        assertNotNull(contributors);
        assertEquals("subject6", contributors[0]);
        assertEquals("subject7", contributors[1]);

    }

}
