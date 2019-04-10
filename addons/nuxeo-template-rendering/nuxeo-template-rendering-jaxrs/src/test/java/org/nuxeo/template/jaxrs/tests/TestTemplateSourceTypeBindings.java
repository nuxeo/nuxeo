package org.nuxeo.template.jaxrs.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public class TestTemplateSourceTypeBindings extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.platform.dublincore");
        deployBundle("org.nuxeo.template.manager.api");
        deployBundle("org.nuxeo.template.manager");
        deployContrib("org.nuxeo.template.manager.jaxrs", "OSGI-INF/core-types-contrib.xml");
        deployContrib("org.nuxeo.template.manager.jaxrs", "OSGI-INF/life-cycle-contrib.xml");
        openSession();
    }

    protected TemplateSourceDocument createTemplateDoc(String name) throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), name, "TemplateSource");
        templateDoc.setProperty("dublincore", "title", name);
        File file = FileUtils.getResourceFileFromContext("data/testDoc.odt");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("testDoc.odt");
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc = session.createDocument(templateDoc);
        session.save();

        TemplateSourceDocument result = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(result);
        return result;
    }

    protected TemplateSourceDocument createWebTemplateDoc(String name) throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(root.getPathAsString(), name, "WebTemplateSource");
        templateDoc.setProperty("dublincore", "title", name);
        templateDoc.setProperty("note", "note", "Template ${doc.title}");
        templateDoc = session.createDocument(templateDoc);
        session.save();

        TemplateSourceDocument result = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(result);
        return result;
    }

    @SuppressWarnings("unused")
    @Test
    public void testAvailableTemplates() throws Exception {
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        session.save();
        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);

        List<DocumentModel> docs = tps.getAvailableTemplateDocs(session, null);
        assertEquals(1, docs.size());

        docs = tps.getAvailableTemplateDocs(session, "all");
        assertEquals(1, docs.size());

        TemplateSourceDocument t2 = createWebTemplateDoc("t2");
        session.save();
        docs = tps.getAvailableTemplateDocs(session, "all");
        assertEquals(2, docs.size());

    }

    @Override
    public void tearDown() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

}
