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

package org.nuxeo.ecm.platform.site.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.site.template.SiteManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Servlet for accessing common file resources
 *
 * @author <a href="mailto:bs@nuxeo.com">Stefanescu Bogdan</a>
 *
 */
public class ResourceServlet extends HttpServlet {

    private static final long serialVersionUID = 6548084847887645044L;

    private static final Log log = LogFactory.getLog(ResourceServlet.class);

    private File root;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            SiteManager mgr = Framework.getService(SiteManager.class);
            root = mgr.getRootDirectory();
        } catch (Exception e) {
            throw new ServletException("Failed to get SiteManager", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getPathInfo();
        File file = new File(root, path);
        if (file.isFile()) {
            String mimeType = getServletConfig().getServletContext().getMimeType(file.getName());
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
        } else if (file.isDirectory()) {
            //TODO
            Writer out = resp.getWriter();
            for (String name : file.list()) {
                out.write("<div><a href=\""+req.getRequestURI()+"/"+name+"\">"+name+"</a></div>");
            }
            out.flush();
            return;
        }

        resp.sendError(404);
    }


    protected void sendBinaryContent(File file, HttpServletResponse resp) throws IOException {
        OutputStream out = resp.getOutputStream();
        InputStream in = new FileInputStream(file);
        FileUtils.copy(in, out);
        out.flush();
    }

    protected void sendTextContent(File file, HttpServletResponse resp) throws IOException {
        //Writer out = resp.getWriter();
        OutputStream out = resp.getOutputStream();
        InputStream in = new FileInputStream(file);
        FileUtils.copy(in, out);
        out.flush();
    }

}
