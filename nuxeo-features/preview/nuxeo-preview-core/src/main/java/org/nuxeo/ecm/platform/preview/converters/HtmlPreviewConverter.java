/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.preview.converters;

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

public class HtmlPreviewConverter implements ExternalConverter {

    protected static ConversionService cs;

    protected static Boolean canUsePDF2Html;

    protected static Boolean canUseOOo2Html;

    protected static ConversionService getConversionService() {
        if (cs == null) {
            cs = Framework.getService(ConversionService.class);
        }
        return cs;
    }

    protected static boolean getCanUsePDF2Html() {
        if (canUsePDF2Html == null) {
            try {
                canUsePDF2Html = getConversionService().isConverterAvailable("pdf2html").isAvailable();
            } catch (ConversionException e) {
                return false;
            }
        }
        return canUsePDF2Html;
    }

    protected static boolean getCanUseOOo2Html() {
        if (canUseOOo2Html == null) {
            try {
                canUseOOo2Html = getConversionService().isConverterAvailable("office2html").isAvailable();
            } catch (ConversionException e) {
                return false;
            }
        }
        return canUseOOo2Html;
    }

    protected List<String> getConverterChain(String srcMT) {
        List<String> subConverters = new ArrayList<String>();

        if (srcMT == null) {
            return null;
        }

        if (srcMT.equals("text/html") || srcMT.equals("text/xml") || srcMT.equals("text/xhtml")) {
            return subConverters;
        }

        if (getCanUsePDF2Html()) {
            if (srcMT.equals("application/pdf")) {
                subConverters.add("pdf2html");
            } else {
                subConverters.add("any2pdf");
                subConverters.add("pdf2html");
            }
        } else {
            if (getCanUseOOo2Html()) {
                subConverters.add("office2html");
            } else {
                return null;
            }
        }

        return subConverters;
    }

    @Override
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
        Blob blob = result.getBlob();
        if (blob != null && blob.getEncoding() == null) {
            blob.setEncoding("UTF-8");
        }
        return result;
    }

    @Override
    public void init(ConverterDescriptor descriptor) {
        // TODO Auto-generated method stub
    }

    @Override
    public ConverterCheckResult isConverterAvailable() {
        ConverterCheckResult result = new ConverterCheckResult();
        result.setAvailable(getCanUseOOo2Html() || getCanUsePDF2Html());
        return result;
    }

}
