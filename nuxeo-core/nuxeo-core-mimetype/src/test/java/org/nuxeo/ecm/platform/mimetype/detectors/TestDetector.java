/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.mimetype.detectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

public class TestDetector {

    private static File getFileFromTestResource(String resource) {
        // retrieves contextually the resource file and decode its path
        // returns the corresponding File Object
        return org.nuxeo.common.utils.FileUtils.getResourceFileFromContext(resource);
    }

    /* Test XML binary file */
    @Test
    public void testSnifferXLSMimetype() {
        XlsMimetypeSniffer sniffer = new XlsMimetypeSniffer();

        // verify that the first returned is the one supported by Nx5
        String[] supportedMimetype = sniffer.getHandledTypes();
        assertEquals("application/vnd.ms-excel", supportedMimetype[0]);
    }

    @Test
    public void testSnifferXLSDirectGuess() {
        File xlsFile = getFileFromTestResource("test-data/hello.xls");
        XlsMimetypeSniffer sniffer = new XlsMimetypeSniffer();

        // direct access
        String[] returnedMimetype = sniffer.guessExcel(xlsFile);
        assertTrue(returnedMimetype.length > 0);
    }

    @Test
    public void testSnifferXLSByByteArray() throws IOException {
        File xlsFile = getFileFromTestResource("test-data/hello.xls");
        XlsMimetypeSniffer sniffer = new XlsMimetypeSniffer();

        // by byte[]
        byte[] data = FileUtils.readFileToByteArray(xlsFile);
        String[] returnedByteMimetype = sniffer.process(data, 0, 0, 0L, 'd', "dummy", new HashMap());
        assertTrue(returnedByteMimetype.length > 0);
    }

    @Test
    public void testSnifferXLSWrongFile() {
        XlsMimetypeSniffer sniffer = new XlsMimetypeSniffer();

        // a wrong file is not detected as excel file
        File otherFile = getFileFromTestResource("test-data/hello.doc");
        String[] mimetype = sniffer.guessExcel(otherFile);
        assertEquals(0, mimetype.length);
    }

    /* Test Msoffice 2003 XML Word & Excel file */
    @Test
    public void testSnifferMsoXMLMimetype() {
        MsoXmlMimetypeSniffer sniffer = new MsoXmlMimetypeSniffer();

        // verify that the first returned is the one supported by Nx5
        String[] supportedMimetypes = sniffer.getHandledTypes();
        assertEquals("application/vnd.ms-excel", supportedMimetypes[0]);
        assertEquals("application/msword", supportedMimetypes[1]);
    }

    @Test
    public void testSnifferMsoXMLDirectGuess() {
        MsoXmlMimetypeSniffer sniffer = new MsoXmlMimetypeSniffer();

        // xml excel 2003
        File xmlFile = getFileFromTestResource("test-data/TestExcel2003AsXML.xml.txt");
        String[] mimetype = sniffer.guessMsoXml(xmlFile);
        assertTrue(mimetype.length > 0);
        assertEquals("application/vnd.ms-excel", mimetype[0]);

        // xml word 2003
        xmlFile = getFileFromTestResource("test-data/TestWord2003AsXML.xml.txt");
        mimetype = sniffer.guessMsoXml(xmlFile);
        assertEquals("application/msword", mimetype[0]);
    }

    @Test
    public void testSnifferMsoXMLByByteArray() throws IOException {

        MsoXmlMimetypeSniffer sniffer = new MsoXmlMimetypeSniffer();

        // by byte[]
        File xmlFile = getFileFromTestResource("test-data/TestWord2003AsXML.xml.txt");
        byte[] data = FileUtils.readFileToByteArray(xmlFile);
        String[] returnedByteMimetype = sniffer.process(data, 0, 0, 0L, 'd', "dummy", new HashMap());
        assertEquals("application/msword", returnedByteMimetype[0]);
    }

    @Test
    public void testSnifferMsoXMLWrongFile() {
        MsoXmlMimetypeSniffer sniffer = new MsoXmlMimetypeSniffer();

        // a wrong file is not detected as excel file
        File otherFile = getFileFromTestResource("test-data/hello.doc");
        String[] returnedMimetype = sniffer.guessMsoXml(otherFile);
        assertEquals(0, returnedMimetype.length);
    }

    /* Test OpenDocument & OOo 1.x */
    @Test
    public void testSnifferODFWrongFile() {
        OOoMimetypeSniffer sniffer = new OOoMimetypeSniffer();

        // a wrong file is not detected as odf file
        File otherFile = getFileFromTestResource("test-data/hello.doc");
        String[] returnedMimetype = sniffer.guessOOo(otherFile);
        assertEquals(0, returnedMimetype.length);
    }

    @Test
    public void testSnifferOOoMimetype() {
        OOoMimetypeSniffer sniffer = new OOoMimetypeSniffer();

        // verify that the first returned is the one supported by Nx5
        String[] supportedMimetype = sniffer.getHandledTypes();
        assertEquals("application/vnd.oasis.opendocument.text", supportedMimetype[2]);
        assertEquals("application/vnd.oasis.opendocument.spreadsheet", supportedMimetype[0]);
        assertEquals("application/vnd.oasis.opendocument.presentation", supportedMimetype[4]);
        // OOo 1.x
        assertEquals("application/vnd.sun.xml.writer", supportedMimetype[16]);
        assertEquals("application/vnd.sun.xml.calc", supportedMimetype[19]);
        assertEquals("application/vnd.sun.xml.impress", supportedMimetype[21]);
    }

    @Test
    public void testSnifferOOoDirectGuess() {
        OOoMimetypeSniffer sniffer = new OOoMimetypeSniffer();

        File file = getFileFromTestResource("test-data/hello.odt");
        String[] returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.oasis.opendocument.text", returnedMimetype[0]);

        file = getFileFromTestResource("test-data/hello.ods");
        returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.oasis.opendocument.spreadsheet", returnedMimetype[0]);

        file = getFileFromTestResource("test-data/hello.odp");
        returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.oasis.opendocument.presentation", returnedMimetype[0]);

        // OOo1.x
        file = getFileFromTestResource("test-data/hello.sxw");
        returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.sun.xml.writer", returnedMimetype[0]);

        file = getFileFromTestResource("test-data/hello.sxc");
        returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.sun.xml.calc", returnedMimetype[0]);

        file = getFileFromTestResource("test-data/hello.sxi");
        returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.sun.xml.impress", returnedMimetype[0]);
    }

    @Test
    public void testSnifferOOoByByteArray() throws IOException {
        OOoMimetypeSniffer sniffer = new OOoMimetypeSniffer();

        // by byte[]
        File file = getFileFromTestResource("test-data/hello.odt");
        byte[] data = FileUtils.readFileToByteArray(file);
        // FIXME: no need for these variables
        int dummyInt = 0;
        long dummyLong = 0;
        Map dummyMap = new HashMap();
        String dummyString = "dummy";
        char dummyChar = dummyString.charAt(0);

        String[] returnedByteMimetype = sniffer.process(data, dummyInt, dummyInt, dummyLong, dummyChar, dummyString,
                dummyMap);
        assertEquals("application/vnd.oasis.opendocument.text", returnedByteMimetype[0]);
    }

    /* Test Powerpoint binary file */
    @Test
    public void testSnifferPPTMimetype() {
        PptMimetypeSniffer sniffer = new PptMimetypeSniffer();

        // verify that the first returned is the one supported by Nx5
        String[] supportedMimetype = sniffer.getHandledTypes();
        assertEquals("application/vnd.ms-powerpoint", supportedMimetype[0]);
    }

    // TODO: fix and reactivate
    @Test
    @Ignore
    public void testSnifferPPTDirectGuess() {
        File pptFile = getFileFromTestResource("test-data/hello.ppt");
        PptMimetypeSniffer sniffer = new PptMimetypeSniffer();

        // direct access
        String[] returnedMimetype = sniffer.guessPowerpoint(pptFile);
        assertTrue(returnedMimetype.length > 0);
    }

    // TODO: fix and reactivate
    @Test
    @Ignore
    public void testSnifferPPTByByteArray() throws IOException {
        File pptFile = getFileFromTestResource("test-data/hello.ppt");
        PptMimetypeSniffer sniffer = new PptMimetypeSniffer();

        // by byte[]
        byte[] data = FileUtils.readFileToByteArray(pptFile);
        // FIXME: no need for these variables
        int dummyInt = 0;
        long dummyLong = 0;
        Map dummyMap = new HashMap();
        String dummyString = "dummy";
        char dummyChar = dummyString.charAt(0);

        String[] returnedByteMimetype = sniffer.process(data, dummyInt, dummyInt, dummyLong, dummyChar, dummyString,
                dummyMap);
        assertTrue(returnedByteMimetype.length > 0);
    }

    @Test
    public void testSnifferPPTWrongFile() {
        PptMimetypeSniffer sniffer = new PptMimetypeSniffer();

        // a wrong file is not detected a a powerpoint file
        File otherFile = getFileFromTestResource("test-data/hello.doc");
        String[] mimetype = sniffer.guessPowerpoint(otherFile);
        assertEquals(0, mimetype.length);
    }

}
