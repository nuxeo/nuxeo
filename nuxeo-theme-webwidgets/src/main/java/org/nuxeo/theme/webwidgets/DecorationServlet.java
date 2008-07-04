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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class DecorationServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DecorationServlet.class);

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {

        response.addHeader("content-type", "text/x-json");
        final PrintWriter responseWriter = response.getWriter();

        final String decorationName = request.getParameter("decoration");
        final Map<String, String> data = new HashMap<String, String>();

        final DecorationType decorationType = Manager.getDecorationType(decorationName);

        if (decorationType != null) {
            for (String mode : decorationType.getWindowDecorationModes()) {
                data.put(mode,
                        decorationType.getWidgetDecoration(mode).getContent());
            }
        } else {
            log.error("Decoration not found: " + decorationName);
        }

        responseWriter.write(org.nuxeo.theme.html.Utils.toJson(data));
        responseWriter.flush();
    }

}
