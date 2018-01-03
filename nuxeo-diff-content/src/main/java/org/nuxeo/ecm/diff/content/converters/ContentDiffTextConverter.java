/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConverterNotRegistered;

/**
 * Text converter for content diff.
 * <p>
 * Uses the "any2text" converter.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
public class ContentDiffTextConverter extends AbstractContentDiffConverter {

    private static final String ANY_2_TEXT_CONVERTER_NAME = "any2text";

    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        BlobHolder convertedBlobHolder = convert(ANY_2_TEXT_CONVERTER_NAME, blobHolder, parameters);

        String convertedBlobString = null;
        try {
            Blob convertedBlob = convertedBlobHolder.getBlob();
            if (convertedBlob != null) {
                convertedBlobString = convertedBlob.getString();
            }
        } catch (IOException e) {
            throw new ConversionException("Error while getting converted blob string.");
        }
        if (StringUtils.isEmpty(convertedBlobString)) {
            // Converted blob is an empty string, this means no text
            // converter was found (see FullTextConverter). Throw
            // appropriate exception.
            String srcMimeType = null;
            Blob blob = blobHolder.getBlob();
            if (blob != null) {
                srcMimeType = blob.getMimeType();
            }
            throw new ConverterNotRegistered(
                    String.format("for sourceMimeType = %s, destinationMimeType = text/plain", srcMimeType));
        }
        return convertedBlobHolder;
    }

}
