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
import java.io.Serializable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;

public final class DataServlet extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DataServlet.class);

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        final String providerName = request.getParameter("provider");
        final String widgetUid = request.getParameter("widget");
        final String dataName = request.getParameter("data");
        ManagerLocal manager = (ManagerLocal) Component.getInstance(
                "nxthemesWebWidgetManager", true);
        WidgetData data = manager.getWidgetData(providerName, widgetUid,
                dataName);
        if (data == null) {
            log.error("Could not get widget data for " + providerName + ", "
                    + dataName);
        } else {
            final ServletOutputStream os = response.getOutputStream();
            response.addHeader("content-type", data.getContentType());
            os.write(data.getContent());
            os.flush();
        }
    }
}
