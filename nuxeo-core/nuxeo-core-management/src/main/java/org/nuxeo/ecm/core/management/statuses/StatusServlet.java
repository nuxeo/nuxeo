/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.ecm.core.management.statuses;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

/**
 * Servlet for retrieving Nuxeo services running status.
 * 
 * @since 9.3 this servlet returns a status based of all the probes registered for the healthCheck.
 */
public class StatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(StatusServlet.class);

    public static final String PARAM = "info";

    public static final String PARAM_STARTED = "started";

    public static final String PARAM_SUMMARY = "summary";

    public static final String PARAM_SUMMARY_KEY = "key";

    public static final String PARAM_RELOAD = "reload";

    private OSGiRuntimeService runtimeService;

    public static final String PROBE_PARAM = "probe";

    protected ProbeManager pm;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String param = req.getParameter(PARAM);
        if (param != null) {
            doPost(req, resp);
        } else {
            HealthCheckResult result = getOrRunHealthCheck(null);
            sendHealthCheckResponse(resp, result);
        }
    }

    protected void sendHealthCheckResponse(HttpServletResponse resp, HealthCheckResult result) throws IOException {
        if (result.healthy) {
            resp.setStatus(HttpServletResponse.SC_OK);
        } else {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        sendResponse(resp, result.toJson());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder response = new StringBuilder();
        String requestedInfo = req.getParameter(PARAM);
        if (requestedInfo.equals(PARAM_STARTED)) {
            getStartedInfo(response);
        } else if (requestedInfo.equals(PARAM_SUMMARY)) {
            String givenKey = req.getParameter(PARAM_SUMMARY_KEY);
            if (getRuntimeService().getProperty(Environment.SERVER_STATUS_KEY).equals(givenKey)) {
                getSummaryInfo(response);
            } else {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
        } else if (requestedInfo.equals(PARAM_RELOAD)) {
            if (isStarted()) {
                response.append("reload();");
            } else {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        } else if (requestedInfo.equals(PROBE_PARAM)) {
            String probetoEval = req.getParameter(PARAM_SUMMARY_KEY);
            try {
                HealthCheckResult result = getOrRunHealthCheck(probetoEval);
                sendHealthCheckResponse(resp, result);
            } catch (IllegalArgumentException e) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
        sendResponse(resp, response.toString());
    }

    protected void sendResponse(HttpServletResponse resp, String response) throws IOException {
        resp.setContentType("application/json");
        resp.setContentLength(response.getBytes().length);
        OutputStream out = resp.getOutputStream();
        out.write(response.getBytes());
        out.close();
    }

    private RuntimeService getRuntimeService() {
        if (runtimeService == null) {
            runtimeService = (OSGiRuntimeService) Framework.getRuntime();
        }
        return runtimeService;
    }

    protected void getSummaryInfo(StringBuilder response) {
        if (isStarted()) {
            StringBuilder msg = new StringBuilder();
            boolean isFine = runtimeService.getStatusMessage(msg);
            response.append(isFine).append("\n");
            response.append(msg);
        } else {
            response.append(false).append("\n");
            response.append("Runtime failed to start");
        }
    }

    protected void getStartedInfo(StringBuilder response) {
        response.append(isStarted()).toString();
    }

    private boolean isStarted() {
        return getRuntimeService() != null && runtimeService.isStarted();
    }

    @Override
    public void init() throws ServletException {
        log.debug("Ready.");
    }

    protected HealthCheckResult getOrRunHealthCheck(String probe) {
        ProbeManager pm = Framework.getService(ProbeManager.class);
        if (StringUtils.isEmpty(probe)) { // run all healthCheck probes
            return pm.getOrRunHealthChecks();
        } else {
            return pm.getOrRunHealthCheck(probe);
        }
    }
}
