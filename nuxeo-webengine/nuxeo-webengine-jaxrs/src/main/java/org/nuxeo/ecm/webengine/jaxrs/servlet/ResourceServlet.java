/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A simple servlet which is serving resources provided by the servlet context
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ResourceServlet extends HttpServlet {

    private static final long serialVersionUID = -3901124568792063159L;

    protected String index;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        index = config.getInitParameter("index");
        if (index == null) {
            index = "index.html";
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.length() == 0) {
            pathInfo = index;
        } else if (pathInfo.endsWith("/")) {
            pathInfo += index;
        }
        InputStream in = getServletContext().getResourceAsStream(pathInfo);
        if (in != null) {
            String ctype = getServletContext().getMimeType(pathInfo);
            if (ctype != null) {
                resp.addHeader("Content-Type", ctype);
            }
            try {
                OutputStream out = resp.getOutputStream();
                byte[] bytes = new byte[1024*64];
                int r = in.read(bytes);
                while (r > -1) {
                    if (r > 0) {
                        out.write(bytes, 0, r);
                    }
                    r = in.read(bytes);
                }
                out.flush();
            } finally {
                in.close();
            }
        }
    }

}
