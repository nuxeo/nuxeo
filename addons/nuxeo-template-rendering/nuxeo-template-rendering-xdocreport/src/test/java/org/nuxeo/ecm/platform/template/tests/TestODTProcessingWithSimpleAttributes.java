package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.xdocreport.ZipXmlHelper;

import static org.junit.Assert.*;

public class TestODTProcessingWithSimpleAttributes extends SimpleTemplateDocTestCase {

    @Test
    public void testDocumentsAttributes() throws Exception {
        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        Blob newBlob = adapter.renderAndStoreAsAttachment(TEMPLATE_NAME, true);

        // System.out.println(((FileBlob) newBlob).getFile().getAbsolutePath());

        String xmlContent = ZipXmlHelper.readXMLContent(newBlob, ZipXmlHelper.OOO_MAIN_FILE);

        assertTrue(xmlContent.contains("Subject 1"));
        assertTrue(xmlContent.contains("Subject 2"));
        assertTrue(xmlContent.contains("Subject 3"));
        assertTrue(xmlContent.contains("MyTestDoc"));
        assertTrue(xmlContent.contains("Administrator"));

        // newBlob.transferTo(new File("/tmp/test.odt"));

    }

    @Override
    protected Blob getTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/DocumentsAttributes.odt");
        Blob blob = Blobs.createBlob(file);
        blob.setFilename("DocumentsAttributes.odt");
        return blob;
    }

}
