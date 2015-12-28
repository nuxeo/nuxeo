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

package org.nuxeo.template.processors.convert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.api.ConverterCheckResult;
import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;
import org.nuxeo.ecm.core.convert.extension.ExternalConverter;
import org.nuxeo.runtime.api.Framework;

/**
 * Converter used to bridge MarkDown and OpenOffice conversions
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
public class AnyToODTConverter implements ExternalConverter {

    protected ConversionService getConversionService() {
        return Framework.getLocalService(ConversionService.class);
    }

    protected List<String> getConverterChain(String srcMT) {
        List<String> subConverters = new ArrayList<String>();

        if (srcMT == null) {
            return null;
        }

        if (srcMT.equals("text/x-web-markdown")) {
            subConverters.add("md2html");
        }

        subConverters.add("sdttext2odt");

        return subConverters;
    }

    public BlobHolder convert(BlobHolder blobHolder, Map<String, Serializable> parameters) throws ConversionException {

        Blob sourceBlob = blobHolder.getBlob();

        List<String> subConverters = getConverterChain(sourceBlob.getMimeType());

        if (subConverters == null) {
            throw new ConversionException("Can not find suitable underlying converters to handle html preview");
        }

        BlobHolder result = blobHolder;

        for (String converterName : subConverters) {
            result = getConversionService().convert(converterName, result, parameters);
        }
        return result;
    }

    public void init(ConverterDescriptor descriptor) {
    }

    public ConverterCheckResult isConverterAvailable() {
        try {
            return getConversionService().isConverterAvailable("sdttext2odt");
        } catch (ConversionException e) {
            ConverterCheckResult result = new ConverterCheckResult();
            result.setAvailable(false);
            return result;
        }
    }

}
