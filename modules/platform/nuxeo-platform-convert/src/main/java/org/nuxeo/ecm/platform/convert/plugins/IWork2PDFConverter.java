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
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.convert.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.ZipUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * iWork2PDF converter.
 *
 * @author ldoguin
 */
public class IWork2PDFConverter implements Converter {

    public static final List<String> IWORK_MIME_TYPES = Arrays.asList(new String[] { "application/vnd.apple.pages",
            "application/vnd.apple.keynote", "application/vnd.apple.numbers", "application/vnd.apple.iwork" });

    private static final String IWORK_PREVIEW_FILE = "QuickLook/Preview.pdf";

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        try {
            // retrieve the blob and verify its mimeType
            Blob blob = blobHolder.getBlob();
            String mimeType = blob.getMimeType();

            if (mimeType == null || !IWORK_MIME_TYPES.contains(mimeType)) {
                throw new ConversionException("not an iWork file", blobHolder);
            }

            // check if the stream represents a valid zip
            try (InputStream blobStream = blob.getStream()) {
                if (!ZipUtils.isValid(blobStream)) {
                    throw new ConversionException("not a valid iWork file", blobHolder);
                }
            }

            // look for the pdf file
            try (InputStream blobStream = blob.getStream()) {
                if (ZipUtils.hasEntry(blobStream, IWORK_PREVIEW_FILE)) {
                    // pdf file exist, let's extract it and return it as a
                    // BlobHolder.
                    Blob previewBlob;
                    try (InputStream previewPDFFile = ZipUtils.getEntryContentAsStream(blob.getStream(),
                            IWORK_PREVIEW_FILE)) {
                        previewBlob = Blobs.createBlob(previewPDFFile);
                    }
                    return new SimpleCachableBlobHolder(previewBlob);
                } else {
                    // Pdf file does not exist, conversion cannot be done.
                    throw new ConversionException("iWork file does not contain a pdf preview.", blobHolder);
                }
            }
        } catch (IOException e) {
            throw new ConversionException("Could not find the pdf preview in the iWork file", blobHolder, e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
