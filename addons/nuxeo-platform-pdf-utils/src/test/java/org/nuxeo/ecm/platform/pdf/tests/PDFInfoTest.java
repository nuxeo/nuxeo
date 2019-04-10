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
import java.io.IOException;
import java.io.StringReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.pdf.PDFInfo;
import org.nuxeo.ecm.platform.pdf.operations.PDFExtractInfoOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFInfoTest {

    private FileBlob pdfFileBlob;

    private DocumentModel testDocsFolder, pdfDocModel;

    private DateFormat dateFormatter;

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
        assertNotNull(testDocsFolder);
        File pdfFile = FileUtils.getResourceFileFromContext(TestUtils.PDF_PATH);
        assertNotNull(pdfFile);
        pdfFileBlob = new FileBlob(pdfFile);
        assertNotNull(pdfFileBlob);
        pdfDocModel = coreSession.createDocumentModel(testDocsFolder.getPathAsString(), pdfFile.getName(), "File");
        pdfDocModel.setPropertyValue("dc:title", pdfFile.getName());
        pdfDocModel.setPropertyValue("file:content", pdfFileBlob);
        pdfDocModel = coreSession.createDocument(pdfDocModel);
        pdfDocModel = coreSession.saveDocument(pdfDocModel);
        assertNotNull(pdfDocModel);
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void tearDown() {
        coreSession.removeDocument(testDocsFolder.getRef());
        coreSession.save();
    }

    @Test
    public void testPDFInfo() {
        PDFInfo info = new PDFInfo(pdfFileBlob);
        assertNotNull(info);
        info.run();
        assertEquals("document.pdf", info.getFileName());
        assertEquals(67122, info.getFileSize());
        assertEquals("1.3", info.getPdfVersion());
        assertEquals(13, info.getNumberOfPages());
        assertEquals("SinglePage", info.getPageLayout());
        assertEquals("Untitled 3", info.getTitle());
        assertEquals("", info.getAuthor());
        assertEquals("", info.getSubject());
        assertEquals("Mac OS X 10.9.5 Quartz PDFContext", info.getProducer());
        assertEquals("TextEdit", info.getContentCreator());
        assertEquals("2014-10-23 20:49:29", dateFormatter.format(info.getCreationDate().getTime()));
        assertEquals("2014-10-23 20:49:29", dateFormatter.format(info.getModificationDate().getTime()));
        assertEquals(false, info.isEncrypted());
        assertEquals("", info.getKeywords());
        assertEquals(612.0, info.getMediaBoxWidthInPoints(), 0.0);
        assertEquals(792.0, info.getMediaBoxHeightInPoints(), 0.0);
        assertEquals(612.0, info.getCropBoxWidthInPoints(), 0.0);
        assertEquals(792.0, info.getCropBoxHeightInPoints(), 0.0);
        assertTrue(info.getPermissions().canPrint());
        assertTrue(info.getPermissions().canModify());
        assertTrue(info.getPermissions().canExtractContent());
        assertTrue(info.getPermissions().canModifyAnnotations());
        assertTrue(info.getPermissions().canFillInForm());
        assertTrue(info.getPermissions().canExtractForAccessibility());
        assertTrue(info.getPermissions().canAssembleDocument());
        assertTrue(info.getPermissions().canPrintDegraded());
    }

    @Test
    public void testPDFInfoHashMapValues() {
        PDFInfo info = new PDFInfo(pdfFileBlob);
        assertNotNull(info);
        info.run();
        HashMap<String, String> values = info.toHashMap();
        assertNotNull(values);
        assertEquals(29, values.size());
        assertEquals("document.pdf", values.get("File name"));
        assertEquals("67122", values.get("File size"));
        assertEquals("1.3", values.get("PDF version"));
        assertEquals("13", values.get("Page count"));
        assertEquals("612.0 x 792.0 points", values.get("Page size"));
        assertEquals("612.0", values.get("Page width"));
        assertEquals("792.0", values.get("Page height"));
        assertEquals("SinglePage", values.get("Page layout"));
        assertEquals("Untitled 3", values.get("Title"));
        assertEquals("", values.get("Author"));
        assertEquals("", values.get("Subject"));
        assertEquals("Mac OS X 10.9.5 Quartz PDFContext", values.get("PDF producer"));
        assertEquals("TextEdit", values.get("Content creator"));
        // The values of both Creation and Modification dates are relative to the local timezone.
        //assertTrue(values.get("Creation date").matches("2014-10-23 2[0-1]:49:29"));
        //assertTrue(values.get("Modification date").matches("2014-10-23 2[0-1]:49:29"));
        assertEquals("false", values.get("Encrypted"));
        assertEquals("", values.get("Keywords"));
        assertEquals("612.0", values.get("Media box width"));
        assertEquals("792.0", values.get("Media box height"));
        assertEquals("612.0", values.get("Crop box width"));
        assertEquals("792.0", values.get("Crop box height"));
        assertEquals("true", values.get("Can Print"));
        assertEquals("true", values.get("Can Modify"));
        assertEquals("true", values.get("Can Extract"));
        assertEquals("true", values.get("Can Modify Annotations"));
        assertEquals("true", values.get("Can Fill Forms"));
        assertEquals("true", values.get("Can Extract for Accessibility"));
        assertEquals("true", values.get("Can Assemble"));
        assertEquals("true", values.get("Can Print Degraded"));
    }

    @Test
    public void testPDFInfoOnEncryptedPDF() {
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_ENCRYPTED_PATH);
        FileBlob fb = new FileBlob(f);
        PDFInfo info = new PDFInfo(fb, TestUtils.PDF_ENCRYPTED_PASSWORD);
        assertNotNull(info);
        info.run();
        assertTrue(info.isEncrypted());
        assertEquals("1.4", info.getPdfVersion());
        assertEquals(67218, info.getFileSize());
        assertEquals("SinglePage", info.getPageLayout());
        assertEquals("TextEdit", info.getContentCreator());
        assertEquals("Mac OS X 10.10 Quartz PDFContext", info.getProducer());
    }

    @Test
    public void testPDFInfoGetXMP() {
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_XMP_PATH);
        FileBlob fb = new FileBlob(f);
        PDFInfo info = new PDFInfo(fb);
        assertNotNull(info);
        info.setParseWithXMP(true);
        info.run();
        String xmp = info.getXmp();
        assertNotNull(xmp);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmp));
            Document doc = dBuilder.parse(is);
            assertEquals("rdf:RDF", doc.getDocumentElement().getNodeName());
        } catch (ParserConfigurationException | IOException | SAXException e) {
            fail("DocumentBuilder failed either on creation or on parsing.");
        }
    }

    @Test
    public void testOwnerPermissions() {
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_PROTECTED_PATH);
        FileBlob fb = new FileBlob(f);
        PDFInfo info = new PDFInfo(fb, TestUtils.PDF_PROTECTED_OWNER_PASSWORD);
        assertNotNull(info);
        info.run();
        assertTrue(info.isEncrypted());
        assertTrue(info.getPermissions().canPrint());
        assertTrue(info.getPermissions().canModify());
        assertTrue(info.getPermissions().canExtractContent());
        assertTrue(info.getPermissions().canModifyAnnotations());
        assertTrue(info.getPermissions().canFillInForm());
        assertTrue(info.getPermissions().canExtractForAccessibility());
        assertTrue(info.getPermissions().canAssembleDocument());
        assertTrue(info.getPermissions().canPrintDegraded());
    }

    @Test
    public void testUserPermissions() {
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_PROTECTED_PATH);
        FileBlob fb = new FileBlob(f);
        PDFInfo info = new PDFInfo(fb, TestUtils.PDF_PROTECTED_USER_PASSWORD);
        assertNotNull(info);
        info.run();
        HashMap<String, String> values = info.toHashMap();
        assertNotNull(values);
        assertTrue(info.isEncrypted());
        assertTrue(info.getPermissions().canPrint());
        assertTrue(info.getPermissions().canExtractContent());
        assertTrue(info.getPermissions().canExtractForAccessibility());
        assertTrue(info.getPermissions().canPrintDegraded());
        assertFalse(info.getPermissions().canModify());
        assertFalse(info.getPermissions().canModifyAnnotations());
        assertFalse(info.getPermissions().canFillInForm());
        assertFalse(info.getPermissions().canAssembleDocument());
    }

    @Test
    public void testInfoToField() throws Exception {
        PDFInfo info = new PDFInfo(pdfDocModel);
        // using dublincore schema as placeholder schema
        HashMap<String, String> mapping = new HashMap<>();
        mapping.put("dc:coverage", "PDF version");
        mapping.put("dc:description", "Page count");
        mapping.put("dc:format", "Page layout");
        mapping.put("dc:language", "Title");
        mapping.put("dc:nature", "Author");
        mapping.put("dc:publisher", "Subject");
        mapping.put("dc:rights", "PDF producer");
        mapping.put("dc:source", "Content creator");
        mapping.put("dc:expired", "Creation date");
        mapping.put("dc:issued", "Modification date");
        DocumentModel result = info.toFields(pdfDocModel, mapping, false, null);
        // PDF Version
        assertEquals("1.3", result.getPropertyValue("dc:coverage"));
        // Page count
        assertEquals("13", result.getPropertyValue("dc:description"));
        // Page layout
        assertEquals("SinglePage", result.getPropertyValue("dc:format"));
        // Title
        assertEquals("Untitled 3", result.getPropertyValue("dc:language"));
        // Author
        assertEquals("", result.getPropertyValue("dc:nature"));
        // Subject
        assertEquals("", result.getPropertyValue("dc:publisher"));
        // PDF producer
        assertEquals("Mac OS X 10.9.5 Quartz PDFContext", result.getPropertyValue("dc:rights"));
        // Content creator
        assertEquals("TextEdit", result.getPropertyValue("dc:source"));
        // Creation Date
        Calendar cal = (Calendar) result.getPropertyValue("dc:expired");
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals("2014-10-23 00:00:00", dateFormatter.format(cal.getTime()));
        // Creation Modification
        cal = (Calendar) result.getPropertyValue("dc:issued");
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals("2014-10-23 00:00:00", dateFormatter.format(cal.getTime()));
    }

    @Test(expected = NuxeoException.class)
    public void testPDFInfoShouldFailOnNonPDFBlob() {
        File f = FileUtils.getResourceFileFromContext(TestUtils.JPG_PATH);
        FileBlob fb = new FileBlob(f);
        PDFInfo info = new PDFInfo(fb);
        info.run(); // IOException: Header doesn't contain versioninfo
    }

    @Test(expected = NuxeoException.class)
    public void testPDFInfoShouldFailOnEncryptedPDFAndBadPassword() {
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_ENCRYPTED_PATH);
        FileBlob fb = new FileBlob(f);
        PDFInfo info = new PDFInfo(fb, "toto");
        info.run(); // CryptographyException: The supplied password does not match...
    }

    @Test
    public void testExtractInfoOperation() throws Exception {
        // using dublincore schema as placeholder schema
        HashMap<String, String> mapping = new HashMap<>();
        mapping.put("dc:coverage", "PDF version");
        mapping.put("dc:description", "Page count");
        mapping.put("dc:format", "Page layout");
        mapping.put("dc:language", "Title");
        mapping.put("dc:nature", "Author");
        mapping.put("dc:publisher", "Subject");
        mapping.put("dc:rights", "PDF producer");
        mapping.put("dc:source", "Content creator");
        mapping.put("dc:expired", "Creation date");
        mapping.put("dc:issued", "Modification date");
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);
        ctx.setInput(pdfDocModel);
        chain.add(PDFExtractInfoOperation.ID)
            .set("properties", new Properties(mapping));
        DocumentModel result = (DocumentModel) automationService.run(ctx, chain);
        assertNotNull(result);
        // PDF Version
        assertEquals("1.3", result.getPropertyValue("dc:coverage"));
        // Page count
        assertEquals("13", result.getPropertyValue("dc:description"));
        // Page layout
        assertEquals("SinglePage", result.getPropertyValue("dc:format"));
        // Title
        assertEquals("Untitled 3", result.getPropertyValue("dc:language"));
        // Author
        assertEquals("", result.getPropertyValue("dc:nature"));
        // Subject
        assertEquals("", result.getPropertyValue("dc:publisher"));
        // PDF producer
        assertEquals("Mac OS X 10.9.5 Quartz PDFContext", result.getPropertyValue("dc:rights"));
        // Content creator
        assertEquals("TextEdit", result.getPropertyValue("dc:source"));
        // Creation Date
        Calendar cal = (Calendar) result.getPropertyValue("dc:expired");
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals("2014-10-23 00:00:00", dateFormatter.format(cal.getTime()));
        // Creation Modification
        cal = (Calendar) result.getPropertyValue("dc:issued");
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals("2014-10-23 00:00:00", dateFormatter.format(cal.getTime()));
    }

}
