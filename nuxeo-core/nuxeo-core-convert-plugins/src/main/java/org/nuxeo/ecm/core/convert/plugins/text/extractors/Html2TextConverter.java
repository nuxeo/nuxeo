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
 */
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

/**
 * Extract the text content of HTML documents while trying to respect the paragraph structure.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 */
public class Html2TextConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        try {
            Blob blob = blobHolder.getBlob();
            // if the underlying source is unambiguously decoded, access the
            // decoded string directly
            Source source;
            if (blob instanceof StringBlob) {
                source = new Source(blob.getString());
            } else if (blob.getEncoding() != null) {
                Reader reader = new InputStreamReader(blob.getStream(), blob.getEncoding());
                source = new Source(reader);
            } else {
                // otherwise use the parser charset heuristic to decode properly
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
            return new SimpleCachableBlobHolder(Blobs.createBlob(text));
        } catch (IOException e) {
            throw new ConversionException("Error during Html2Text conversion", blobHolder, e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
