/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
