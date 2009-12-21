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

package org.nuxeo.ecm.platform.transform.plugin.xslt.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.transform.api.TransformException;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.nuxeo.ecm.platform.transform.plugin.xslt.api.XSLTPlugin;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class XSLTPluginImpl extends AbstractPlugin implements XSLTPlugin {

    private static final long serialVersionUID = -2295663211441703689L;

    private static final Log log = LogFactory.getLog(XSLTPluginImpl.class);

    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>();

    static {
        MIME_TYPES.put("html", "text/html");
        MIME_TYPES.put("xml", "text/xml");
        MIME_TYPES.put("text", "text/plain");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {
        final List<TransformDocument> results = super.transform(options,
                sources);

        Blob stylesheet = null;
        if (options != null) {
            stylesheet = (Blob) options.get(OPTION_STYLESHEET);
        }
        if (options == null || stylesheet == null) {
            throw new TransformException(
                    "You must specify a XSL stylesheet as option.");
        }

        final Map<String, Object> xslParameters = (Map<String, Object>) options.get(OPTION_XSL_PARAMETERS);

        try {
            final Source xsltSource = new StreamSource(stylesheet.getStream());
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer(xsltSource);
            for (final TransformDocument source : sources) {
                final Source xmlSource = new StreamSource(
                        source.getBlob().getStream());
                final ByteArrayOutputStream out = new ByteArrayOutputStream();

                // set parameters
                if (xslParameters != null) {
                    for (final Map.Entry<String, Object> entry : xslParameters.entrySet()) {
                        transformer.setParameter(entry.getKey(), entry.getValue());
                    }
                }

                transformer.transform(xmlSource, new StreamResult(out));
                final InputStream in = new ByteArrayInputStream(
                        out.toByteArray());

                final Blob result = new FileBlob(in,
                        MIME_TYPES.get(transformer.getOutputProperty("method")));
                results.add(new TransformDocumentImpl(result,
                        result.getMimeType()));
            }
        } catch (Exception e) {
            log.error("error during XSL transformation...", e);
            throw new TransformException("Error during XSL transformation.", e);
        }

        return results;
    }

}
