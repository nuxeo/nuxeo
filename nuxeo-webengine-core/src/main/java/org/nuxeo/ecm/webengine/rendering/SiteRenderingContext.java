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

package org.nuxeo.ecm.webengine.rendering;

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
import org.nuxeo.ecm.webengine.WebContext;

/**
 * A simple context to be able to render templates not linked to document instances
 * This is useful for example to display static content but still using
 * the rendering engine and environment variable support
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteRenderingContext implements ServletRenderingContext {

    private final WebContext context;

    public SiteRenderingContext(WebContext context) {
        this.context = context;
    }

    public WebContext getWebContext() {
        return context;
    }

    public RenderingContext getParentContext() {
        return null;
    }

    public DocumentModel getDocument() {
        return null;
    }

    public OutputStream getOut() {
        try {
            return context.getResponse().getOutputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public CoreSession getSession() {
        try {
            return context.getCoreSession();
        } catch (Exception e) {
            throw new RuntimeException("Failed to open a session", e);
        }
    }

    public Writer getWriter() {
        try {
            return context.getResponse().getWriter();
        } catch (IOException e) {
            return null;
        }
    }

    public WebContext getContext() {
        return context;
    }

    public HttpServletRequest getRequest() {
        return context.getRequest();
    }

    public HttpServletResponse getResponse() {
        return context.getResponse();
    }

    public RenderingContextView getView() {
        return EmptyContextView.INSTANCE;
    }

}
