/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

        Blob sourceBlob;

        try {
            sourceBlob = blobHolder.getBlob();
        } catch (Exception e) {
            throw new ConversionException("Can not fetch blob", e);
        }

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
