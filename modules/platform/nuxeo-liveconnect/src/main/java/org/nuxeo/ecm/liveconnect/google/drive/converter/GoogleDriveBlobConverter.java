/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Nelson Silva
 */
package org.nuxeo.ecm.liveconnect.google.drive.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Converter that relies on {@link org.nuxeo.ecm.core.blob.BlobProvider} conversions.
 *
 * @since 7.3
 */
public class GoogleDriveBlobConverter implements Converter {

    protected ConverterDescriptor descriptor;

    @Override
    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        Blob srcBlob, dstBlob;

        srcBlob = blobHolder.getBlob();
        if (srcBlob == null) {
            return null;
        }

        try {
            DocumentModel doc = null;
            if (blobHolder instanceof DocumentBlobHolder) {
                doc = ((DocumentBlobHolder) blobHolder).getDocument();
            }
            dstBlob = convert(srcBlob, doc);
        } catch (IOException e) {
            throw new ConversionException("Unable to fetch conversion", e);
        }

        if (dstBlob == null) {
            return null;
        }

        return new SimpleCachableBlobHolder(dstBlob);
    }

    protected Blob convert(Blob blob, DocumentModel doc) throws IOException {
        String mimetype = descriptor.getDestinationMimeType();
        InputStream is = Framework.getService(DocumentBlobManager.class).getConvertedStream(blob, mimetype, doc);
        return is == null ? null : Blobs.createBlob(is, mimetype);
    }
}
