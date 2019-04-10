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
package org.nuxeo.ecm.diff.content.converters;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract class for content diff converter implementations.
 *
 * @author Antoine Taillefer
 */
public abstract class AbstractContentDiffConverter implements Converter {

    public void init(ConverterDescriptor descriptor) {
        // Nothing to do here
    }

    protected final BlobHolder convert(String converterName, BlobHolder blobHolder, Map<String, Serializable> parameters)
            throws ConversionException {

        ConversionService cs = Framework.getService(ConversionService.class);

        return cs.convert(converterName, blobHolder, parameters);
    }

}
