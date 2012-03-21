package org.nuxeo.ecm.platform.template.tests;

import java.io.File;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;

public class TestFMProcessing extends SimpleTemplateDocTestCase {

    public void testDocumentsAttributes() throws Exception {
        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        String processorType = adapter.getSourceTemplate(TEMPLATE_NAME).getTemplateType();
        assertEquals("Freemarker", processorType);

        Blob newBlob = adapter.renderWithTemplate(TEMPLATE_NAME);

        String xmlContent = newBlob.getString();

        assertTrue(xmlContent.contains(testDoc.getTitle()));
        assertTrue(xmlContent.contains(testDoc.getId()));

    }

    @Override
    protected Blob getTemplateBlob() {
        File file = FileUtils.getResourceFileFromContext("data/test.ftl");
        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("test.ftl");
        return fileBlob;
    }

}
