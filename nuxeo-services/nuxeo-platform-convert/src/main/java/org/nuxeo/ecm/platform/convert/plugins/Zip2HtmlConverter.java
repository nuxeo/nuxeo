/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * Zip2Html converter.
 *
 * @author ldoguin
 */
public class Zip2HtmlConverter implements Converter {

    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        Blob blob = blobHolder.getBlob();
        String mimeType = blob.getMimeType();
        if (!mimeType.equals("application/zip") && !mimeType.equals("application/x-zip-compressed")) {
            throw new ConversionException("not a zip file");
        }
        return new ZipCachableBlobHolder(blob);
    }

    public void init(ConverterDescriptor descriptor) {
    }

}
