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

package org.nuxeo.wss.handlers.fprpc;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.fprpc.FPRPCCall;
import org.nuxeo.wss.fprpc.FPRPCRequest;
import org.nuxeo.wss.fprpc.FPRPCResponse;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSListItem;
import org.nuxeo.wss.url.WSSUrlMapper;

public class OWSSvrHandler extends AbstractFPRPCHandler implements FPRPCHandler {

    private static final Log log = LogFactory.getLog(OWSSvrHandler.class);

    @Override
    protected void processCall(FPRPCRequest request, FPRPCResponse fpResponse, int callIndex, WSSBackend backend)
            throws WSSException {

        FPRPCCall call = request.getCalls().get(callIndex);

        fpResponse.addRenderingParameter("siteRoot", request.getSitePath());
        fpResponse.addRenderingParameter("request", request);

        log.debug("Handling FP OWS call on method " + call.getMethodName());

        if ("FileOpen".equals(call.getMethodName())) {
            handleFileDialog(request, fpResponse, call, backend, false);
        } else if ("FileSave".equals(call.getMethodName())) {
            try {
                fpResponse.getHttpResponse().sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,
                        "Please use list-document API for save as");
                return;
            } catch (IOException e) {
                throw new WSSException("Error while sending error!", e);
            }
            // handleFileDialog(request, fpResponse, call, backend, true);
        } else if ("SaveForm".equals(call.getMethodName())) {

            if ("HEAD".equals(request.getHttpRequest().getMethod())) {
                fpResponse.setContentType("text/html");
                fpResponse.getHttpResponse().setStatus(HttpServletResponse.SC_GONE);
                return;
            } else {
                fpResponse.setContentType("text/html");
                fpResponse.getHttpResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
    }

    protected void handleFileDialog(FPRPCRequest request, FPRPCResponse response, FPRPCCall call, WSSBackend backend,
            boolean save) throws WSSException {
        if ("HEAD".equals(request.getHttpRequest().getMethod())) {
            response.setContentType("text/html");
            return;
        }

        String location = call.getParameters().get("location");
        location = WSSUrlMapper.getUrlWithSitePath(request, location);

        WSSListItem parent = backend.getItem(location);
        List<WSSListItem> items = backend.listItems(location);
        response.addRenderingParameter("parent", parent);
        response.addRenderingParameter("items", items);
        response.setContentType("text/html");

        Cookie MSOWebPartCookie = new Cookie("MSOWebPartPage_AnonymousAccessCookie", "80");
        MSOWebPartCookie.setPath("/");
        MSOWebPartCookie.setMaxAge(3600);

        response.getHttpResponse().setCharacterEncoding("UTF-8");

        response.getHttpResponse().addCookie(MSOWebPartCookie);
        if (save) {
            // response.setRenderingTemplateName("FileSave.ftl");
            response.setRenderingTemplateName("FileOpen.ftl");
        } else {
            response.setRenderingTemplateName("FileOpen.ftl");
        }
    }

}
