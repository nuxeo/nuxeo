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
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webengine.fm.extensions;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import freemarker.template.TemplateModel;

/**
 * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
 *
 */
public class Utils {

    // Utility class.
    private Utils() {
    }

    public static Map<String, String> getTemplateDirectiveParameters(
            Map<String, TemplateModel> params) {
        Map<String, String> attributes = new HashMap<String, String>();
        for (Map.Entry<String, TemplateModel> entry : params.entrySet()) {
            TemplateModel v = entry.getValue();
            attributes.put(entry.getKey(), v.toString());
        }
        return attributes;
    }

    public static boolean isWebEngineVirtualHosting(final HttpServletRequest request) {
        return request.getHeader("nuxeo-webengine-base-path") != null;
    }

}
