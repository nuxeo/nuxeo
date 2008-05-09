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

package org.nuxeo.ecm.webengine;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingContextView;
import org.nuxeo.ecm.webengine.actions.ActionDescriptor;
import org.nuxeo.ecm.webengine.rendering.ServletRenderingContext;
import org.nuxeo.ecm.webengine.rendering.SiteDocumentView;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class WebObject implements ServletRenderingContext {

    protected WebObject next;
    protected WebObject prev;

    protected DocumentModel doc;
    protected final String name;
    protected final DefaultWebContext request;
    protected ObjectDescriptor desc;


    public WebObject(DefaultWebContext request, String name, DocumentModel doc) {
        this.request = request;
        this.name = name;
        this.doc = doc;
    }

    public final ActionDescriptor getAction(String action) {
        ObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getAction(action);
        }
        return null;
    }

    public String getActionScript(String action) {
        ActionDescriptor desc = getAction(action);
        String path;
        if (desc != null) {
            path = desc.getScript();
            if (path != null) {
                return path;
            }
        }
        WebRoot root = request.getRoot();
        if (doc != null) {
            String type = doc.getType();
            path = type + '/' + action + ".ftl";
            File file = root.getFile(path);
            if (file.isFile()) {
                return path;
            }
        }
        return "default/"+action+".ftl";
    }

    /**
     * @return the request.
     */
    public DefaultWebContext getSiteRequest() {
        return request;
    }

    public final String getName() {
        return name;
    }

    public final DocumentModel getDocument() {
        return doc;
    }

    public boolean isResolved() {
        return doc != null;
    }

    public final WebObject next() {
        return next;
    }

    public final WebObject prev() {
        return prev;
    }

    public boolean isRoot() {
        return request.head == this;
    }

    public final boolean isLast() {
        return request.tail == this;
    }

    public final boolean isLastResolved() {
        return request.lastResolved == this;
    }

    public final ObjectDescriptor getDescriptor() {
        if (desc == null && doc != null) {
            desc = request.engine.getInstanceOf(doc.getDocumentType());
        }
        return desc;
    }

    public final RequestHandler getRequestHandler() throws WebException {
        ObjectDescriptor desc = getDescriptor();
        if (desc != null) {
            return desc.getRequestHandler();
        }
        return null;
    }

    public String getPath() {
        StringBuilder buf = new StringBuilder();
        collectPath(buf);
        return buf.toString();
    }

    public String getAbsolutePath() {
        StringBuilder buf = new StringBuilder(request.getSiteBaseUrl());
        collectPath(buf);
        return buf.toString();
    }

    protected void collectPath(StringBuilder buf) {
        if (prev != null) {
            prev.collectPath(buf);
        }
        buf.append('/').append(name);
    }

    public boolean resolve(DocumentModel doc) {
        if (request.getFirstUnresolvedObject() == this) {
            this.doc = doc;
            request.lastResolved = this;
            return true;
        }
        return false;
    }

    public boolean traverse() throws WebException {
        ObjectDescriptor desc = getDescriptor();
        return desc != null ? desc.getRequestHandler().traverse(this) : false;
    }

    @Override
    public String toString() {
        return name + " [ " + (isResolved() ? doc.getPath() : "unresolved") + " ]";
    }

    /**  --------------- RenderingContext API ----------------  */

    public HttpServletRequest getRequest() {
        return request.getRequest();
    }

    public HttpServletResponse getResponse() {
        return request.getResponse();
    }

    public RenderingContext getParentContext() {
        return prev;
    }

    public RenderingContextView getView() {
        return SiteDocumentView.INSTANCE;
    }

    public OutputStream getOut() {
        try {
            return request.getResponse().getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CoreSession getSession() {
        try {
        return request.getCoreSession();
        } catch (Exception e) {
            throw new RuntimeException("Failed to open a session", e);
        }
    }

    public Writer getWriter() {
        try {
            return request.getResponse().getWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
