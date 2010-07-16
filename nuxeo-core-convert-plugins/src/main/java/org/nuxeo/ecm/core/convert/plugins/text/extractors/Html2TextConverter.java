/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class Html2TextConverter implements Converter {

    private static final Log log = LogFactory.getLog(Html2TextConverter.class);

    public static final String TAG_FILTER_PARAMETER = "tagFilter";

    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        InputStream stream = null;
        try {
            stream = blobHolder.getBlob().getStream();
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            String tagFilter = null;
            if (parameters != null) {
                tagFilter = (String) parameters.get(TAG_FILTER_PARAMETER);
            }
            HtmlParser parser = new HtmlParser(tagFilter);

            SAXResult result = new SAXResult(new DefaultHandler());

            SAXSource source = new SAXSource(parser, new InputSource(stream));
            transformer.transform(source, result);

            //HtmlHandler html2text = new HtmlHandler();
            //String text = html2text.parse(stream);
            String text = parser.getContents();

            return new SimpleCachableBlobHolder(new StringBlob(text,
                    "text/plain"));
        } catch (Exception e) {
            throw new ConversionException("Error during Html2Text conversion", e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.error("Error while closing Blob stream", e);
                }
            }
        }
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
