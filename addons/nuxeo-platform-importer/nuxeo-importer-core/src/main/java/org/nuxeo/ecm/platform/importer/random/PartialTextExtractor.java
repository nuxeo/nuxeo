/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.platform.importer.random;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

public class PartialTextExtractor implements Converter {

    public static final double TEXT_RATIO = 0.01;

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        try {
            Blob blob = blobHolder.getBlob();

            String data = blob.getString();
            int endIdx = (int) (data.length() * TEXT_RATIO);
            String txtData = data.substring(0, endIdx);
            return new SimpleBlobHolder(Blobs.createBlob(txtData));

        } catch (IOException e) {
            throw new ConversionException("error extracting partial text content", e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
