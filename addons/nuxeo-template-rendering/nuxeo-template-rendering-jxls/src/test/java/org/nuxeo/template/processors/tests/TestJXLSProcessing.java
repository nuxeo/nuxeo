package org.nuxeo.template.processors.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.plugins.text.extractors.XL2TextConverter;
import org.nuxeo.template.api.TemplateInput;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.processors.jxls.JXLSTemplateProcessor;

public class TestJXLSProcessing extends SimpleTemplateDocTestCase {

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

        JXLSTemplateProcessor processor = new JXLSTemplateProcessor();

        Blob newBlob = processor.renderTemplate(adapter, TEMPLATE_NAME);

        // System.out.println(((FileBlob) newBlob).getFile().getAbsolutePath());

        XL2TextConverter xlConverter = new XL2TextConverter();
        BlobHolder textBlob = xlConverter.convert(new SimpleBlobHolder(newBlob), null);

        String xlContent = textBlob.getBlob().getString();

        // System.out.println(xlContent);

        assertTrue(xlContent.contains(testDoc.getId()));
        assertTrue(xlContent.contains(testDoc.getTitle()));
        assertTrue(xlContent.contains((String) testDoc.getPropertyValue("dc:description")));
        assertTrue(xlContent.contains("YoVar1"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        assertTrue(xlContent.contains(dateFormat.format(Calendar.getInstance().getTime())));
    }

    @Override
    protected Blob getTemplateBlob() throws IOException {
        File file = FileUtils.getResourceFileFromContext("data/jxls_simpletest.xls");
        Blob blob = Blobs.createBlob(file);
        blob.setFilename("jxls_simpletest.xls");
        return blob;
    }

}
