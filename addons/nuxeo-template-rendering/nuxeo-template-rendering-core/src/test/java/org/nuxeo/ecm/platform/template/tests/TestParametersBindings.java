package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public class TestParametersBindings extends SimpleTemplateDocTestCase {

    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
    }

    @Test
    public void testParametersViaAdapters() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        TemplateSourceDocument src = adapter.getSourceTemplate(TEMPLATE_NAME);
        assertNotNull(src);
        List<TemplateInput> params = src.getParams();
        assertEquals(2, params.size());
        assertEquals("variableA", params.get(0).getName());
        assertEquals("variableB", params.get(1).getName());

        src.setTemplateBlob(getTemplateBlobWithOneParam(), true);
        params = src.getParams();
        assertEquals(1, params.size());
        assertEquals("variable1", params.get(0).getName());

    }

    @Test
    public void testParametersViaDoc() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        TemplateSourceDocument src = adapter.getSourceTemplate(TEMPLATE_NAME);
        assertNotNull(src);
        List<TemplateInput> params = src.getParams();
        assertEquals(2, params.size());
        assertEquals("variableA", params.get(0).getName());
        assertEquals("variableB", params.get(1).getName());

        DocumentModel doc = src.getAdaptedDoc();

        doc.setPropertyValue("file:content", (Serializable) getTemplateBlobWithOneParam());
        doc = session.saveDocument(doc);

        src = doc.getAdapter(TemplateSourceDocument.class);

        params = src.getParams();
        assertEquals(1, params.size());
        assertEquals("variable1", params.get(0).getName());

        doc = session.saveDocument(doc);

    }

    @Override
    protected Blob getTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/test2.ftl");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("test2.ftl");
        return fileBlob;
    }

    protected Blob getTemplateBlobWithOneParam() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/test.ftl");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("test2.ftl");
        return fileBlob;
    }

}
