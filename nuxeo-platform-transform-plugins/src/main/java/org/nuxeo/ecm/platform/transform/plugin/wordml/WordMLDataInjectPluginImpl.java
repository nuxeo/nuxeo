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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Injects fields values into XML type document (wordML).
 *
 * @author DM
 *
 */
public class WordMLDataInjectPluginImpl extends AbstractPlugin implements
        WordMLDataInjectPlugin {

    private static final long serialVersionUID = -7522610596626881865L;

    private static final Log log = LogFactory.getLog(WordMLDataInjectPluginImpl.class);

    // TODO : make this configurable (for example passing the format as option)
    private final DateFormat tzDateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssZ");

    public WordMLDataInjectPluginImpl() {
        // Only takes XML as sources documents.
        sourceMimeTypes = new ArrayList<String>();
        sourceMimeTypes.add("text/xml");

        // Only outputs text.
        destinationMimeType = "text/xml";
    }

    private Document readXML(InputStream in) throws SAXException, IOException,
            ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(in);
    }

    private static File createTmpFile() throws IOException {
        final long time = System.currentTimeMillis();
        File tmpFile = File.createTempFile("WordMLDataInjectPlugin_" + time,
                ".xml");
        tmpFile.deleteOnExit();

        return tmpFile;
    }

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) {

        final String logPrefix = "<transform> ";

        List<TransformDocument> results = new ArrayList<TransformDocument>();

        options = convertToStringValues(options);

        try {
            results = super.transform(options, sources);
        } catch (Exception e) {
            log.error(e);
        }

        for (TransformDocument srcDocument : sources) {
            String srcMimeType;
            try {
                srcMimeType = srcDocument.getMimetype();
            } catch (Exception e) {
                log.error(e);
                srcMimeType = null;
            }
            if (srcMimeType == null || !sourceMimeTypes.contains(srcMimeType)) {
                log.debug(logPrefix
                        + "cannot apply transformer plugin for mimeType: "
                        + srcMimeType);
                continue;
            }

            final TransformDocument resultDocument = transform(options,
                    srcDocument);
            if (resultDocument != null) {
                results.add(resultDocument);
            }
        }

        return results;
    }

    /**
     * Given the fact that the received options are going to be written into a
     * WordML valid document we should adjust option values to appropriate
     * string values.
     *
     * @param options
     * @return
     */
    private Map<String, Serializable> convertToStringValues(
            Map<String, Serializable> options) {

        final Map<String, Serializable> formattedOptions = new HashMap<String, Serializable>();

        final Set<Map.Entry<String, Serializable>> entries = options.entrySet();
        for (Map.Entry<String, Serializable> entry : entries) {
            final String key = entry.getKey();
            final Serializable value = entry.getValue();

            // support for different data types
            final String stringValue;
            if (value instanceof String) {
                stringValue = (String) value;
            } else if (value instanceof Calendar) {
                stringValue = tzDateFormat.format(((Calendar) value).getTime());
            } else if (value instanceof Date) {
                stringValue = tzDateFormat.format((Date) value);
            } else {
                log.warn("using default toString for value type: "
                        + value.getClass());
                stringValue = value.toString();
            }

            if (log.isDebugEnabled()) {
                log.debug("convert for key=" + key + ", old value=" + value
                        + ", new value=" + stringValue);
            }

            formattedOptions.put(key, stringValue);
        }

        return formattedOptions;
    }

    /**
     * Transforms a single document.
     *
     * @param options
     * @param srcDoc
     * @return transformed document or <code>null</code> if an error occurs
     */
    private TransformDocument transform(Map<String, Serializable> options,
            TransformDocument srcDoc) {

        final String logPrefix = "<transform(2)> ";

        TransformDocument resultDoc;

        File tmpFile = null;
        OutputStream ostream = null;

        try {
            // XMLDocument
            Document xmlDoc = readXML(srcDoc.getBlob().getStream());

            XmlDataInjector xmlDataInjector = new XmlDataInjector(options);

            tmpFile = createTmpFile();

            log.debug(logPrefix + "created tmpFile: " + tmpFile);

            ostream = new FileOutputStream(tmpFile);

            xmlDataInjector.transform(xmlDoc, ostream);

            InputStream istream = new FileInputStream(tmpFile);
            // create a transform document containing the result
            resultDoc = new TransformDocumentImpl(new FileBlob(istream),
                    destinationMimeType);
        } catch (Exception e) {
            log.error(e);
            resultDoc = null;
        } finally {
            if (ostream != null) {
                try {
                    ostream.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }

        return resultDoc;
    }

}
