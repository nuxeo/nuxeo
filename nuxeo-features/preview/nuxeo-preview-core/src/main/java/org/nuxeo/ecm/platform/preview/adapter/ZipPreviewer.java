/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.platform.preview.adapter;

import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.htmlsanitizer.HtmlSanitizerService;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Previewer for Zip blobs.
 * <p>
 * It sanitizes each zip entry if needed.
 *
 * @since 11.5
 */
public class ZipPreviewer implements MimeTypePreviewer {

    private static final Logger log = LogManager.getLogger(ZipPreviewer.class);

    protected static final Set<String> HTML_MIME_TYPES = new HashSet<>(Arrays.asList(TEXT_HTML, TEXT_XML, TEXT_PLAIN));

    @Override
    public List<Blob> getPreview(Blob blob, DocumentModel dm) throws PreviewException {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        String converterName = conversionService.getConverterName("application/zip", "text/html");
        if (converterName == null) {
            throw new PreviewException("Unable to find converter from application/zip to text/html");
        }

        try {
            BlobHolder result = conversionService.convert(converterName, new SimpleBlobHolder(blob), null);
            return result.getBlobs().stream().map(this::sanitize).collect(Collectors.toList());
        } catch (ConversionException e) {
            throw new PreviewException(e.getMessage(), e);
        }
    }

    protected Blob sanitize(Blob blob) {
        String filename = blob.getFilename();
        if (!isSanitizable(blob)) {
            log.debug("ZipEntryBlob: {} is not sanitizable", filename);
            return blob;
        }
        HtmlSanitizerService sanitizerService = Framework.getService(HtmlSanitizerService.class);
        try {
            String content = blob.getString();
            content = sanitizerService.sanitizeString(content, null);
            content = PreviewHelper.makeHtmlPage(content);
            return Blobs.createBlob(content, "text/html", null, filename);
        } catch (IOException e) {
            throw new PreviewException("Cannot read ZipEntryBlob content with filename: " + filename, e);
        }
    }

    protected boolean isSanitizable(Blob blob) {
        MimetypeRegistry registry = Framework.getService(MimetypeRegistry.class);
        String mimeType = registry.getMimetypeFromFilenameWithBlobMimetypeFallback(blob.getFilename(), blob, null);
        if (mimeType == null) {
            try (InputStream stream = blob.getStream()) {
                // last chance introspect the content
                LineIterator lineIt = IOUtils.lineIterator(stream, defaultIfNull(blob.getEncoding(), "UTF-8"));
                while (lineIt.hasNext()) {
                    String line = lineIt.nextLine();
                    if (line.contains("<script")) {
                        return true;
                    }
                }
                return false;
            } catch (IOException e) {
                throw new PreviewException("Unable to introspect content");
            }
        } else {
            return HTML_MIME_TYPES.contains(mimeType);
        }
    }
}
