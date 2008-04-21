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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingContextView;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.rendering.SiteDocumentView;
import org.nuxeo.ecm.platform.site.template.SiteManager;
import org.nuxeo.ecm.platform.site.template.SitePageTemplate;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class SiteObject implements ServletRenderingContext {

    protected SiteObject next;
    protected SiteObject prev;

    protected DocumentModel doc;
    protected SiteAwareObject adapter;
    protected SitePageTemplate template;
    protected String name;

    protected SiteRequest request;


    public SiteObject(SiteRequest request, String name, DocumentModel doc) {
        this.request = request;
        this.name = name;
        this.doc = doc;
    }

    /**
     * @return the request.
     */
    public SiteRequest getSiteRequest() {
        return request;
    }

    public final String getName() {
        return name;
    }

    public final DocumentModel getDocument() {
        return doc;
    }

    public final SitePageTemplate getSiteTemplate() {
        if (template == null) {
            SiteManager mgr = request.getSiteManager();
            template = mgr.resolve(doc);
            if (template == null) {
                template = mgr.getDefaultSiteObject();
            }
        }
        return template;
    }

    public boolean isResolved() {
        return doc != null;
    }

    public final SiteObject next() {
        return next;
    }

    public final SiteObject prev() {
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

    public final SiteAwareObject getAdapter() {
        if (adapter == null) {
            adapter = doc.getAdapter(SiteAwareObject.class);
        }
        return adapter;
    }

    public String getPath() {
        StringBuilder buf = new StringBuilder();
        collectPath(buf);
        return buf.toString();
    }

    public String getUrlPath() {
        StringBuilder buf = new StringBuilder(request.getSiteBaseUrl());
        collectPath(buf);
        return buf.toString();
    }

    protected void collectPath(StringBuilder buf) {
        if (prev != null) prev.collectPath(buf);
        buf.append("/").append(name);
    }

    public boolean resolve(DocumentModel doc) {
        if (request.getFirstUnresolvedObject() == this) {
            this.doc = doc;
            request.lastResolved = this;
            return true;
        }
        return false;
    }


    @Override
    public String toString() {
        return name + " [ "+ (isResolved()?getDocument().getPath():"unresolved") +" ]";
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
