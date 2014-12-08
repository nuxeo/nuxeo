/*
 * (C) Copyright 2002-20012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content.converters;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
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
        Blob blob = null;
        try {
            blob = blobHolder.getBlob();
        } catch (ClientException ce) {
            throw new ConversionException("Cannot fetch blob from blob holder.", ce);
        }
        if (blob == null) {
            LOGGER.warn("Trying to convert a blob holder that has a null blob. Nothing to do, returning the blob holder.");
            return blobHolder;
        }

        // Get HTML converter name from blob mime type
        String mimeType = blob.getMimeType();
        ConversionService cs = Framework.getLocalService(ConversionService.class);
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
