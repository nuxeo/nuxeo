/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */
package org.nuxeo.ecm.restapi.server.jaxrs.targetplatforms;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.restlet.Request;
import org.restlet.Response;

import java.io.IOException;

public class TargetPlatformsRestlet extends BaseNuxeoRestlet {
    @Override
    public void handle(Request request, Response response) {
        HttpServletResponse res = BaseNuxeoRestlet.getHttpResponse(response);
        HttpServletRequest req = BaseNuxeoRestlet.getHttpRequest(request);
        if (req == null || res == null || res.isCommitted()) {
            return;
        }

        try {
            res.sendRedirect(VirtualHostHelper.getBaseURL(req) + "api/v1/target-platforms/public");
        } catch (IOException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
