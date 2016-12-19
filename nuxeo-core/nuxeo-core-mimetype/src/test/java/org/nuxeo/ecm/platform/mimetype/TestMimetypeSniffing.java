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

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.mimetype.service.MimetypeRegistryService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

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

    private static File getTextDocument() {
        return FileUtils.getResourceFileFromContext("test-data/hello.txt");
    }

    @Test
    public void testTextDocumentFromFile() throws Exception {
        assertEquals("text/plain", mimetypeRegistry.getMimetypeFromFile(getTextDocument()));
    }

    private static File getWordDocument() {
        return FileUtils.getResourceFileFromContext("test-data/hello.doc");
    }

    @Test
    public void testWordDocumentFromFile() throws Exception {
        assertEquals("application/msword", mimetypeRegistry.getMimetypeFromFile(getWordDocument()));
    }

    private static File getExcelDocument() {
        return getFileFromResource("test-data/hello.xls");
    }

    public void xtestExcelDocumentFromFile() throws Exception {
        assertEquals("application/vnd.ms-excel", mimetypeRegistry.getMimetypeFromFile(getExcelDocument()));
    }

    private static File getPowerpointDocument() {
        return getFileFromResource("test-data/hello.ppt");
    }

    public void xtestPowerpointDocumentFromFile() throws Exception {
        assertEquals("application/vnd.ms-powerpoint", mimetypeRegistry.getMimetypeFromFile(getPowerpointDocument()));
    }

    // Zip file
    private static File getZipDocument() {
        return getFileFromResource("test-data/hello.zip");
    }

    @Test
    public void testZipDocumentFromFile() throws Exception {
        assertEquals("application/zip", mimetypeRegistry.getMimetypeFromFile(getZipDocument()));
    }

    // Ms Office Visio
    public void xtestVisioDocument() throws Exception {
        assertEquals("getMimetypeFromExtension vsd", "application/visio",
                mimetypeRegistry.getMimetypeFromExtension("vsd"));
        assertEquals("getMimetypeFromExtension vst", "application/visio",
                mimetypeRegistry.getMimetypeFromExtension("vst"));
        assertEquals("getMimetypeFromFilename", "application/visio",
                mimetypeRegistry.getMimetypeFromFilename("test-data/hello.vsd"));
        assertEquals("getMimetypeFromFile", "application/visio",
                mimetypeRegistry.getMimetypeFromFile(getFileFromResource("test-data/hello.vsd")));
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

    // OpenDocument Spreadsheet
    private static File getODFspreadsheetDocument() {
        return getFileFromResource("test-data/hello.ods");
    }

    public void xtestODFspreadsheetDocumentFromFile() throws Exception {
        assertEquals("application/vnd.oasis.opendocument.spreadsheet",
                mimetypeRegistry.getMimetypeFromFile(getODFspreadsheetDocument()));
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

    // MSO 2003 XML Excel
    private static File getMso2003XmlExcelDocument() {
        return getFileFromResource("test-data/TestExcel2003AsXML.xml.txt");
    }

    public void xtestMso2003XmlExcelDocumentFromFile() throws Exception {
        assertEquals("application/vnd.ms-excel", mimetypeRegistry.getMimetypeFromFile(getMso2003XmlExcelDocument()));
    }

    // MSO 2003 XML Word
    private static File getMso2003XmlWordDocument() {
        return getFileFromResource("test-data/TestWord2003AsXML.xml.txt");
    }

    public void xtestMso2003XmlWordDocumentFromFile() throws Exception {
        assertEquals("application/msword", mimetypeRegistry.getMimetypeFromFile(getMso2003XmlWordDocument()));
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
