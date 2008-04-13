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

package org.nuxeo.ecm.platform.site.rendering;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.DocumentView;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.site.servlet.SiteObject;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;
import org.nuxeo.ecm.platform.site.template.SiteObjectView;
import org.nuxeo.ecm.platform.site.template.SitePageTemplate;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SiteRenderingContext implements RenderingContext {

    private final static SiteDocumentView DOCUMENT_VIEW = new SiteDocumentView();

    private SiteRequest request;
    private HttpServletResponse response;
    private SiteObject currentSiteObject;

    public SiteRenderingContext(SiteRequest request, HttpServletResponse response,
            SiteObject currentObject) {
        this.request = request;
        this.response = response;
        this.currentSiteObject = currentObject;
    }

    public RenderingContext createChildContext() {
        SiteObject child = currentSiteObject.next();
        if (child.isResolved() && !child.isRoot()) {
            return new SiteRenderingContext(request, response, child);
        } else {
            return null;
        }
    }

    public DocumentModel getDocument() {
        return currentSiteObject.getDocument();
    }

    public OutputStream getOut() {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public CoreSession getSession() {
        return request.getCoreSession();
    }

    public String getTemplate() {
        SitePageTemplate template = currentSiteObject.getSiteTemplate();
        SiteObjectView view = null;
        if (currentSiteObject.isLastResolved()) {
            view = template.getView(request.getMode().toLowerCase());
        } else {
            view = template.getView("view");
        }
        if (view != null) {
            return view.getTemplate().toExternalForm();
        } else {
            return "error";
        }
    }

    public Writer getWriter() {
        try {
            return response.getWriter();
        } catch (IOException e) {
            return null;
        }
    }

    public SiteRequest getRequest() {
        return request;
    }

    public DocumentView getDocumentView() {
        return DOCUMENT_VIEW;
    }

}
