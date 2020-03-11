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
 *     Thibaud Arguillere
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf.tests;

import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.pdf.PDFUtils;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import javax.inject.Inject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFUtilsTest {

    private File pdfFile;

    private DocumentModel testDocsFolder;

    @Inject
    CoreSession coreSession;

    @Before
    public void setUp() throws Exception {
        testDocsFolder = coreSession.createDocumentModel("/", "test-pictures", "Folder");
        testDocsFolder.setPropertyValue("dc:title", "test-pdfutils");
        testDocsFolder = coreSession.createDocument(testDocsFolder);
        testDocsFolder = coreSession.saveDocument(testDocsFolder);
        pdfFile = FileUtils.getResourceFileFromContext(TestUtils.PDF_PATH);
        DocumentModel pdfDocModel = coreSession.createDocumentModel(testDocsFolder.getPathAsString(),
            pdfFile.getName(), "File");
        pdfDocModel.setPropertyValue("dc:title", pdfFile.getName());
        pdfDocModel.setPropertyValue("file:content", new FileBlob(pdfFile));
        pdfDocModel = coreSession.createDocument(pdfDocModel);
        pdfDocModel = coreSession.saveDocument(pdfDocModel);
        assertNotNull(pdfDocModel);
    }

    @After
    public void tearDown() {
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    @Test
    public void testUtilsRGB() {
        int[] rgb = PDFUtils.hex255ToRGB("#000000");
        assertEquals(3, rgb.length);
        assertEquals(0, rgb[0]);
        assertEquals(0, rgb[1]);
        assertEquals(0, rgb[2]);
        rgb = PDFUtils.hex255ToRGB("0xffFfFf");
        assertEquals(255, rgb[0]);
        assertEquals(255, rgb[1]);
        assertEquals(255, rgb[2]);
        rgb = PDFUtils.hex255ToRGB("123456");
        assertEquals(18, rgb[0]);
        assertEquals(52, rgb[1]);
        assertEquals(86, rgb[2]);
        rgb = PDFUtils.hex255ToRGB("");
        assertEquals(0, rgb[0]);
        assertEquals(0, rgb[1]);
        assertEquals(0, rgb[2]);
    }

    @Test
    public void testUtilsSaveInTempFile() throws Exception {
        PDDocument doc = PDDocument.load(pdfFile);
        FileBlob fb = PDFUtils.saveInTempFile(doc);
        assertNotNull(fb);
        assertEquals("application/pdf", fb.getMimeType());
        assertEquals(67100, fb.getLength());
        doc.close();
    }

    @Test
    public void testUtilsCheckXPath() {
        assertEquals("file:content", PDFUtils.checkXPath(null));
        assertEquals("file:content", PDFUtils.checkXPath(""));
        assertEquals("myschema:myfield", PDFUtils.checkXPath("myschema:myfield"));
    }

    @Test
    public void testUtilsSetInfos() throws Exception {
        PDDocument doc = PDDocument.load(pdfFile);
        assertEquals("Untitled 3", doc.getDocumentInformation().getTitle());
        assertNull(doc.getDocumentInformation().getSubject());
        assertNull(doc.getDocumentInformation().getAuthor());
        PDFUtils.setInfos(doc, "The Title", "The Subject", "The Author");
        assertEquals("The Title", doc.getDocumentInformation().getTitle());
        assertEquals("The Subject", doc.getDocumentInformation().getSubject());
        assertEquals("The Author", doc.getDocumentInformation().getAuthor());
        doc.close();
    }

}
