package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.processors.fm.JODReportTemplateProcessor;

public class TestJODProcessingWithSimpleAttributes extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployContrib("org.nuxeo.ecm.platform.template.managaner",
                "OSGI-INF/core-types-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.managaner",
                "OSGI-INF/life-cycle-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.template.managaner",
                "OSGI-INF/adapter-contrib.xml");
        openSession();
    }

    protected TemplateBasedDocument setupTestDocs() throws Exception {

        DocumentModel root = session.getRootDocument();
        DocumentModel testDoc = session.createDocumentModel(root
                .getPathAsString(), "templatedDoc", "TemplateBasedFile");
        testDoc.setProperty("dublincore", "title", "MyTestDoc");

        File file = FileUtils
                .getResourceFileFromContext("data/DocumentsAttributes.odt");

        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("DocumentsAttributes.odt");
        testDoc.setProperty("file", "content", fileBlob);
        testDoc.setProperty("dublincore", "description", "some description");

        List<String> subjects = new ArrayList<String>();
        subjects.add("Subject 1");
        subjects.add("Subject 2");
        subjects.add("Subject 3");

        testDoc.setPropertyValue("dc:subjects", (Serializable) subjects);

        testDoc = session.createDocument(testDoc);

        TemplateBasedDocument adapter = testDoc
                .getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        return adapter;
    }


    public void testDocumentsAttributes() throws Exception {
        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        Blob newBlob = adapter.updateBlobFromParams(true);

        System.out.println(((FileBlob)newBlob).getFile().getAbsolutePath());

        JODReportTemplateProcessor processor = new JODReportTemplateProcessor();
        String xmlContent = processor.readXMLContent(newBlob);

        assertTrue(xmlContent.contains("Subject 1"));
        assertTrue(xmlContent.contains("Subject 2"));
        assertTrue(xmlContent.contains("Subject 3"));
        assertTrue(xmlContent.contains("MyTestDoc"));
        assertTrue(xmlContent.contains("Administrator"));


    }
}
