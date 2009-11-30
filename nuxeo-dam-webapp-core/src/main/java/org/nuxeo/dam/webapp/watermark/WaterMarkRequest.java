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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.watermark;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class WaterMarkRequest extends HttpServletRequestWrapper {
    private String requestUri;

    public WaterMarkRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getRequestURI() {
        if (requestUri == null) {
            requestUri = super.getRequestURI().replace("watermark", "nxbigfile");
        }

        return requestUri;
    }
}
