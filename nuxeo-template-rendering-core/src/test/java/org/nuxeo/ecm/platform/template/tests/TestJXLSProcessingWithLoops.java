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
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.template.TemplateInput;
import org.nuxeo.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.template.fm.FMContextBuilder;
import org.nuxeo.template.processors.jxls.JXLSTemplateProcessor;

public class TestJXLSProcessingWithLoops extends SimpleTemplateDocTestCase {

    public void testLoops() throws Exception {
        // build fake AuditEntries
        ArrayList<LogEntry> auditEntries = new ArrayList<LogEntry>();
        for (int i = 0; i < 5; i++) {
            LogEntryImpl entry = new LogEntryImpl();
            entry.setId(i);
            entry.setComment("Comment" + i);
            entry.setCategory("TestingCat" + i);
            entry.setEventId("TestEvent" + i);
            entry.setPrincipalName("TestingUser");
            auditEntries.add(entry);
        }
        FMContextBuilder.testAuditEntries = auditEntries;

        // setup redering
        TemplateBasedDocument adapter = setupTestDocs();
        DocumentModel testDoc = adapter.getAdaptedDoc();
        assertNotNull(testDoc);

        JXLSTemplateProcessor processor = new JXLSTemplateProcessor();

        Blob newBlob = processor.renderTemplate(adapter, TEMPLATE_NAME);

        // System.out.println(((FileBlob) newBlob).getFile().getAbsolutePath());

        XL2TextConverter xlConverter = new XL2TextConverter();
        BlobHolder textBlob = xlConverter.convert(
                new SimpleBlobHolder(newBlob), null);

        String xlContent = textBlob.getBlob().getString();

        // System.out.println(xlContent);

        assertTrue(xlContent.contains(testDoc.getId()));
        assertTrue(xlContent.contains(testDoc.getTitle()));
        assertTrue(xlContent.contains((String) testDoc.getPropertyValue("dc:description")));
        assertTrue(xlContent.contains("Comment0"));
        assertTrue(xlContent.contains("Comment2"));
        assertTrue(xlContent.contains("Comment4"));

        assertTrue(xlContent.contains("TestingCat0"));
        assertTrue(xlContent.contains("TestingCat1"));
        assertTrue(xlContent.contains("TestingCat2"));

    }

    @Override
    protected Blob getTemplateBlob() {
        File file = FileUtils.getResourceFileFromContext("data/jxls_simpleloop.xls");
        Blob fileBlob = new FileBlob(file);
        fileBlob.setFilename("jxls_simpletest.xls");
        return fileBlob;
    }

}
