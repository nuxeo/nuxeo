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
package org.nuxeo.wss.fprpc;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.wss.servlet.WSSResponse;

/**
 * Simple wrapper for FP-RPC responses.
 *
 * @author Thierry Delprat
 */
public class FPRPCResponse extends WSSResponse {

    public FPRPCResponse(HttpServletResponse httpResponse) {
        super(httpResponse);
        if (!httpResponse.isCommitted()) {
            httpResponse.setBufferSize(100000);
        }
    }

    @Override
    protected String getDefaultContentType() {
        return FPRPCConts.VERMEER_CT;
    }

    public void sendFPError(FPRPCRequest request, String errorCode, String errorMessage) {
        String method = request.getCalls().get(0).getMethodName();
        this.addRenderingParameter("request", request);
        this.addRenderingParameter("method", method);
        this.addRenderingParameter("errorCode", errorCode);
        this.addRenderingParameter("errorMessage", errorMessage);
        this.setRenderingTemplateName("fp-error.ftl");
    }

}
