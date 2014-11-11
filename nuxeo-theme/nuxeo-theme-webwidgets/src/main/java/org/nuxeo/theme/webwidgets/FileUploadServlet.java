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
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;

public final class FileUploadServlet extends HttpServlet implements
        Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(FileUploadServlet.class);

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

        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<?> fileItems = null;
        try {
            fileItems = upload.parseRequest(request);
        } catch (FileUploadException e) {
            log.error("Could not upload file", e);
        }

        ManagerLocal manager = (ManagerLocal) Component.getInstance(
                "nxthemesWebWidgetManager", true);

        if (fileItems == null) {
            log.error("No file upload found.");
            return;
        }

        WidgetData data = null;
        Iterator<?> it = fileItems.iterator();
        if (it.hasNext()) {
            FileItem fileItem = (FileItem) it.next();
            if (!fileItem.isFormField()) {
                /* The file item contains an uploaded file */
                final String contentType = fileItem.getContentType();
                final byte[] fileData = fileItem.get();
                final String filename = fileItem.getName();
                data = new WidgetData(contentType, filename, fileData);
            }
        }

        manager.setWidgetData(providerName, widgetUid, dataName, data);
        PrintWriter writer = response.getWriter();
        response.addHeader("content-type", "text/html");
        writer.write(String.format(
                "<script type=\"text/javascript\">window.parent.NXThemesWebWidgets.getUploader('%s', '%s', '%s').complete();</script>",
                providerName, widgetUid, dataName));
        writer.flush();
    }
}
