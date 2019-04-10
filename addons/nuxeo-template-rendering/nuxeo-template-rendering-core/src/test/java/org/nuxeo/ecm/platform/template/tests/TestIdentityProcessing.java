package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;

public class TestIdentityProcessing extends SimpleTemplateDocTestCase {

    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.mimetype.api");
        deployBundle("org.nuxeo.ecm.platform.mimetype.core");
    }

    @Test
    public void testDocumentsAttributes() throws Exception {
        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        String processorType = adapter.getSourceTemplate(TEMPLATE_NAME).getTemplateType();

        Blob newBlob = adapter.renderWithTemplate(TEMPLATE_NAME);

        String fakeBinContent = newBlob.getString();

        assertEquals(getTemplateBlob().getString(), fakeBinContent);

    }

    @Override
    protected Blob getTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/testIdentity.bin");
        Blob blob = Blobs.createBlob(file);
        blob.setFilename("testIdentity.bin");
        return blob;
    }

}
