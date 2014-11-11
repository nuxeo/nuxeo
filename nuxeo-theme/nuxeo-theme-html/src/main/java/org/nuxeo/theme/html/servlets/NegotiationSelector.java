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

package org.nuxeo.theme.html.servlets;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class NegotiationSelector extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {

        final String referrer = request.getHeader("referer");
        if (referrer == null) {
            response.getWriter().write("no referrer found");
            return;
        }

        final String engine = request.getParameter("engine");
        if (engine != null) {
            response.addCookie(createCookie("nxthemes.engine", engine));
        }

        final String mode = request.getParameter("mode");
        if (mode != null) {
            response.addCookie(createCookie("nxthemes.mode", mode));
        }

        final String theme = request.getParameter("theme");
        if (theme != null) {
            response.addCookie(createCookie("nxthemes.theme", theme));
        }

        final String perspective = request.getParameter("perspective");
        if (perspective != null) {
            response.addCookie(createCookie("nxthemes.perspective", perspective));
        }

        response.sendRedirect(referrer);
    }

    private Cookie createCookie(final String name, final String value) {
        final Cookie cookie = new Cookie(name, value);

        // remove the cookie of the value is an empty string
        if (value.equals("")) {
            cookie.setMaxAge(0);
        }
        cookie.setPath("/");
        return cookie;
    }
}
