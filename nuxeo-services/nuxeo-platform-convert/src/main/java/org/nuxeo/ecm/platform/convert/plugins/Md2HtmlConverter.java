/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
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
    public BlobHolder convert(BlobHolder blobHolder,
            Map<String, Serializable> parameters) throws ConversionException {

        Boolean bodyContentOnly = Boolean.FALSE;
        if (parameters != null) {
            bodyContentOnly = (Boolean) parameters.get(BODY_CONTENT_ONLY);
        }
        try {
            Blob inputBlob = blobHolder.getBlob();
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
            Blob outputBlob = new StringBlob(html.toString(),
                    descriptor.getDestinationMimeType());
            String filename = inputBlob.getFilename();
            if (filename != null) {
                int dotPosition = filename.lastIndexOf('.');
                if (dotPosition > -1) {
                    filename = filename.substring(0, dotPosition) + ".html";
                }
                outputBlob.setFilename(filename);
            }
            return new SimpleCachableBlobHolder(outputBlob);
        } catch (Exception e) {
            throw new ConversionException(
                    "Could not get Markdown string from BlobHolder", e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

}
