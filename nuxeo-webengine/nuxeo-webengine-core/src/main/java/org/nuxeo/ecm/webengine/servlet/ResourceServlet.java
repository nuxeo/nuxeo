/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webengine.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.Module;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet for accessing common file resources
 *
 * @author <a href="mailto:bs@nuxeo.com">Stefanescu Bogdan</a>
 */
public class ResourceServlet extends HttpServlet {

    protected static final Log log = LogFactory.getLog(ResourceServlet.class);

    private static final long serialVersionUID = 6548084847887645044L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        WebEngine engine = Framework.getService(WebEngine.class);
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

        Module module = engine.getModule(moduleName, (WebContext)req.getAttribute(WebContext.class.getName()));
        if (module == null) {
            resp.sendError(404);
            return;
        }

        try {
            service(req, resp, module, "/resources" + path);
        } catch (IOException e) {
            log.error("Unable to serve resource for " + path, e);
            resp.sendError(404);
        }
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp, Module module, String path)
            throws IOException {

        ScriptFile file = module.getSkinResource(path);
        if (file != null) {
            long lastModified = file.lastModified();
            resp.setDateHeader("Last-Modified:", lastModified);
            resp.addHeader("Cache-Control", "public");
            resp.addHeader("Server", "Nuxeo/WebEngine-1.0");

            WebEngine engine = Framework.getService(WebEngine.class);
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

    protected static void sendBinaryContent(ScriptFile file, HttpServletResponse resp) throws IOException {
        OutputStream out = resp.getOutputStream();
        try (InputStream in = file.getInputStream()) {
            IOUtils.copy(in, out);
            out.flush();
        }
    }

    protected static void sendTextContent(ScriptFile file, HttpServletResponse resp) throws IOException {
        // Writer out = resp.getWriter();
        OutputStream out = resp.getOutputStream();
        try (InputStream in = file.getInputStream()) {
            IOUtils.copy(in, out);
            out.flush();
        }
    }

}
