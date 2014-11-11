/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet for accessing common file resources
 *
 * @author <a href="mailto:bs@nuxeo.com">Stefanescu Bogdan</a>
 */
public class ResourceServlet extends HttpServlet {

    private static final long serialVersionUID = 6548084847887645044L;

    protected WebEngine engine;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if (engine == null) {
            try {
                engine = Framework.getService(WebEngine.class);
            } catch (Exception e) {
                throw new ServletException("Failed to lookup WebEngine service", e);
            }
        }

        String path = req.getPathInfo();
        if (path == null) {
            resp.sendError(404);
            return;
        }
        int p = path.indexOf('/', 1);
        String moduleName = null;
        if (p > -1) {
            moduleName = path.substring(1, p);
            path = path.substring(p);
        } else {
            resp.sendError(404);
            return;
        }

        Module module = engine.getModule(moduleName);
        if (module == null) {
            resp.sendError(404);
            return;
        }

        service(req, resp, module, "/resources"+path);
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp,
            Module module, String path) throws IOException {

        ScriptFile file = module.getSkinResource(path);
        if (file != null) {
            long lastModified = file.lastModified();
            resp.setDateHeader("Last-Modified:", lastModified);
            resp.addHeader("Cache-Control", "public");
            resp.addHeader("Server", "Nuxeo/WebEngine-1.0");

            String mimeType = engine.getMimeType(file.getExtension());
            if (mimeType == null) {
                mimeType = "text/plain";
            }
            resp.setContentType(mimeType);
            if (mimeType.startsWith("text/")) {
                sendTextContent(file, resp);
            } else {
                sendBinaryContent(file, resp);
            }
            return;
        }
        resp.sendError(404);
    }

    protected static void sendBinaryContent(ScriptFile file, HttpServletResponse resp)
            throws IOException {
        OutputStream out = resp.getOutputStream();
        InputStream in = file.getInputStream();
        try {
            FileUtils.copy(in, out);
        }
        finally {
            in.close();
        }
        out.flush();
    }

    protected static void sendTextContent(ScriptFile file, HttpServletResponse resp)
            throws IOException {
        //Writer out = resp.getWriter();
        OutputStream out = resp.getOutputStream();
        InputStream in = file.getInputStream();
        try {
            FileUtils.copy(in, out);
        }
        finally {
            in.close();
        }
        out.flush();
    }

}
