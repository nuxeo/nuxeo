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

package org.nuxeo.theme.jsf.servlets;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class NegotiationSelector extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(NegotiationSelector.class);

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {

        final String referer = request.getHeader("referer");
        if (referer == null) {
            log.error("No referer found.");
            response.sendRedirect("/nuxeo/nxthemes/error/negotiationSelectorMissingReferer.faces");
            return;
        }

        final String root = request.getContextPath();

        final String engine = request.getParameter("engine");
        if (engine != null) {
            response.addCookie(createCookie("nxthemes.engine", engine, root));
        }

        final String mode = request.getParameter("mode");
        if (mode != null) {
            response.addCookie(createCookie("nxthemes.mode", mode, root));
        }

        final String theme = request.getParameter("theme");
        if (theme != null) {
            response.addCookie(createCookie("nxthemes.theme", theme, root));
        }

        final String perspective = request.getParameter("perspective");
        if (perspective != null) {
            response.addCookie(createCookie("nxthemes.perspective",
                    perspective, root));
        }

        response.sendRedirect(referer);
    }

    private Cookie createCookie(final String name, final String value,
            final String root) {
        final Cookie cookie = new Cookie(name, value);

        // remove the cookie of the value is an empty string
        if (value.equals("")) {
            cookie.setMaxAge(0);
        }
        cookie.setPath(root);
        return cookie;
    }
}
