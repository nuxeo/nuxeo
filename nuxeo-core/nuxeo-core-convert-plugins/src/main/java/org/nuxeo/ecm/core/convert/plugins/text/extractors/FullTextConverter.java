/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.convert.plugins.text.extractors;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Converter that tries to find a way to extract full text content according to input mime-type.
 *
 * @author tiry
 */
public class FullTextConverter implements Converter {

    private static final String TEXT_PLAIN_MT = "text/plain";

    private static final Log log = LogFactory.getLog(FullTextConverter.class);

    protected ConverterDescriptor descriptor;

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        String srcMT = blobHolder.getBlob().getMimeType();

        if (TEXT_PLAIN_MT.equals(srcMT)) {
            // no need to convert !
            return blobHolder;
        }

        ConversionService cs = Framework.getService(ConversionService.class);

        String converterName = cs.getConverterName(srcMT, TEXT_PLAIN_MT);

        if (converterName != null) {
            if (converterName.equals(descriptor.getConverterName())) {
                // Should never happen !
                log.debug("Existing from converter to avoid a loop");
                return new SimpleBlobHolder(Blobs.createBlob(""));
            }
            return cs.convert(converterName, blobHolder, parameters);
        } else {
            log.debug("Unable to find full text extractor for source mime type" + srcMT);
            return new SimpleBlobHolder(Blobs.createBlob(""));
        }
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        this.descriptor = descriptor;
    }

}
