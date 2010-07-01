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
import java.io.OutputStream;
import java.io.Serializable;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.html.Utils;

public final class Images extends HttpServlet implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request,
            final HttpServletResponse response) throws IOException {

        final String path = request.getPathInfo().substring(1);

        String ext = FileUtils.getFileExtension(path);
        String mimeType = Utils.getImageMimeType(ext);
        response.addHeader("content-type", mimeType);

        // Cache headers
        final String lifetime = "604800"; // 1 week
        final long now = System.currentTimeMillis();
        response.addHeader("Cache-Control", "max-age=" + lifetime);
        response.addHeader("Cache-Control", "must-revalidate");
        response.setDateHeader("Last-Modified", now);
        response.setDateHeader("Expires", now + new Long(lifetime) * 1000L);

        byte[] data = Manager.getThemeManager().getImageResource(path);

        OutputStream os = response.getOutputStream();
        os.write(data);
        os.close();
    }
}
