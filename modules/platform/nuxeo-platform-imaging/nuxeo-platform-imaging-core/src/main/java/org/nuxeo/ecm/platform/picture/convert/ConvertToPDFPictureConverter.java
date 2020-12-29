/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.picture.convert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.4
 */
public class ConvertToPDFPictureConverter implements Converter {

    private static final Log log = LogFactory.getLog(ConvertToPDFPictureConverter.class);

    @Override
    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {
        List<Blob> sources = blobHolder.getBlobs();
        List<Blob> results = new ArrayList<>(sources.size());
        sources.stream().filter(source -> source != null).forEach(source -> {
            Blob result = convert(source, parameters);
            if (result != null) {
                results.add(result);
            }
        });
        return new SimpleCachableBlobHolder(results);
    }

    @Override
    public Blob convert(Blob blob, Map<String, Serializable> parameters) throws ConversionException {
        ImagingService service = Framework.getService(ImagingService.class);
        return service.convertToPDF(blob);
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
    }

}
