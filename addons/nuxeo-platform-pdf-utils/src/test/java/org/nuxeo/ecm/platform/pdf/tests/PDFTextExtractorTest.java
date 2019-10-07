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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.pdf.PDFTextExtractor;
import org.nuxeo.ecm.platform.pdf.operations.PDFExtractTextOperation;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
@ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class, cause = "NXP-26793")
public class PDFTextExtractorTest {

    private FileBlob pdfFileBlob;

    private DocumentModel testDocsFolder;

    private DocumentModel testDoc;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    protected OperationContext ctx;

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
        ctx = new OperationContext(coreSession);
    }

    @After
    public void tearDown() {
        ctx.close();
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    @Test
    public void testExtractText() throws Exception {
        PDFTextExtractor pdfte = new PDFTextExtractor(pdfFileBlob);

        // All text
        String text = pdfte.getAllExtractedLines();
        assertNotNull(text);

        assertEquals(19045, text.length());
        assertFalse(text.contains("Something that is not in the file"));
        assertTrue(text.contains("13.1"));

        // Extract some text
        assertEquals("Contract Number: 123456789", pdfte.extractLineOf("Contract Number: "));
        assertEquals("123456789", pdfte.extractLastPartOfLine("Contract Number: "));
    }

    protected void doTestExtractAllText(boolean useEmptyPattern) throws Exception {

        Map<String, Object> params = new HashMap<>();
        // We don't save because we set the dc:description field which has a max chars limit of 2,000
        // and we don't want to complicate the unit test with XML extensions changing that or deploying
        // another field, etc.
        params.put("save", false);
        params.put("targetxpath", "dc:description");
        if (useEmptyPattern) {
            params.put("patterntofind", "");
        }

        OperationChain chain = new OperationChain("testChain");

        ctx.setInput(testDoc);
        chain.add(PDFExtractTextOperation.ID);
        chain.addChainParameters(params);

        DocumentModel documentModified = (DocumentModel) automationService.run(ctx, chain);
        String text = (String) documentModified.getPropertyValue("dc:description");
        assertNotNull(text);
        assertEquals(19045, text.length());
        assertFalse(text.contains("Something that is not in the file"));
        assertTrue(text.contains("13.1"));

    }

    @Test
    public void testOperationExtractAlltext() throws Exception {

        // Test with no patterntofind at all
        doTestExtractAllText(false);

        // Passing an empty patterntofind => all text
        doTestExtractAllText(true);
    }

    protected DocumentModel doOperationExtractSomeText(String pattern, boolean removePatternFromResult)
            throws Exception {

        OperationChain chain = new OperationChain("testChain");
        ctx.setInput(testDoc);
        chain.add(PDFExtractTextOperation.ID)
             .set("save", false)
             .set("targetxpath", "dc:description")
             .set("patterntofind", pattern)
             .set("removepatternfromresult", removePatternFromResult);
        DocumentModel result = (DocumentModel) automationService.run(ctx, chain);

        return result;
    }

    @Test
    public void testOperationExtractSomeText() throws Exception {

        DocumentModel documentModified;

        // Getting one line
        documentModified = doOperationExtractSomeText("Contract Number: ", false);
        assertEquals("Contract Number: 123456789", documentModified.getPropertyValue("dc:description"));

        // Getting part of a line
        documentModified = doOperationExtractSomeText("Contract Number: ", true);
        assertEquals("123456789", documentModified.getPropertyValue("dc:description"));

        // Passing a pattern that does not exist => extracted text must be null
        documentModified = doOperationExtractSomeText("Something that is not in the file", false);
        assertNull(documentModified.getPropertyValue("dc:description"));
    }

}
