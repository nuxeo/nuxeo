/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.connect.client.we;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.ecm.webengine.model.WebContext;

public class RequestHelper {

    private RequestHelper() {
    }

    public static boolean isInternalLink(WebContext ctx) {

        HttpServletRequest request = ctx.getRequest();

        String referer = request.getHeader("referer");

        if (referer == null) {
            return false;
        }

        String currentUrl = VirtualHostHelper.getServerURL(request) + request.getRequestURI().substring(1);
        String[] currentUrlParts = currentUrl.split("connectClient");
        String[] refererParts = referer.split("connectClient");

        return currentUrlParts[0].equals(refererParts[0]);
    }

}
