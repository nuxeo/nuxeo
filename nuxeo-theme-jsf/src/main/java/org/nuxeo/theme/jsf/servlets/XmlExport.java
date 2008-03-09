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
import java.io.PrintWriter;
import java.io.Serializable;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.ThemeElement;
import org.nuxeo.theme.themes.ThemeSerializer;

public final class XmlExport extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(XmlExport.class);

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {

        final String themeName = request.getParameter("theme");
        final String download = request.getParameter("download");
        final String indent = request.getParameter("indent");
        if (themeName == null) {
            return;
        }
        final ThemeElement theme = Manager.getThemeManager().getThemeByName(themeName);
        if (theme == null) {
            return;
        }

        final ThemeSerializer serializer = new ThemeSerializer();
        int indenting = 0;
        if (indent != null) {
            try {
                indenting = Integer.valueOf(indent);
            } catch (NumberFormatException e) {
                log.error("Incorrect indentation value: " + indent);
            }
        }
        final String xml = serializer.serializeToXml(theme, indenting);
        if (xml == null) {
            return;
        }

        response.addHeader("content-type", "text/xml");
        if (download != null) {
            response.addHeader("Content-disposition", String.format(
                    "attachment; filename=theme-%s.xml", theme.getName()));
        }

        final PrintWriter responseWriter = response.getWriter();
        responseWriter.write(xml);
        responseWriter.flush();
    }

}
