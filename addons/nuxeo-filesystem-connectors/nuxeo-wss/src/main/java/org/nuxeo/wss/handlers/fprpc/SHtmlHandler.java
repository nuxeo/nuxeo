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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.fprpc.FPRPCCall;
import org.nuxeo.wss.fprpc.FPRPCRequest;
import org.nuxeo.wss.fprpc.FPRPCResponse;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.url.WSSUrlMapper;
import org.nuxeo.wss.url.WSSUrlMapping;

public class SHtmlHandler extends AbstractFPRPCHandler implements FPRPCHandler {

    private static final Log log = LogFactory.getLog(SHtmlHandler.class);

    protected void processCall(FPRPCRequest request, FPRPCResponse fpResponse, int callIndex, WSSBackend backend)
            throws WSSException {

        FPRPCCall call = request.getCalls().get(callIndex);

        log.debug("Handling FP SHtml call on method " + call.getMethodName());

        if ("server version".equals(call.getMethodName())) {
            fpResponse.addRenderingParameter("request", request);
            fpResponse.setRenderingTemplateName("server-version.ftl");

        } else if ("url to web url".equals(call.getMethodName())) {
            // http://msdn.microsoft.com/fr-fr/library/ms460544.aspx

            // XXX handle only one site for now !!!
            String askedUrl = call.getParameters().get("url");

            WSSUrlMapping mapping = WSSUrlMapper.getWebMapping(request, askedUrl);

            String webUrl = mapping.getSiteUrl();
            String fileUrl = mapping.getResourceUrl();

            fpResponse.addRenderingParameter("request", request);
            fpResponse.addRenderingParameter("webUrl", webUrl);
            fpResponse.addRenderingParameter("fileUrl", fileUrl);
            fpResponse.setRenderingTemplateName("url-to-web-url.ftl");
        }
    }

}
