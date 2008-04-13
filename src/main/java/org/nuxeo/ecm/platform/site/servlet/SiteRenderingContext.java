/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.site.servlet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingContextView;
import org.nuxeo.ecm.platform.rendering.api.EmptyContextView;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.site.template.SiteManager;

/**
 * A simple context to be able to render templates not linked to document instances
 * This is useful for example to display static content but still using
 * the rendering engine and environment variable support
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteRenderingContext implements ServletRenderingContext {

    private SiteManager manager;
    private HttpServletRequest request;
    private HttpServletResponse response;

    public SiteRenderingContext(HttpServletRequest request, HttpServletResponse response, SiteManager manager) {
        this.request = request;
        this.response = response;
        this.manager = manager;
    }

    public RenderingContext getParentContext() {
        return null;
    }

    public RenderingContext getChildContext() {
        return null;
    }

    public DocumentModel getDocument() {
        return null;
    }

    public OutputStream getOut() {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public CoreSession getSession() {
        return null;
    }

    public String getTemplate() {
        String path = request.getPathInfo();
        File file = manager.getRootDirectory(); // make a method getResourceFile();
        if (path != null) {
            file = new File(file, path);
        }
        if (file.isDirectory()) {
            file = new File(file, "index.ftl");
        } else if (!file.exists()) {
            return "error.ftl"; //TODO
        }
        try {
        return file.toURI().toURL().toExternalForm();
        } catch (Exception e) {
            return null;
        }
    }

    public Writer getWriter() {
        try {
            return response.getWriter();
        } catch (IOException e) {
            return null;
        }
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return the response.
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    public RenderingContextView getDocumentView() {
        return EmptyContextView.INSTANCE;
    }

}
