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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.plugin.wordml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.transform.AbstractPluginTestCase;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.interfaces.Transformer;
import org.nuxeo.ecm.platform.transform.plugin.wordml.SimpleXmlComparator;
import org.nuxeo.ecm.platform.transform.plugin.wordml.XmlTransformerConstants;
import org.xml.sax.SAXException;

/**
 * Test the wordML transformation plugin.
 *
 * @author DM
 */
public class TestXmlTransformer extends AbstractPluginTestCase {

    private Transformer transformer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        transformer = service.getTransformerByName(XmlTransformerConstants.TRANSFORMER_NAME);
    }

    @Override
    public void tearDown() throws Exception {
        transformer = null;
        super.tearDown();
    }

    /**
     * Tests if the same data given as input is returned when no options are
     * specified.
     */
    public void testXMLSameTransformation() throws Exception {
        String path = "test-data/wordml/hello-simple.xml";
        //String pathExpectedResult = "test-data/wordml/hello-simple-result.xml";
        String pathExpectedResult = "test-data/wordml/hello-simple.xml";

        List<TransformDocument> results = transformer.transform(null,
                new TransformDocumentImpl(getBlobFromPath(path), "text/xml"));

        assertNotNull(results);
        assertEquals(1, results.size());

        compareXml(pathExpectedResult, results.get(0).getBlob().getStream());
    }

    public void testXMLTransformationWithOptions() throws Exception {
        String path = "test-data/wordml/hello-simple.xml";
        String pathExpectedResult = "test-data/wordml/hello-simple-result.xml";

        final Map<String, Serializable> fieldsValueMap = new HashMap<String, Serializable>();
        fieldsValueMap.put("o:Revision", "02.15");
        fieldsValueMap.put("o:CreateurDocument", "DM");

        final Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String, Serializable>>();
        options.put(XmlTransformerConstants.PLUGIN_NAME, fieldsValueMap);
        final List<TransformDocument> results = transformer.transform(options,
                new TransformDocumentImpl(getBlobFromPath(path), "text/xml"));

        assertNotNull(results);
        assertEquals(1, results.size());

        compareXml(pathExpectedResult, results.get(0).getBlob().getStream());
    }

    private static void compareXml(final String expectedResultFilePath,
            final InputStream resultStream) throws IOException, SAXException,
            ParserConfigurationException {

        final File textFile = getFileFromInputStream(resultStream, "txt");
        textFile.deleteOnExit();
        final String resultTxt = DocumentTestUtils.readContent(textFile);

        final byte[] expectedResultData = readFileContent(expectedResultFilePath);

        InputStream outcomeBais = new FileInputStream(textFile);
        InputStream expectedBais = new ByteArrayInputStream(expectedResultData);

        boolean eq = SimpleXmlComparator.compareXmlDocs(expectedBais,
                outcomeBais);
        String exp = new String(readFileContent(expectedResultFilePath));
        String out = resultTxt;
        assertTrue("Expected: \n" + exp  + "\n------\n" + out, eq);

        // assertEquals("transformed content", new String(expectedResultData),
        // resultTxt);
    }

    private static byte[] readFileContent(String path) throws IOException {
        final InputStream expectedResultIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                path);

        return FileUtils.readBytes(expectedResultIs);
    }

}
