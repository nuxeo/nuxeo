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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.transform.xslt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.DocumentTestUtils;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.xslt.api.XSLTPlugin;

/**
 * Test the XSLTPlugin requesting the transformer service.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class TestXSLTTransformerService extends AbstractXSLTPluginTest {

    private static final String TRANSFORMER_NAME = PLUGIN_NAME;

    public void testSimpleHtmlXSLTransformation() throws Exception {
        final String xmlSource = "test-data/xslt/test-simple-html.xml";
        final String xslSource = "test-data/xslt/test-simple-html.xsl";
        final String expectedSource = "test-data/xslt/test-simple-html.expected";

        final Map<String, Serializable> pluginOptions = new HashMap<String, Serializable>();
        pluginOptions.put(XSLTPlugin.OPTION_STYLESHEET, (FileBlob) getBlobFromPath(xslSource));
        final Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String, Serializable>>();
        options.put(PLUGIN_NAME, pluginOptions);

        final List<TransformDocument> results = service.transform(
                TRANSFORMER_NAME, options, getBlobFromPath(xmlSource));

        final Blob result = results.get(0).getBlob();
        assertEquals("wrong mimetype", "text/html", result.getMimeType());

        final InputStream expected = new FileInputStream(
                FileUtils.getResourceFileFromContext(expectedSource));

        assertTrue("xml content", compareXML(result.getStream(), expected));
    }

    public void testSimpleTextXSLTransformation() throws Exception {
        final String xmlSource = "test-data/xslt/test-simple-text.xml";
        final String xslSource = "test-data/xslt/test-simple-text.xsl";

        final Map<String, Serializable> pluginOptions = new HashMap<String, Serializable>();
        pluginOptions.put(XSLTPlugin.OPTION_STYLESHEET, (FileBlob) getBlobFromPath(xslSource));
        final Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String, Serializable>>();
        options.put(PLUGIN_NAME, pluginOptions);

        final List<TransformDocument> results = service.transform(
                TRANSFORMER_NAME, options, getBlobFromPath(xmlSource));

        assertEquals("wrong mimetype", "text/plain",
                results.get(0).getBlob().getMimeType());
        File textFile = getFileFromInputStream(
                results.get(0).getBlob().getStream(), "txt");
        assertEquals("text content", "Hello Bob!",
                DocumentTestUtils.readContent(textFile));
    }

    public void testSimpleXMLXSLTransformation() throws Exception {
        final String xmlSource = "test-data/xslt/test-simple-xml.xml";
        final String xslSource = "test-data/xslt/test-simple-xml.xsl";
        final String expectedSource = "test-data/xslt/test-simple-xml.expected";

        final Map<String, Serializable> pluginOptions = new HashMap<String, Serializable>();
        pluginOptions.put(XSLTPlugin.OPTION_STYLESHEET, (FileBlob) getBlobFromPath(xslSource));
        final Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String, Serializable>>();
        options.put(PLUGIN_NAME, pluginOptions);

        final List<TransformDocument> results = service.transform(
                TRANSFORMER_NAME, options, getBlobFromPath(xmlSource));

        final Blob result = results.get(0).getBlob();
        assertEquals("wrong mimetype", "text/xml", result.getMimeType());

        final InputStream expected = new FileInputStream(
                FileUtils.getResourceFileFromContext(expectedSource));

        assertTrue("text content", compareXML(result.getStream(), expected));
    }

    public void testXMLXSLTransformation() throws Exception {
        final String xmlSource = "test-data/xslt/test-xml.xml";
        final String xslSource = "test-data/xslt/test-xml.xsl";
        final String expectedSource = "test-data/xslt/test-xml.expected";

        final Map<String, Serializable> pluginOptions = new HashMap<String, Serializable>();
        pluginOptions.put(XSLTPlugin.OPTION_STYLESHEET, (FileBlob) getBlobFromPath(xslSource));
        final Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String, Serializable>>();
        options.put(PLUGIN_NAME, pluginOptions);

        final List<TransformDocument> results = service.transform(
                TRANSFORMER_NAME, options, getBlobFromPath(xmlSource));

        final Blob result = results.get(0).getBlob();
        assertEquals("wrong mimetype", "text/xml", result.getMimeType());

        final InputStream expected = new FileInputStream(
                FileUtils.getResourceFileFromContext(expectedSource));

        assertTrue("text content", compareXML(result.getStream(), expected));
    }

    public void testNoOutputMethod() throws Exception {
        final String xmlSource = "test-data/xslt/test-no-output-method.xml";
        final String xslSource = "test-data/xslt/test-no-output-method.xsl";
        final String expectedSource = "test-data/xslt/test-no-output-method.expected";

        final Map<String, Serializable> pluginOptions = new HashMap<String, Serializable>();
        pluginOptions.put(XSLTPlugin.OPTION_STYLESHEET, (FileBlob) getBlobFromPath(xslSource));
        final Map<String, Map<String, Serializable>> options = new HashMap<String, Map<String, Serializable>>();
        options.put(PLUGIN_NAME, pluginOptions);

        final List<TransformDocument> results = service.transform(
                TRANSFORMER_NAME, options, getBlobFromPath(xmlSource));

        final Blob result = results.get(0).getBlob();
        assertEquals("wrong mimetype", "text/xml", result.getMimeType());

        final InputStream expected = new FileInputStream(
                FileUtils.getResourceFileFromContext(expectedSource));

        assertTrue("text content", compareXML(result.getStream(), expected));
    }
}
