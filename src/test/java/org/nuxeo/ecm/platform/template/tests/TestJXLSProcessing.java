package org.nuxeo.ecm.platform.template.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.XL2TextConverter;
import org.nuxeo.ecm.platform.template.TemplateInput;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.processors.jxls.JXLSTemplateProcessor;

public class TestJXLSProcessing extends SimpleTemplateDocTestCase {

    public void testFileUpdateFromParams() throws Exception {

        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        List<TemplateInput> params = new ArrayList<TemplateInput>();
        TemplateInput input = new TemplateInput("variable1", "YoVar1");
        params.add(input);

        testDoc = adapter.saveParams(TEMPLATE_NAME, params, true);
        session.save();

        JXLSTemplateProcessor processor = new JXLSTemplateProcessor();

        Blob newBlob = processor.renderTemplate(adapter, TEMPLATE_NAME);

        System.out.println(((FileBlob) newBlob).getFile().getAbsolutePath());

        XL2TextConverter xlConverter = new XL2TextConverter();
        BlobHolder textBlob = xlConverter.convert(
                new SimpleBlobHolder(newBlob), null);

        String xlContent = textBlob.getBlob().getString();

        System.out.println(xlContent);

        assertTrue(xlContent.contains(testDoc.getId()));
        assertTrue(xlContent.contains(testDoc.getTitle()));
        assertTrue(xlContent.contains((String) testDoc.getPropertyValue("dc:description")));
        assertTrue(xlContent.contains("YoVar1"));
    }

    @Override
    protected Blob getTemplateBlob() {
        File file = FileUtils.getResourceFileFromContext("data/jxls_simpletest.xls");
        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("jxls_simpletest.xls");
        return fileBlob;
    }

}
