/*
 * (C) Copyright 2002-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.convert.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.convert.extension.ConverterDescriptor;

/**
 * Helper class to manage chains of converters.
 *
 * @author tiry
 */
public class MimeTypeTranslationHelper {

    protected static final Map<String, List<ConvertOption>> srcMappings
            = new HashMap<String, List<ConvertOption>>();
    protected static final Map<String, List<ConvertOption>> dstMappings
            = new HashMap<String, List<ConvertOption>>();

    // Utility class.
    private MimeTypeTranslationHelper() {
    }

    public static void addConverter(ConverterDescriptor desc) {
        List<String> sMts = desc.getSourceMimeTypes();
        String dMt = desc.getDestinationMimeType();

        List<ConvertOption> dco = dstMappings.get(dMt);
        if (dco == null) {
            dco = new ArrayList<ConvertOption>();
        }

        for (String sMT : sMts) {
            List<ConvertOption> sco = srcMappings.get(sMT);

            if (sco == null) {
                sco = new ArrayList<ConvertOption>();
            }

            sco.add(new ConvertOption(desc.getConverterName(), dMt));
            srcMappings.put(sMT, sco);

            dco.add(new ConvertOption(desc.getConverterName(), sMT));
        }

        dstMappings.put(dMt, dco);
    }

    public static String getConverterName(String sourceMimeType,
            String destMimeType) {

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

    public static List<String> getDestinationMimeTypes(String sourceMimeType) {
        List<String> dst = new ArrayList<String>();

        List<ConvertOption> sco = srcMappings.get(sourceMimeType);

        if (sco != null) {
            for (ConvertOption co : sco) {
                dst.add(co.getMimeType());
            }
        }
        return dst;
    }

    public static List<String> getSourceMimeTypes(String destinationMimeType) {
        List<String> src = new ArrayList<String>();

        List<ConvertOption> dco = dstMappings.get(destinationMimeType);

        if (dco != null) {
            for (ConvertOption co : dco) {
                src.add(co.getMimeType());
            }
        }
        return src;
    }

}
