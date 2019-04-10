package org.nuxeo.ecm.platform.template.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.xdocreport.XDocReportProcessor;
import org.nuxeo.template.processors.xdocreport.ZipXmlHelper;

public class TestXDocReportODSProcessing extends SimpleTemplateDocTestCase {

    @Test
    public void testFileUpdateFromParams() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        List<TemplateInput> params = new ArrayList<TemplateInput>();
        TemplateInput input = new TemplateInput("variable1", "YoVar1");
        params.add(input);

        testDoc = adapter.saveParams(TEMPLATE_NAME, params, true);
        session.save();

        XDocReportProcessor processor = new XDocReportProcessor();

        Blob newBlob = processor.renderTemplate(adapter, TEMPLATE_NAME);

        String xmlContent = ZipXmlHelper.readXMLContent(newBlob, ZipXmlHelper.OOO_MAIN_FILE);

        assertTrue(xmlContent.contains(testDoc.getTitle()));
        assertTrue(xmlContent.contains("YoVar1"));

    }

    @Override
    protected Blob getTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/testODS.ods");
        Blob fileBlob = Blobs.createBlob(file);
        fileBlob.setFilename("testODS.odt");
        return fileBlob;
    }

}
