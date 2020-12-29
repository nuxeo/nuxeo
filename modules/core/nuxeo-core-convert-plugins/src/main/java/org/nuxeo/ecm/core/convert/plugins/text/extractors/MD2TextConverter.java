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
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * Markdown to text converter.
 * <p>
 * It basically returns a {@link StringBlob} with the markdown text and the plain/text mime type.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public class MD2TextConverter implements Converter {

    private static final Log LOGGER = LogFactory.getLog(MD2TextConverter.class);

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Blob blob = convert(blobHolder.getBlob(), parameters);
        return blob == null ? blobHolder : new SimpleCachableBlobHolder(blob);
    }

    @Override
    public Blob convert(Blob blob, Map<String, Serializable> parameters) throws ConversionException {
        try {
            if (blob == null) {
                LOGGER.warn("Trying to convert a null blob");
                return blob;
            }
            String text = blob.getString();
            return Blobs.createBlob(text);
        } catch (IOException e) {
            throw new ConversionException("Error during MD2Text conversion", blob, e);
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
