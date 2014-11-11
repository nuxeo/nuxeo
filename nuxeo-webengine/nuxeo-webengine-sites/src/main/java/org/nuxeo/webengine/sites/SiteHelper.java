/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.webengine.sites;

import java.io.Serializable;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;

public class SiteHelper {

    private SiteHelper() {
    }

    public static String getString(DocumentModel d, String xpath,
            String defaultValue) {
        try {
            return getString(d, xpath);
        } catch (ClientException e) {
            return defaultValue;
        }
    }

    public static String getString(DocumentModel d, String xpath)
            throws ClientException {
        Property p = d.getProperty(xpath);
        if (p != null) {
            Serializable v = p.getValue();
            if (v != null) {
                return v.toString();
            }
        }
        return "";
    }

    public static GregorianCalendar getGregorianCalendar(DocumentModel d,
            String xpath) throws ClientException {
        Property p = d.getProperty(xpath);
        if (p != null) {
            Serializable v = p.getValue();
            if (v != null) {
                return (GregorianCalendar) v;
            }
        }
        return null;
    }

    public static Blob getBlob(DocumentModel d, String xpath)
            throws ClientException {
        Property p = d.getProperty(xpath);
        if (p != null) {
            Serializable v = p.getValue();
            if (v != null) {
                return (Blob) v;
            }
        }
        return null;
    }

    public static boolean getBoolean(DocumentModel d, String xpath,
            boolean defaultValue) {
        try {
            return getBoolean(d, xpath);
        } catch (ClientException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(DocumentModel d, String xpath)
            throws ClientException {
        Property p = d.getProperty(xpath);
        if (p != null) {
            Serializable v = p.getValue();
            if (v != null) {
                return (Boolean) v;
            }
        }
        throw new ClientException("value is null");
    }

    public static String getFistNWordsFromString(String string, int n) {
        String[] result = string.split(" ", n + 1);
        StringBuffer firstNwords = new StringBuffer();
        for (int i = 0; i < ((n <= result.length) ? n : result.length); i++) {
            firstNwords.append(result[i]);
            firstNwords.append(" ");

        }
        return new String(firstNwords);
    }

}
