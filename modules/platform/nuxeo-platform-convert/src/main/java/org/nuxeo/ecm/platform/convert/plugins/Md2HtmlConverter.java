/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

import com.cforcoding.jmd.MarkDownParserAndSanitizer;

/**
 * Markdown to HTML converter
 *
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.5
 */
public class Md2HtmlConverter implements Converter {

    private static final String BODY_CONTENT_ONLY = "bodyContentOnly";

    private ConverterDescriptor descriptor;

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        return new SimpleCachableBlobHolder(convert(blobHolder.getBlob(), parameters));
    }

    @Override
    public Blob convert(Blob inputBlob, Map<String, Serializable> parameters) throws ConversionException {

        Boolean bodyContentOnly = Boolean.FALSE;
        if (parameters != null) {
            bodyContentOnly = (Boolean) parameters.get(BODY_CONTENT_ONLY);
        }
        if (bodyContentOnly == null) {
            bodyContentOnly = Boolean.FALSE;
        }
        try {
            String mdString = inputBlob.getString();
            MarkDownParserAndSanitizer md = new MarkDownParserAndSanitizer();
            StringBuilder html = new StringBuilder();
            if (!bodyContentOnly) {
                html.append("<html><head></head><body>");
            }
            html.append(md.transform(mdString));
            if (!bodyContentOnly) {
                html.append("</body></html>");
            }
            Blob outputBlob = Blobs.createBlob(html.toString(), descriptor.getDestinationMimeType());
            String filename = inputBlob.getFilename();
            if (filename != null) {
                int dotPosition = filename.lastIndexOf('.');
                if (dotPosition > -1) {
                    filename = filename.substring(0, dotPosition) + ".html";
                }
                outputBlob.setFilename(filename);
            }
            return outputBlob;
        } catch (IOException e) {
            throw new ConversionException("Could not get Markdown string from BlobHolder", inputBlob, e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

}
