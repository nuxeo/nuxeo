/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: TestDetector.java 30392 2008-02-21 06:49:19Z sfermigier $
 */

package org.nuxeo.ecm.platform.mimetype.detectors;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.common.utils.FileUtils;

public class TestDetector extends TestCase {

    private static File getFileFromTestResource(String resource) {
        // retrieves contextually the resource file and decode its path
        // returns the corresponding File Object
        return FileUtils.getResourceFileFromContext(resource);
    }

    /* Test XML binary file */

    public void testSnifferXLSMimetype() {
        XlsMimetypeSniffer sniffer = new XlsMimetypeSniffer();

        // verify that the first returned is the one supported by Nx5
        String[] supportedMimetype = sniffer.getHandledTypes();
        assertEquals("application/vnd.ms-excel", supportedMimetype[0]);
    }

    public void testSnifferXLSDirectGuess() {
        File xlsFile = getFileFromTestResource("test-data/hello.xls");
        XlsMimetypeSniffer sniffer = new XlsMimetypeSniffer();

        // direct access
        String[] returnedMimetype = sniffer.guessExcel(xlsFile);
        assertTrue(returnedMimetype.length > 0);
    }

    public void testSnifferXLSByByteArray() throws IOException {
        File xlsFile = getFileFromTestResource("test-data/hello.xls");
        XlsMimetypeSniffer sniffer = new XlsMimetypeSniffer();

        // by byte[]
        byte[] data = FileUtils.readBytes(xlsFile);
        String[] returnedByteMimetype = sniffer.process(data, 0, 0, 0L, 'd',
                "dummy", new HashMap());
        assertTrue(returnedByteMimetype.length > 0);
    }

    public void testSnifferXLSWrongFile() {
        XlsMimetypeSniffer sniffer = new XlsMimetypeSniffer();

        //a wrong file is not detected as excel file
        File otherFile = getFileFromTestResource("test-data/hello.doc");
        String[] mimetype = sniffer.guessExcel(otherFile);
        assertEquals(0, mimetype.length);
    }

    /* Test Msoffice 2003 XML Word & Excel file */

    public void testSnifferMsoXMLMimetype() {
        MsoXmlMimetypeSniffer sniffer = new MsoXmlMimetypeSniffer();

        // verify that the first returned is the one supported by Nx5
        String[] supportedMimetypes = sniffer.getHandledTypes();
        assertEquals("application/vnd.ms-excel", supportedMimetypes[0]);
        assertEquals("application/msword", supportedMimetypes[1]);
    }

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

    public void testSnifferMsoXMLByByteArray() throws IOException {

        MsoXmlMimetypeSniffer sniffer = new MsoXmlMimetypeSniffer();

        // by byte[]
        String [] returnedByteMimetype = {};
        File xmlFile = getFileFromTestResource("test-data/TestWord2003AsXML.xml.txt");
        byte[] data = FileUtils.readBytes(xmlFile);
        returnedByteMimetype = sniffer.process(data, 0, 0, 0L, 'd', "dummy",
                new HashMap());
        assertEquals("application/msword", returnedByteMimetype[0]);
    }

    public void testSnifferMsoXMLWrongFile() {
        MsoXmlMimetypeSniffer sniffer = new MsoXmlMimetypeSniffer();

        // a wrong file is not detected as excel file
        File otherFile = getFileFromTestResource("test-data/hello.doc");
        String[] returnedMimetype = sniffer.guessMsoXml(otherFile);
        assertEquals(0, returnedMimetype.length);
    }

    /* Test OpenDocument & OOo 1.x */

    public void testSnifferODFWrongFile() {
        OOoMimetypeSniffer sniffer = new OOoMimetypeSniffer();

        // a wrong file is not detected as odf file
        File otherFile = getFileFromTestResource("test-data/hello.doc");
        String[] returnedMimetype = sniffer.guessOOo(otherFile);
        assertEquals(0, returnedMimetype.length);
    }

    public void testSnifferOOoMimetype() {
        OOoMimetypeSniffer sniffer = new OOoMimetypeSniffer();

        // verify that the first returned is the one supported by Nx5
        String[] supportedMimetype = sniffer.getHandledTypes();
        assertEquals("application/vnd.oasis.opendocument.text",
                supportedMimetype[2]);
        assertEquals("application/vnd.oasis.opendocument.spreadsheet",
                supportedMimetype[0]);
        assertEquals("application/vnd.oasis.opendocument.presentation",
                supportedMimetype[4]);
        // OOo 1.x
        assertEquals("application/vnd.sun.xml.writer", supportedMimetype[16]);
        assertEquals("application/vnd.sun.xml.calc", supportedMimetype[19]);
        assertEquals("application/vnd.sun.xml.impress", supportedMimetype[21]);
    }

    public void testSnifferOOoDirectGuess() {
        OOoMimetypeSniffer sniffer = new OOoMimetypeSniffer();

        File file = getFileFromTestResource("test-data/hello.odt");
        String[] returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.oasis.opendocument.text",
                returnedMimetype[0]);

        file = getFileFromTestResource("test-data/hello.ods");
        returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.oasis.opendocument.spreadsheet",
                returnedMimetype[0]);

        file = getFileFromTestResource("test-data/hello.odp");
        returnedMimetype = sniffer.guessOOo(file);
        assertTrue(returnedMimetype.length > 0);
        assertEquals("application/vnd.oasis.opendocument.presentation",
                returnedMimetype[0]);

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

    public void testSnifferOOoByByteArray() throws IOException {
        OOoMimetypeSniffer sniffer = new OOoMimetypeSniffer();

        // by byte[]
        File file = getFileFromTestResource("test-data/hello.odt");
        byte[] data = FileUtils.readBytes(file);
        // FIXME: no need for these variables
        int dummyInt = 0;
        long dummyLong = 0;
        Map dummyMap = new HashMap();
        String dummyString = "dummy";
        char dummyChar = dummyString.charAt(0);

        String[] returnedByteMimetype = sniffer.process(data, dummyInt,
                dummyInt, dummyLong, dummyChar, dummyString, dummyMap);
        assertEquals("application/vnd.oasis.opendocument.text",
                returnedByteMimetype[0]);
    }

    /* Test Powerpoint binary file */

    public void testSnifferPPTMimetype() {
        PptMimetypeSniffer sniffer = new PptMimetypeSniffer();

        // verify that the first returned is the one supported by Nx5
        String[] supportedMimetype = sniffer.getHandledTypes();
        assertEquals("application/vnd.ms-powerpoint", supportedMimetype[0]);
    }

    // TODO: fix and reactivate
    public void XXXtestSnifferPPTDirectGuess() {
        File pptFile = getFileFromTestResource("test-data/hello.ppt");
        PptMimetypeSniffer sniffer = new PptMimetypeSniffer();

        // direct access
        String[] returnedMimetype = sniffer.guessPowerpoint(pptFile);
        assertTrue(returnedMimetype.length > 0);
    }

    // TODO: fix and reactivate
    public void XXXtestSnifferPPTByByteArray() throws IOException {
        File pptFile = getFileFromTestResource("test-data/hello.ppt");
        PptMimetypeSniffer sniffer = new PptMimetypeSniffer();

        // by byte[]
        byte[] data = FileUtils.readBytes(pptFile);
        // FIXME: no need for these variables
        int dummyInt = 0;
        long dummyLong = 0;
        Map dummyMap = new HashMap();
        String dummyString = "dummy";
        char dummyChar = dummyString.charAt(0);

        String[] returnedByteMimetype = sniffer.process(data, dummyInt,
                dummyInt,
                dummyLong, dummyChar, dummyString, dummyMap);
        assertTrue(returnedByteMimetype.length > 0);
    }

    public void testSnifferPPTWrongFile() {
        PptMimetypeSniffer sniffer = new PptMimetypeSniffer();

        // a wrong file is not detected a a powerpoint file
        File otherFile = getFileFromTestResource("test-data/hello.doc");
        String[] mimetype = sniffer.guessPowerpoint(otherFile);
        assertEquals(0, mimetype.length);
    }

}
