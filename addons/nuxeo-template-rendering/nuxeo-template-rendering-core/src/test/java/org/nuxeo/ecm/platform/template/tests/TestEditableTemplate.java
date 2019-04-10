package org.nuxeo.ecm.platform.template.tests;

import java.io.File;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

import static org.junit.Assert.*;

public class TestEditableTemplate extends SQLRepositoryTestCase {

    protected static final String TEMPLATE_NAME = "mytestTemplate";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployBundle("org.nuxeo.template.manager.api");
        deployBundle("org.nuxeo.template.manager");
        openSession();
    }

    protected TemplateBasedDocument setupTestDocs() throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(
                root.getPathAsString(), "templatedDoc", "TemplateSource");
        templateDoc.setProperty("dublincore", "title", "MyTemplate");
        File file = FileUtils.getResourceFileFromContext("data/test.ftl");
        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("test.ftl");
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc.setPropertyValue("tmpl:templateName", TEMPLATE_NAME);
        templateDoc = session.createDocument(templateDoc);

        // now create a template based doc
        DocumentModel testDoc = session.createDocumentModel(
                root.getPathAsString(), "templatedBasedDoc",
                "TemplateBasedFile");
        testDoc.setProperty("dublincore", "title", "MyTestDoc");
        testDoc.setProperty("dublincore", "description", "some description");

        testDoc = session.createDocument(testDoc);

        // associate doc and template
        TemplateBasedDocument adapter = testDoc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        adapter.setTemplate(templateDoc, true);

        return adapter;
    }

    @Test
    public void testEdiableTemplate() throws Exception {
        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel doc = adapter.getAdaptedDoc();
        TemplateSourceDocument source = adapter.getSourceTemplate(TEMPLATE_NAME);
        assertNotNull(adapter);

        // check that the template has not been copied
        assertNull(adapter.getAdaptedDoc().getPropertyValue("file:content"));

        // check source template
        Blob templateBlob = adapter.getTemplateBlob(TEMPLATE_NAME);
        assertNotNull(templateBlob);
        assertTrue(templateBlob.getString().contains("This is document"));

        // check rendintion
        Blob newBlob = adapter.renderWithTemplate(TEMPLATE_NAME);
        String xmlContent = newBlob.getString();
        assertTrue(xmlContent.contains(doc.getTitle()));
        assertTrue(xmlContent.contains(doc.getId()));

        // detach the doc
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        doc = tps.detachTemplateBasedDocument(doc, TEMPLATE_NAME, true);

        // change template config
        DocumentModel sourceDoc = source.getAdaptedDoc();
        sourceDoc.setPropertyValue("tmpl:useAsMainContent", true);
        sourceDoc = session.saveDocument(sourceDoc);
        session.save();

        doc = tps.makeTemplateBasedDocument(doc, sourceDoc, true);
        adapter = doc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        // force refresh
        doc = adapter.initializeFromTemplate(TEMPLATE_NAME, true);
        adapter = doc.getAdapter(TemplateBasedDocument.class);

        Blob localTemplate = (Blob) doc.getPropertyValue("file:content");
        assertNotNull(localTemplate);

        StringBlob newTemplate = new StringBlob("Hello ${doc.id}");
        newTemplate.setFilename("newTemplate.ftl");

        doc.setPropertyValue("file:content", newTemplate);
        doc = session.saveDocument(doc);
        session.save();

        adapter = doc.getAdapter(TemplateBasedDocument.class);
        assertNotNull(adapter);

        Blob result = adapter.renderWithTemplate(TEMPLATE_NAME);
        assertNotNull(result);

        // System.out.println("Result=\n" + result.getString());

        assertTrue(result.getString().contains("Hello"));
        assertTrue(result.getString().contains(doc.getId()));

    }

    @Override
    public void tearDown() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

}
