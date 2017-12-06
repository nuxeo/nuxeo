/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content.converters;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;
import org.nuxeo.runtime.api.Framework;

/**
 * HTML converter for content diff.
 * <p>
 * Uses the converter registered with sourceMimeType = mime type of the {@code blobHolder} and destinationMimeType =
 * {@code text/html}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public class ContentDiffHtmlConverter extends AbstractContentDiffConverter {

    private static final Log LOGGER = LogFactory.getLog(ContentDiffHtmlConverter.class);

    private static final String HTML_MIME_TYPE = "text/html";

    private static final String ANY_2_HTML_CONVERTER_NAME = "any2html";

    private static final String OFFICE_2_HTML_CONVERTER_NAME = "office2html";

    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        String converterName = null;

        // Fetch blob from blob holder
        Blob blob = blobHolder.getBlob();
        if (blob == null) {
            LOGGER.warn("Trying to convert a blob holder that has a null blob. Nothing to do, returning the blob holder.");
            return blobHolder;
        }

        // Get HTML converter name from blob mime type
        String mimeType = blob.getMimeType();
        ConversionService cs = Framework.getService(ConversionService.class);
        converterName = cs.getConverterName(mimeType, HTML_MIME_TYPE);
        // We don't want to use the "any2html" converter contributed for the
        // preview in the case of non pdf blobs since it uses the following
        // conversion chain : any2pdf --> pdf2html.
        // In this case we want to use the "office2html" converter which
        // gives a better result when applying the HTMLContentDiffer on the
        // converted HTML.
        if (ANY_2_HTML_CONVERTER_NAME.equals(converterName) && !"application/pdf".equals(mimeType)) {
            converterName = OFFICE_2_HTML_CONVERTER_NAME;
        }

        // No converter found, throw appropriate exception
        if (converterName == null) {
            throw new ConverterNotRegistered(String.format("for sourceMimeType = %s, destinationMimeType = %s",
                    mimeType, HTML_MIME_TYPE));
        }

        return convert(converterName, blobHolder, parameters);
    }

}
