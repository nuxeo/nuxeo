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

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.chemistry.opencmis.commons.impl.JSONConstants.ERROR_EXCEPTION;
import static org.apache.chemistry.opencmis.commons.impl.JSONConstants.ERROR_MESSAGE;
import static org.apache.chemistry.opencmis.commons.impl.JSONConstants.ERROR_STACKTRACE;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.impl.json.JSONObject;
import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.commons.server.CmisService;
import org.apache.chemistry.opencmis.server.impl.browser.AbstractBrowserServiceCall;
import org.apache.chemistry.opencmis.server.impl.browser.BrowserCallContextImpl;
import org.apache.chemistry.opencmis.server.impl.browser.CmisBrowserBindingServlet;
import org.apache.chemistry.opencmis.server.shared.Dispatcher;
import org.apache.chemistry.opencmis.server.shared.ExceptionHelper;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.opencmis.bindings.NuxeoCmisErrorHelper.ErrorInfo;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subclass NuxeoCmisBrowserBindingServlet to inject a virtual-hosted base URL if needed.
 */
public class NuxeoCmisBrowserBindingServlet extends CmisBrowserBindingServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(NuxeoCmisBrowserBindingServlet.class);

    public static final NuxeoBrowserServiceCall CALL = new NuxeoBrowserServiceCall();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        String baseUrl = VirtualHostHelper.getBaseURL(request);
        if (baseUrl != null) {
            baseUrl = StringUtils.stripEnd(baseUrl, "/") + request.getServletPath() + "/"
                    + AbstractBrowserServiceCall.REPOSITORY_PLACEHOLDER + "/";
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
    public void printError(CallContext context, Exception ex, HttpServletRequest request, HttpServletResponse response) {
        ErrorInfo errorInfo = extractError(ex);
        if (response.isCommitted()) {
            LOG.warn("Failed to send error message to client. " + "Response is already committed.", ex);
            return;
        }

        String token = (context instanceof BrowserCallContextImpl ? ((BrowserCallContextImpl) context).getToken()
                : null);

        if (token == null) {
            response.resetBuffer();
            CALL.setStatus(request, response, errorInfo.statusCode);

            String message = ex.getMessage();
            if (!(ex instanceof CmisBaseException)) {
                message = "An error occurred!";
            }

            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put(ERROR_EXCEPTION, errorInfo.exceptionName);
            jsonResponse.put(ERROR_MESSAGE, errorInfo.message);

            String st = ExceptionHelper.getStacktraceAsString(ex);
            if (st != null) {
                jsonResponse.put(ERROR_STACKTRACE, st);
            }

            try {
                CALL.writeJSON(jsonResponse, request, response);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
                try {
                    response.sendError(errorInfo.statusCode, message);
                } catch (IOException en) {
                    // there is nothing else we can do
                }
            }
        } else {
            CALL.setStatus(request, response, SC_OK);
            response.setContentType(AbstractBrowserServiceCall.HTML_MIME_TYPE);
            response.setContentLength(0);

            if (context != null) {
                CALL.setCookie(request, response, context.getRepositoryId(), token,
                        CALL.createCookieValue(errorInfo.statusCode, null, errorInfo.exceptionName, ex.getMessage()));
            }
        }
    }

    // this class exists in order to call AbstractBrowserServiceCall methods
    public static class NuxeoBrowserServiceCall extends AbstractBrowserServiceCall {
        @Override
        public void serve(CallContext context, CmisService service, String repositoryId, HttpServletRequest request,
                HttpServletResponse response) {
            // no implementation
        }
    }

}
