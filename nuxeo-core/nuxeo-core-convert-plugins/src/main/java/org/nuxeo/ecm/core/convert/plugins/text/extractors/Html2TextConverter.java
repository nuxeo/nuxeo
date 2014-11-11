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

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.services.streaming.StreamSource;
import org.nuxeo.runtime.services.streaming.StringSource;

/**
 * Extract the text content of HTML documents while trying to respect the
 * paragraph structure.
 * 
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class Html2TextConverter implements Converter {

    private static final Log log = LogFactory.getLog(Html2TextConverter.class);

    @Override
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        InputStream stream = null;
        try {
            Blob blob = blobHolder.getBlob();
            Source source = null;

            // if the underlying source is unambiguously decoded, access the
            // decoded string directly
            if (blob instanceof StreamingBlob) {
                StreamingBlob sblob = (StreamingBlob) blob;
                StreamSource streamSource = sblob.getStreamSource();
                if (streamSource instanceof StringSource) {
                    source = new Source(
                            ((StringSource) streamSource).getString());
                }
            }
            if (source == null) {
                // use the parser charset heuristic to decode properly
                source = new Source(blob.getStream());
            }
            Renderer renderer = source.getRenderer();
            renderer.setIncludeHyperlinkURLs(false);
            renderer.setDecorateFontStyles(false);
            String text = renderer.toString();
            text = text.replaceAll("\r\n", "\n"); // unix end of line
            text = text.replaceAll(" *\n", "\n"); // clean trailing spaces
            text = text.replaceAll("\\n\\n+", "\n\n"); // clean multiple lines
            text = text.trim();
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

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
