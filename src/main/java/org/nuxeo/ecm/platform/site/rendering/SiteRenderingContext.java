/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
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
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;
import org.nuxeo.ecm.platform.site.template.SiteObject;
import org.nuxeo.ecm.platform.site.template.SiteObjectView;

public class SiteRenderingContext implements RenderingContext {

    private SiteRequest request;
    private HttpServletResponse response;
    private SiteAwareObject currentSiteObject;

    public SiteRenderingContext(SiteRequest request, HttpServletResponse response,
            SiteAwareObject currentObject) {
        this.request = request;
        this.response = response;
        this.currentSiteObject = currentObject;
    }

    public RenderingContext createChildContext() {
        SiteAwareObject child = request.getTraversalChild(currentSiteObject);
        if (child != null) {
            return new SiteRenderingContext(request, response, child);
        } else {
            return null;
        }
    }

    public DocumentModel getDocument() {
        return currentSiteObject.getSourceDocument();
    }

    public OutputStream getOut() {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            return null;
        }
    }

    public CoreSession getSession() {
        return request.getDocumentManager();
    }

    public String getTemplate() {
        SiteObject cfg = currentSiteObject.getSiteConfiguration(request);
        Object[] ar = cfg.getViewMap().values().toArray();
        SiteObjectView view = cfg.getView(request.getMode().toLowerCase());
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
}
