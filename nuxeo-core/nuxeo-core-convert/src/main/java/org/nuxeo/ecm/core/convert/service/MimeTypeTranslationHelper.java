/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.convert.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * Helper class to manage chains of converters.
 *
 * @author tiry
 */
public class MimeTypeTranslationHelper {

    protected final Log log = LogFactory.getLog(MimeTypeTranslationHelper.class);

    protected final Map<String, List<ConvertOption>> srcMappings = new HashMap<>();

    protected final Map<String, List<ConvertOption>> dstMappings = new HashMap<>();

    public void addConverter(ConverterDescriptor desc) {
        List<String> sMts = desc.getSourceMimeTypes();
        String dMt = desc.getDestinationMimeType();

        List<ConvertOption> dco = dstMappings.get(dMt);
        if (dco == null) {
            dco = new ArrayList<>();
        }

        for (String sMT : sMts) {
            List<ConvertOption> sco = srcMappings.get(sMT);

            if (sco == null) {
                sco = new ArrayList<>();
            }

            sco.add(new ConvertOption(desc.getConverterName(), dMt));
            srcMappings.put(sMT, sco);

            dco.add(new ConvertOption(desc.getConverterName(), sMT));
        }

        dstMappings.put(dMt, dco);
        log.debug("Added converter " + desc.getSourceMimeTypes() + " to " + desc.getDestinationMimeType());
    }

    public String getConverterName(String sourceMimeType, String destMimeType) {

        List<ConvertOption> sco = srcMappings.get(sourceMimeType);
        if (sco == null) {
            // use wildcard
            sco = srcMappings.get("*");
            if (sco == null) {
                return null;
            }
        }
        for (ConvertOption co : sco) {
            if (co.mimeType.equals(destMimeType)) {
                return co.getConverterName();
            }
        }
        return null;
    }

    public List<String> getConverterNames(String sourceMimeType, String destMimeType) {
        List<ConvertOption> sco = srcMappings.get(sourceMimeType);
        List<String> converterNames = new ArrayList<>();
        if (sco == null) {
            // use wildcard
            sco = srcMappings.get("*");
            if (sco == null) {
                return converterNames;
            }
        }
        for (ConvertOption co : sco) {
            if (co.mimeType.equals(destMimeType)) {
                converterNames.add(co.getConverterName());
            }
        }
        return converterNames;
    }

    public List<String> getDestinationMimeTypes(String sourceMimeType) {
        List<String> dst = new ArrayList<>();

        List<ConvertOption> sco = srcMappings.get(sourceMimeType);

        if (sco != null) {
            for (ConvertOption co : sco) {
                dst.add(co.getMimeType());
            }
        }
        return dst;
    }

    public List<String> getSourceMimeTypes(String destinationMimeType) {
        List<String> src = new ArrayList<>();

        List<ConvertOption> dco = dstMappings.get(destinationMimeType);

        if (dco != null) {
            for (ConvertOption co : dco) {
                src.add(co.getMimeType());
            }
        }
        return src;
    }

    public void clear() {
        dstMappings.clear();
        srcMappings.clear();
        log.debug("clear");
    }

}
