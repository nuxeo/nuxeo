package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.processors.xdocreport.ZipXmlHelper;

public class TestXDocReportDOCXProcessing extends SimpleTemplateDocTestCase {

    public void testFileUpdateFromParams() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        List<TemplateInput> params = getTestParams();

        testDoc = adapter.saveParams(params, true);
        session.save();

        Blob newBlob = adapter.renderAndStoreAsAttachment(true);

        System.out.println(((FileBlob) newBlob).getFile().getAbsolutePath());

        String xmlContent = ZipXmlHelper.readXMLContent(newBlob,
                ZipXmlHelper.DOCX_MAIN_FILE);

        System.out.println(xmlContent);

        assertTrue(xmlContent.contains("John Smith"));
        assertTrue(xmlContent.contains("some description"));
        assertTrue(xmlContent.contains("The Boolean value is false"));
        assertTrue(xmlContent.contains("r:embed=\"xdocreport_0\""));

    }

    @Override
    protected Blob getTemplateBlob() {
        File file = FileUtils.getResourceFileFromContext("data/testDoc.docx");
        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("testDoc.odt");
        return fileBlob;
    }

}
