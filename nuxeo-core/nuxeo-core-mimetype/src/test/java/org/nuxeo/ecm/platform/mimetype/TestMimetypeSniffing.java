/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestMimetypeSniffing.java 28493 2008-01-04 19:51:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import javax.inject.Inject;

/**
 * Test binary files sniff.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 * @author <a href="mailto:lg@nuxeo.com">Laurent Godard</a>
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.core.mimetype")
public class TestMimetypeSniffing {

    @Inject
    private MimetypeRegistry mimetypeRegistry;

    private static File getFileFromResource(String path) {
        // retrieves contextually the resource file and decode its path
        // returns the corresponding File Object
        return FileUtils.getResourceFileFromContext(path);
    }

    private static File getZeroesDocument() {
        return FileUtils.getResourceFileFromContext("test-data/zeroes");
    }

    @Test
    public void testZeroesDocumentFromFile() throws Exception {
        assertEquals("application/octet-stream", mimetypeRegistry.getMimetypeFromFile(getZeroesDocument()));
    }

    @Test
    public void testZeroesDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getZeroesDocument());
        assertEquals("application/octet-stream", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    private static File getTextDocument() {
        return FileUtils.getResourceFileFromContext("test-data/hello.txt");
    }

    @Test
    public void testTextDocumentFromFile() throws Exception {
        assertEquals("text/plain", mimetypeRegistry.getMimetypeFromFile(getTextDocument()));
    }

    @Test
    public void testTextDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getTextDocument());
        assertEquals("text/plain", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    private static File getWordDocument() {
        return FileUtils.getResourceFileFromContext("test-data/hello.doc");
    }

    @Test
    public void testWordDocumentFromFile() throws Exception {
        assertEquals("application/msword", mimetypeRegistry.getMimetypeFromFile(getWordDocument()));
    }

    @Test
    public void testWordDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getWordDocument());
        assertEquals("application/msword", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    private static File getExcelDocument() {
        return getFileFromResource("test-data/hello.xls");
    }

    public void xtestExcelDocumentFromFile() throws Exception {
        assertEquals("application/vnd.ms-excel", mimetypeRegistry.getMimetypeFromFile(getExcelDocument()));
    }

    public void xtestExcelDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getExcelDocument());
        assertEquals("application/vnd.ms-excel", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    private static File getPowerpointDocument() {
        return getFileFromResource("test-data/hello.ppt");
    }

    public void xtestPowerpointDocumentFromFile() throws Exception {
        assertEquals("application/vnd.ms-powerpoint", mimetypeRegistry.getMimetypeFromFile(getPowerpointDocument()));
    }

    public void xtestPowerpointDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getPowerpointDocument());
        assertEquals("application/vnd.ms-powerpoint", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    // Zip file
    private static File getZipDocument() {
        return getFileFromResource("test-data/hello.zip");
    }

    @Test
    public void testZipDocumentFromFile() throws Exception {
        assertEquals("application/zip", mimetypeRegistry.getMimetypeFromFile(getZipDocument()));
    }

    @Test
    public void testZipDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getZipDocument());
        assertEquals("application/zip", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    // Ms Office Visio
    @Test
    public void testVisioDocument() {
        assertEquals("application/visio", mimetypeRegistry.getMimetypeFromExtension("vsdx"));
        assertEquals("application/visio", mimetypeRegistry.getMimetypeFromExtension("vsd"));
        assertEquals("application/visio", mimetypeRegistry.getMimetypeFromExtension("vst"));
        assertEquals("application/visio", mimetypeRegistry.getMimetypeFromExtension("vst"));
        assertEquals("application/visio", mimetypeRegistry.getMimetypeFromFilename("test-data/hello.vsd"));
        assertEquals("application/visio",
                mimetypeRegistry.getMimetypeFromFile(getFileFromResource("test-data/hello.vsd")));
    }

    @Test
    public void testCustomMimetypExtensions() {
        assertEquals("application/octet-stream", mimetypeRegistry.getMimetypeFromExtension("rvt"));
        assertEquals("application/octet-stream", mimetypeRegistry.getMimetypeFromExtension("dlx"));
    }

    // CSV file
    @Test
    public void testCsvDocument() throws Exception {
        assertEquals("text/csv", mimetypeRegistry.getMimetypeFromExtension("csv"));
        assertEquals("text/csv", mimetypeRegistry.getMimetypeFromFilename("test-data/test.csv"));
        assertEquals("text/csv", mimetypeRegistry.getMimetypeFromFile(getFileFromResource("test-data/test.csv")));
    }

    // OpenDocument Writer
    private static File getODFwriterDocument() {
        return getFileFromResource("test-data/hello.odt");
    }

    public void xtestODFwriterDocumentFromFile() throws Exception {
        System.out.println(mimetypeRegistry.getMimetypeFromFile(getODFwriterDocument()));
        assertEquals("application/vnd.oasis.opendocument.text",
                mimetypeRegistry.getMimetypeFromFile(getODFwriterDocument()));
    }

    public void xtestODFwriterDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getODFwriterDocument());
        assertEquals("application/vnd.oasis.opendocument.text", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    // OpenDocument Spreadsheet
    private static File getODFspreadsheetDocument() {
        return getFileFromResource("test-data/hello.ods");
    }

    public void xtestODFspreadsheetDocumentFromFile() throws Exception {
        assertEquals("application/vnd.oasis.opendocument.spreadsheet",
                mimetypeRegistry.getMimetypeFromFile(getODFspreadsheetDocument()));
    }

    public void xtestODFspreadsheetDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getODFspreadsheetDocument());
        assertEquals("application/vnd.oasis.opendocument.spreadsheet", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    // OpenDocument Presentation
    private static File getODFpresentationDocument() {
        return getFileFromResource("test-data/hello.odp");
    }

    public void xtestODFpresentationDocumentFromFile() throws Exception {
        mimetypeRegistry = new MimetypeRegistryService();
        assertEquals("application/vnd.oasis.opendocument.presentation",
                mimetypeRegistry.getMimetypeFromFile(getODFpresentationDocument()));
    }

    public void xtestODFpresentationDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getODFpresentationDocument());
        assertEquals("application/vnd.oasis.opendocument.presentation", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    // MSO 2003 XML Excel
    private static File getMso2003XmlExcelDocument() {
        return getFileFromResource("test-data/TestExcel2003AsXML.xml.txt");
    }

    public void xtestMso2003XmlExcelDocumentFromFile() throws Exception {
        assertEquals("application/vnd.ms-excel", mimetypeRegistry.getMimetypeFromFile(getMso2003XmlExcelDocument()));
    }

    public void xtestMso2003XmlExcelDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getMso2003XmlExcelDocument());
        assertEquals("application/vnd.ms-excel", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    // MSO 2003 XML Word
    private static File getMso2003XmlWordDocument() {
        return getFileFromResource("test-data/TestWord2003AsXML.xml.txt");
    }

    public void xtestMso2003XmlWordDocumentFromFile() throws Exception {
        assertEquals("application/msword", mimetypeRegistry.getMimetypeFromFile(getMso2003XmlWordDocument()));
    }

    public void xtestMso2003XmlWordDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getMso2003XmlWordDocument());
        assertEquals("application/msword", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    // Pure XML Document
    private static File getXmlDocument() {
        return getFileFromResource("test-data/simple.xml");
    }

    @Test
    public void testXmlDocumentFromFile() throws Exception {
        assertEquals("text/xml", mimetypeRegistry.getMimetypeFromFile(getXmlDocument()));
    }

    // OOo 1.x Writer
    private static File getOOowriterDocument() {
        return getFileFromResource("test-data/hello.sxw");
    }

    public void xtestOOowriterDocumentFromFile() throws Exception {
        assertEquals("application/vnd.sun.xml.writer", mimetypeRegistry.getMimetypeFromFile(getOOowriterDocument()));
    }

    public void xtestOOowriterDocumentFromStream() throws Exception {
        InputStream stream = new FileInputStream(getOOowriterDocument());
        assertEquals("application/vnd.sun.xml.writer", mimetypeRegistry.getMimetypeFromStream(stream));
    }

    // OOo special EMF graphic file
    private static File getOOoEmfDocument() {
        return getFileFromResource("test-data/graphic_ooo.vclmtf");
    }

    public void xtestOOoEMFDocumentFromFile() throws Exception {
        assertEquals("application/x-vclmtf", mimetypeRegistry.getMimetypeFromFile(getOOoEmfDocument()));
    }

    // EMF graphic file
    private static File getEmfDocument() {
        return getFileFromResource("test-data/graphic.emf");
    }

    public void xtestEMFDocumentFromFile() throws Exception {
        assertEquals("application/x-emf", mimetypeRegistry.getMimetypeFromFile(getEmfDocument()));
    }

    /**
     * @Test public void testBigBinFromFile() throws Exception { long t0 = System.currentTimeMillis(); String mt
     *       =mimetypeRegistry.getMimetypeFromFile(new File("/tmp/file-050MB.funky")); long t1 =
     *       System.currentTimeMillis(); System.out.println(mt); System.out.println(t1-t0); }
     **/

}
