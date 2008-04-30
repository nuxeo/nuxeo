/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.versioning.api.VersioningActions;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DocumentFormHelper {

    public static final String DOCTYPE = "doctype";
    public static final String VERSIONING = "versioning";
    public static final String MAJOR = "major";
    public static final String MINOR = "minor";


    /**
     * Fills the given document model properties using data from request parameters.
     *
     * @param doc
     * @param request
     */
    @SuppressWarnings("unchecked")
    public static void fillDocumentProperties(DocumentModel doc, HttpServletRequest request) throws PropertyException {
        Map<String,String[]> map = (Map<String,String[]>)request.getParameterMap();
        for (Map.Entry<String,String[]> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.indexOf(':') > -1) { // an XPATH property
                Property p = null;
                try {
                    p = doc.getProperty(key);
                } catch (PropertyException e) {
                    continue; // not a valid property
                }
                String[] ar = entry.getValue();
                if (ar == null || ar.length == 0) {
                    p.remove();
                } else if (p.isScalar()) {
                    p.setValue(ar[0]);
                } else if (p.isList()) {
                    p.setValue(entry.getValue());
                }
            }
        }
    }

    public static VersioningActions getVersioningOption(HttpServletRequest request) {
        String val = request.getParameter(VERSIONING);
        if (val != null) {
            return val.equals(MAJOR) ? VersioningActions.ACTION_INCREMENT_MAJOR
                    : (val.equals(MINOR) ? VersioningActions.ACTION_INCREMENT_MINOR : null);
        }
        return null;
    }

    public static String getDocumentType(HttpServletRequest request) {
        return request.getParameter(DOCTYPE);
    }

}
