/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.bindings;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.server.impl.atompub.AbstractAtomPubServiceCall;
import org.apache.chemistry.opencmis.server.impl.atompub.CmisAtomPubServlet;
import org.apache.chemistry.opencmis.server.shared.Dispatcher;
import org.apache.chemistry.opencmis.server.shared.ExceptionHelper;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisErrorHelper.ErrorInfo;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subclass CmisAtomPubServlet to inject a virtual-hosted base URL if needed.
 */
public class NuxeoCmisAtomPubServlet extends CmisAtomPubServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(NuxeoCmisAtomPubServlet.class);

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        String baseUrl = VirtualHostHelper.getBaseURL(request);
        if (baseUrl != null) {
            baseUrl = StringUtils.stripEnd(baseUrl, "/") + request.getServletPath() + "/"
                    + AbstractAtomPubServiceCall.REPOSITORY_PLACEHOLDER + "/";
            request.setAttribute(Dispatcher.BASE_URL_ATTRIBUTE, baseUrl);
        }
        super.service(request, response);
    }

    /**
     * Extracts the error from the exception.
     *
     * @param ex the exception
     * @return the error info
     * @since 7.1
     */
    protected ErrorInfo extractError(Exception ex) {
        return NuxeoCmisErrorHelper.extractError(ex);
    }

    @Override
    protected void printError(Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ErrorInfo errorInfo = extractError(ex);
        if (response.isCommitted()) {
            LOG.warn("Failed to send error message to client. " + "Response is already committed.", ex);
            return;
        }

        try {
            response.resetBuffer();
            response.setStatus(errorInfo.statusCode);
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");

            PrintWriter pw = response.getWriter();

            pw.print("<html><head><title>Apache Chemistry OpenCMIS - "
                    + errorInfo.exceptionName
                    + " error</title>"
                    + "<style><!--H1 {font-size:24px;line-height:normal;font-weight:bold;background-color:#f0f0f0;color:#003366;border-bottom:1px solid #3c78b5;padding:2px;} "
                    + "BODY {font-family:Verdana,arial,sans-serif;color:black;font-size:14px;} "
                    + "HR {color:#3c78b5;height:1px;}--></style></head><body>");
            pw.print("<h1>HTTP Status " + errorInfo.statusCode + " - <!--exception-->" + errorInfo.exceptionName
                    + "<!--/exception--></h1>");
            pw.print("<p><!--message-->" + StringEscapeUtils.escapeHtml4(errorInfo.message) + "<!--/message--></p>");

            String st = ExceptionHelper.getStacktraceAsString(ex);
            if (st != null) {
                pw.print("<hr noshade='noshade'/><!--stacktrace--><pre>\n" + st
                        + "\n</pre><!--/stacktrace--><hr noshade='noshade'/>");
            }

            pw.print("</body></html>");
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            try {
                response.sendError(errorInfo.statusCode, errorInfo.message);
            } catch (IOException en) {
                // there is nothing else we can do
            }
        }
    }

}
