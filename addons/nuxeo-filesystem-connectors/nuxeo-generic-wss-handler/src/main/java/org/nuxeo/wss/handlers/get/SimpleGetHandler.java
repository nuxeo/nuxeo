/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.handlers.get;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSConfig;
import org.nuxeo.wss.fprpc.FPRPCConts;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.servlet.WSSResponse;

public class SimpleGetHandler {

    private static final Log log = LogFactory.getLog(SimpleGetHandler.class);

    public void handleRequest(WSSRequest request, WSSResponse response) throws Exception {

        String uri = request.getHttpRequest().getRequestURI();
        String method = request.getHttpRequest().getMethod();

        String[] parts = uri.split("/");
        String lastSegment = parts[parts.length-1];
        String UA = request.getHttpRequest().getHeader("User-Agent");

        log.debug("handling get request on uri = " + uri);

        if (FPRPCConts.MSOFFICE_USERAGENT.equals(UA)) {

            response.getHttpResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
            /*
            WSSBackend backend = Backend.get(request);

            WSSListItem doc = backend.getItem(uri);
            if (doc==null) {
                response.getHttpResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } else {
                HttpServletResponse httpResponse = response.getHttpResponse();
                httpResponse.addHeader("ETag", "\"{"+ doc.getEtag() + "},4\"");
                httpResponse.addHeader("ResourceTag", "rt:" + doc.getEtag() + "@00000000004");
                httpResponse.addHeader("Public-Extension", "http://schemas.microsoft.com/repl-2");
                httpResponse.setContentLength(doc.getSize());

                if ("HEAD".equals(method)) {
                    //
                } else if ("GET".equals(method)) {
                    httpResponse.getOutputStream();

                    InputStream stream = doc.getStream();
                    byte[] buffer = new byte[10*1024];
                    int read;
                    while ((read = stream.read(buffer)) != -1) {
                        httpResponse.getOutputStream().write(buffer, 0, read);
                    }
                }
            }*/
        } else if ("_vti_inf.html".equals(lastSegment)) {

            String prefix = "";
            if (!WSSConfig.instance().isHostFPExtensionAtRoot()) {
                prefix = WSSConfig.instance().getContextPath();
                if (prefix==null) {
                    prefix ="";
                } else if (!prefix.equals("")) {
                    if (!prefix.endsWith("/")) {
                        prefix=prefix + "/";
                    }
                    if (prefix.startsWith("/")) {
                        prefix=prefix.substring(1);
                    }
                }
            }

            response.addRenderingParameter("prefix", prefix);
            response.getHttpResponse().setHeader(
                    "Public-Extension", "http://schemas.microsoft.com/repl-2");
            response.setRenderingTemplateName(lastSegment);
        }
    }

}
