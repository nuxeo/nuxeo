package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.api.TemplateProcessorService;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public class TestTemplateSourceTypeBindings extends SQLRepositoryTestCase {

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
        deployContrib("org.nuxeo.template.manager",
                "OSGI-INF/core-types-contrib.xml");
        deployContrib("org.nuxeo.template.manager",
                "OSGI-INF/life-cycle-contrib.xml");
        deployContrib("org.nuxeo.template.manager",
                "OSGI-INF/adapter-contrib.xml");
        deployContrib("org.nuxeo.template.manager",
                "OSGI-INF/templateprocessor-service.xml");
        deployContrib("org.nuxeo.template.manager",
                "OSGI-INF/templateprocessor-contrib.xml");
        deployContrib("org.nuxeo.template.manager",
                "OSGI-INF/listener-contrib.xml");
        openSession();
    }

    protected TemplateSourceDocument createTemplateDoc(String name)
            throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(
                root.getPathAsString(), name, "TemplateSource");
        templateDoc.setProperty("dublincore", "title", name);
        File file = FileUtils.getResourceFileFromContext("data/testDoc.odt");
        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("testDoc.odt");
        templateDoc.setProperty("file", "content", fileBlob);
        templateDoc = session.createDocument(templateDoc);
        session.save();

        TemplateSourceDocument result = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(result);
        return result;
    }

    protected TemplateSourceDocument createWebTemplateDoc(String name)
            throws Exception {

        DocumentModel root = session.getRootDocument();

        // create template
        DocumentModel templateDoc = session.createDocumentModel(
                root.getPathAsString(), name, "WebTemplateSource");
        templateDoc.setProperty("dublincore", "title", name);
        templateDoc.setProperty("note", "note", "Template ${doc.title}");
        templateDoc = session.createDocument(templateDoc);
        session.save();

        TemplateSourceDocument result = templateDoc.getAdapter(TemplateSourceDocument.class);
        assertNotNull(result);
        return result;
    }

    @Test
    public void testTypeBindingAndOverride() throws Exception {

        // test simple mapping
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        t1.setForcedTypes(new String[] { "File", "Note" }, true);

        assertTrue(t1.getForcedTypes().contains("File"));
        assertTrue(t1.getForcedTypes().contains("Note"));

        session.save();

        // wait for Async listener to run !
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);

        Map<String, List<String>> mapping = tps.getTypeMapping();

        assertTrue(mapping.get("File").contains(t1.getAdaptedDoc().getId()));
        assertTrue(mapping.get("Note").contains(t1.getAdaptedDoc().getId()));

        // wait for Async listener to run !
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        // test override
        TemplateSourceDocument t2 = createTemplateDoc("t2");
        t2.setForcedTypes(new String[] { "Note" }, true);

        assertFalse(t2.getForcedTypes().contains("File"));
        assertTrue(t2.getForcedTypes().contains("Note"));

        session.save();

        // wait for Async listener to run !
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        session.save();

        mapping = tps.getTypeMapping();

        assertTrue(mapping.get("File").contains(t1.getAdaptedDoc().getId()));
        assertTrue(mapping.get("Note").contains(t1.getAdaptedDoc().getId()));
        assertTrue(mapping.get("Note").contains(t2.getAdaptedDoc().getId()));

        // check update on initial template
        // re-fetch staled DocumentModel
        t1 = session.getDocument(new IdRef(t1.getAdaptedDoc().getId())).getAdapter(
                TemplateSourceDocument.class);
        assertTrue(t1.getForcedTypes().contains("File"));
        assertTrue(t1.getForcedTypes().contains("Note"));
    }

    @Test
    public void testAutomaticTemplateBinding() throws Exception {

        // create a template and a simple mapping
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        t1.setForcedTypes(new String[] { "File" }, true);
        assertTrue(t1.getForcedTypes().contains("File"));
        session.save();

        // wait for Async listener to run !
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        // now create a simple file
        DocumentModel root = session.getRootDocument();
        DocumentModel simpleFile = session.createDocumentModel(
                root.getPathAsString(), "myTestFile", "File");
        simpleFile = session.createDocument(simpleFile);

        session.save();

        // verify that template has been associated
        TemplateBasedDocument templatizedFile = simpleFile.getAdapter(TemplateBasedDocument.class);
        assertNotNull(templatizedFile);

        // remove binding
        t1.setForcedTypes(new String[] {}, true);
        session.save();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        // now create a simple file
        DocumentModel simpleFile2 = session.createDocumentModel(
                root.getPathAsString(), "myTestFile2", "File");
        simpleFile2 = session.createDocument(simpleFile2);

        // verify that template has NOT been associated
        assertNull(simpleFile2.getAdapter(TemplateBasedDocument.class));

    }

    @Test
    public void testManualTemplateBinding() throws Exception {

        // create a template and no mapping
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        session.save();

        // now create a simple Note
        DocumentModel root = session.getRootDocument();
        DocumentModel simpleNote = session.createDocumentModel(
                root.getPathAsString(), "myTestFile", "Note");
        simpleNote = session.createDocument(simpleNote);

        session.save();

        // verify that not template is associated
        assertNull(simpleNote.getAdapter(TemplateBasedDocument.class));

        TemplateProcessorService tps = Framework.getLocalService(TemplateProcessorService.class);
        simpleNote = tps.makeTemplateBasedDocument(simpleNote,
                t1.getAdaptedDoc(), true);

        // verify that template has been associated
        assertNotNull(simpleNote.getAdapter(TemplateBasedDocument.class));

    }

    @Test
    public void testAutomaticTemplateMultiBinding() throws Exception {

        // create a template and a simple mapping
        TemplateSourceDocument t1 = createTemplateDoc("t1");
        t1.setForcedTypes(new String[] { "File" }, true);
        assertTrue(t1.getForcedTypes().contains("File"));
        session.save();

        // create a second template and a simple mapping
        TemplateSourceDocument t2 = createTemplateDoc("t2");
        t2.setForcedTypes(new String[] { "File" }, true);
        assertTrue(t2.getForcedTypes().contains("File"));
        session.save();

        // wait for Async listener to run !
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();

        // now create a simple file
        DocumentModel root = session.getRootDocument();
        DocumentModel simpleFile = session.createDocumentModel(
                root.getPathAsString(), "myTestFile", "File");
        simpleFile = session.createDocument(simpleFile);

        session.save();

        // verify that template has been associated
        TemplateBasedDocument templatizedFile = simpleFile.getAdapter(TemplateBasedDocument.class);
        assertNotNull(templatizedFile);

        List<String> templateNames = templatizedFile.getTemplateNames();

        assertTrue(templateNames.contains("t1"));
        assertTrue(templateNames.contains("t2"));

    }

    @Override
    public void tearDown() throws Exception {
        EventService eventService = Framework.getLocalService(EventService.class);
        eventService.waitForAsyncCompletion();
        closeSession();
        super.tearDown();
    }

}
