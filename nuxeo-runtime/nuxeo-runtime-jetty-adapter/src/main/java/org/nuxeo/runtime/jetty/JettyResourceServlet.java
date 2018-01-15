/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple resource servlet used as default servlet when EP is deployed in Jetty.
 *
 * @author Thierry Delprat
 */
public class JettyResourceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected static final int BUFFER_SIZE = 1024 * 10;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String context = req.getContextPath();
        String resourceVPath = req.getRequestURI().substring(context.length());
        String resourcePath = getServletContext().getRealPath(resourceVPath);

        if (!checkAccess(resourcePath)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        File resource = new File(resourcePath);
        if (resource.exists()) {
            if (resource.isDirectory()) {
                resp.sendRedirect("index.jsp");
                // resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            sendFile(resource, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    protected boolean checkAccess(String resourcePath) {
        // XXX
        return true;
    }

    protected void sendFile(File resource, HttpServletResponse resp) throws ServletException, IOException {
        try (InputStream in = new FileInputStream(resource); //
                OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
        } finally {
            resp.flushBuffer();
        }
    }

}
