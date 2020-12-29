/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */
package org.nuxeo.ecm.core.convert.tests;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * @since 7.3
 */
public class DummyPDFConverter implements Converter {

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Blob blob = convert((Blob) null, parameters);
        return new SimpleBlobHolder(blob);
    }

    @Override
    public Blob convert(Blob blob, Map<String, Serializable> parameters) throws ConversionException {
        Boolean setMimeType = (Boolean) parameters.get("setMimeType");
        if (setMimeType == null) {
            setMimeType = false;
        }
        Boolean tempFilename = (Boolean) parameters.get("tempFilename");
        if (tempFilename == null) {
            tempFilename = false;
        }
        blob = Blobs.createBlob("", setMimeType ? "application/octet-stream" : null);
        if (tempFilename) {
            blob.setFilename("nxblob-434523.tmp");
        }
        return blob;
    }
    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
