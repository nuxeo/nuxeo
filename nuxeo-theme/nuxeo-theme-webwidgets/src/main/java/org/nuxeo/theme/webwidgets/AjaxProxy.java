/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webwidgets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class AjaxProxy extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(AjaxProxy.class);

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {

        // Prevent direct access to the proxy from other hosts.
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final String sourceUrl = request.getParameter("url");
        final String cache = request.getParameter("cache");

        response.addHeader("content-type", "text/plain");

        if (cache != null && cache.matches("[0-9]+")) {
            final long now = System.currentTimeMillis();
            final long lifetime = Long.valueOf(cache) * 1000L;
            response.addHeader("Cache-Control", "max-age=" + lifetime);
            response.addHeader("Cache-Control", "must-revalidate");
            response.setDateHeader("Last-Modified", now);
            response.setDateHeader("Expires", now + new Long(lifetime) * 1000L);
        }

        URL url = null;
        try {
            url = new URL(sourceUrl);
        } catch (MalformedURLException e) {
            log.error("Incorrect URL: " + sourceUrl);
        }
        String content = "";
        if (url != null) {
            content = Utils.fetchUrl(url);
        }

        final PrintWriter responseWriter = response.getWriter();
        responseWriter.append(content);
        responseWriter.flush();
    }

}
