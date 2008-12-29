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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.api.Framework;
import org.w3c.dom.Document;

/**
 *
 * @author DM
 *
 */
class XmlDataInjector {

    private static final Log log = LogFactory.getLog(XmlDataInjector.class);

    // :FIXME:
    private static final String xslChunkFile = "org/nuxeo/ecm/platform/transform/plugin/wordml/xsl/element-chunk.xsl";
    private static final String xslFieldNameToReplace = "${field_name}";
    private static final String xslFieldValueToReplace = "${field_value}";

    // :FIXME:
    private static final String xslMasterFile = "org/nuxeo/ecm/platform/transform/plugin/wordml/xsl/identity-trans.xsl";
    private static final String xslTxtToReplace = "${fields_value_inject_templates}";

    private final Map<String, Serializable> fieldValues;

    /**
     * Caching the XSL chunk template.
     */
    private String chunkTemplate;

    XmlDataInjector(Map<String, Serializable> fieldValues) {
        this.fieldValues = fieldValues;
    }

    /**
     * Transforms the doc according with the field/values Map given in the
     * constructor.
     */
    public void transform(Document inDoc, OutputStream ostream)
            throws TransformerException, IOException {

        final Transformer transformer = createTransformer();

        transformer.transform(new DOMSource(inDoc), new StreamResult(ostream));
        // xmlDoc.getTextContent();
    }

    private Transformer createTransformer()
            throws TransformerConfigurationException, IOException {
        TransformerFactory tFactory = TransformerFactory.newInstance();

        final Source xslSource;
        // FIXME empty map in createXslSource is not equivalent to identify
        // transformation and thus do not return the expecting result.
        if (fieldValues == null || fieldValues.keySet().isEmpty()) {
            log.debug("<createTransformer> null options, use identity Xsl transformation");
            xslSource = getXslSource();
        } else {
            xslSource = createXslSource();
        }

        return tFactory.newTransformer(xslSource);
    }

    private static Source getXslSource() {
        final String xslFilePath = "test-data/wordml/identity-trans.xsl";

        final URL xslURL = Thread.currentThread().getContextClassLoader().getResource(
                xslFilePath);

        if (null == xslURL) {
            log.error("resource not found: " + xslFilePath);
            return null;
        }
        log.debug("xsl url: " + xslURL);
        File xslFile = new File(xslURL.getFile());
        return new StreamSource(xslFile);
    }

    private Source createXslSource() throws IOException {
        final String logPrefix = "<createXslSource> ";
        final String masterXslTemplate = readFile(xslMasterFile);
        final String xslChunks = getAllChunks(fieldValues);

        log.debug(logPrefix + "xslChunks: \n" + xslChunks);

        final String masterXsl = replace(xslTxtToReplace, xslChunks,
                masterXslTemplate);

        log.debug(logPrefix + "masterXsl: \n" + masterXsl);

        return new StreamSource(new StringReader(masterXsl));
    }

    private String getAllChunks(final Map<String, Serializable> fieldValues)
            throws IOException {
        final String logPrefix = "<getAllChunks> ";

        final StringBuilder buf = new StringBuilder();

        final Set<Entry<String, Serializable>> entries = fieldValues.entrySet();
        for (Entry<String, Serializable> entry : entries) {
            final String key = entry.getKey();
            final Serializable value = entry.getValue();

            log.debug(logPrefix + "get chunk for key=" + key + ", value="
                    + value);
            final String xslChunk = getXslChunkForField(key, value.toString());

            buf.append(xslChunk);
        }

        return buf.toString();
    }

    private String getXslChunkForField(String fieldname, String value)
            throws IOException {

        if (chunkTemplate == null) {
            chunkTemplate = readFile(xslChunkFile);
        }
        final String xslChunkTemplate = chunkTemplate;

        log.debug("xsl chunk template: \n" + xslChunkTemplate);
        String xslChunk = replace(xslFieldNameToReplace, fieldname,
                xslChunkTemplate);
        log.debug("xsl chunk         : \n" + xslChunk);
        xslChunk = replace(xslFieldValueToReplace, value, xslChunk);
        return xslChunk;
    }

    private static String readFile(String filePath) throws IOException {
        // final InputStream instream = Thread.currentThread()
        // .getContextClassLoader().getResourceAsStream(filePath);
        InputStream instream = XmlDataInjector.class.getClassLoader().getResourceAsStream(
                filePath);
        if (instream == null) {
            // throw new IOException("cannot find file: " + filePath);

            final URL fileUrl = Framework.getRuntime().getContext().getResource(
                    filePath);
            log.debug("fileUrl: " + fileUrl);
            if (fileUrl == null) {
                throw new IOException("cannot find file: " + filePath);
            }
            instream = fileUrl.openStream();
        }

        final byte[] data = FileUtils.readBytes(instream);

        return new String(data);
    }

    private static String replace(String oldStr, String newStr, String inString) {
        int start = inString.indexOf(oldStr);
        if (start == -1) {
            return inString;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(inString.substring(0, start));
        sb.append(newStr);
        sb.append(inString.substring(start + oldStr.length()));
        return sb.toString();
    }

}
