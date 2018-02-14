/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Frederic Vadon
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf.tests;

import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.pdf.PDFTextExtractor;
import org.nuxeo.ecm.platform.pdf.operations.PDFExtractTextOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import javax.inject.Inject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFTextExtractorTest {

    private FileBlob pdfFileBlob;

    private DocumentModel testDocsFolder;

    private DocumentModel testDoc;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Before
    public void setUp() {
        testDocsFolder = coreSession.createDocumentModel("/", "test-pictures", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_CONTRACT_PATH);
        pdfFileBlob = new FileBlob(f);
        DocumentModel doc = coreSession.createDocumentModel(testDocsFolder.getPathAsString(), f.getName(), "File");
        doc.setPropertyValue("dc:title", f.getName());
        doc.setPropertyValue("file:content", new FileBlob(f));
        testDoc = coreSession.createDocument(doc);
    }

    @After
    public void tearDown() {
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    @Test
    public void testExtractText() throws Exception {
        PDFTextExtractor pdfte = new PDFTextExtractor(pdfFileBlob);
        String text = pdfte.getAllExtractedLines();
        assertNotNull(text);
        assertEquals(19045, text.length());
        assertEquals("Contract Number: 123456789", pdfte.extractLineOf("Contract Number: "));
        assertFalse(text.contains("Something that is not in the file"));
        assertTrue(text.contains("13.1"));
        assertEquals("123456789", pdfte.extractLastPartOfLine("Contract Number: "));
    }

    @Test
    public void testExtractTextOperation() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(testDoc);
        chain.add(PDFExtractTextOperation.ID)
            .set("save", true)
            .set("targetxpath", "dc:description")
            .set("patterntofind", "Contract Number: ")
            .set("removepatternfromresult", false);
        DocumentModel documentModified = (DocumentModel) automationService.run(ctx, chain);
        assertEquals("Contract Number: 123456789", documentModified.getPropertyValue("dc:description"));
        chain = new OperationChain("testChain");
        chain.add(PDFExtractTextOperation.ID)
            .set("save", true)
            .set("targetxpath", "dc:description")
            .set("patterntofind", "Something that is not in the file")
            .set("removepatternfromresult", false);
        documentModified = (DocumentModel) automationService.run(ctx, chain);
        assertNull(documentModified.getPropertyValue("dc:description"));
        chain = new OperationChain("testChain");
        chain.add(PDFExtractTextOperation.ID)
            .set("save", true)
            .set("targetxpath", "dc:description")
            .set("patterntofind", "Contract Number: ")
            .set("removepatternfromresult", true);
        documentModified = (DocumentModel) automationService.run(ctx, chain);
        assertEquals("123456789", documentModified.getPropertyValue("dc:description"));
    }

}
