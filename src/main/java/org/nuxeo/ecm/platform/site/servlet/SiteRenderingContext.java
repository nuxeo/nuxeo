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
import org.nuxeo.ecm.platform.rendering.api.EmptyContextView;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingContextView;

/**
 * A simple context to be able to render templates not linked to document instances
 * This is useful for example to display static content but still using
 * the rendering engine and environment variable support
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteRenderingContext implements ServletRenderingContext {

    private SiteRequest request;

    public SiteRenderingContext(SiteRequest request) {
        this.request = request;
    }

    public RenderingContext getParentContext() {
        return null;
    }

    public DocumentModel getDocument() {
        return null;
    }

    public OutputStream getOut() {
        try {
            return request.getResponse().getOutputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public CoreSession getSession() {
        try {
            return request.getCoreSession();
        } catch (Exception e) {
            throw new RuntimeException("Failed to open a session", e);
        }
    }

    public String getTemplate() {
        String path = request.getPathInfo();
        File file = request.getSiteManager().getRootDirectory(); // make a method getResourceFile();
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
            return request.getResponse().getWriter();
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
        return request.getResponse();
    }

    public RenderingContextView getView() {
        return EmptyContextView.INSTANCE;
    }

}
