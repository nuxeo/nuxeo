/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;

/**
 * Servlet for retrieving Nuxeo services running status
 */
public class StatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(StatusServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        OSGiRuntimeService runtimeService;
        try {
            runtimeService = (OSGiRuntimeService) Framework.getRuntime();
        } catch (Exception e) {
            return;
        }
        StringBuilder response = new StringBuilder();
        if (pathInfo.contains("runtime")) {
            response.append(
                    runtimeService != null && runtimeService.isStarted()).toString();
        } else if (pathInfo.contains("summary")) {
            if (runtimeService != null && runtimeService.isStarted()) {
                StringBuilder msg = new StringBuilder();
                boolean isError = runtimeService.getStatusMessage(msg);
                response.append(isError).append("\n");
                response.append(msg);
            } else {
                response.append(false).append("\n");
                response.append("Runtime failed to start");
            }
        }
        resp.setContentType("text/plain");
        resp.setContentLength(response.toString().getBytes().length);
        OutputStream out = resp.getOutputStream();
        out.write(response.toString().getBytes());
        out.close();
    }

    @Override
    public void init() throws ServletException {
        log.debug("Ready.");
    }

}
