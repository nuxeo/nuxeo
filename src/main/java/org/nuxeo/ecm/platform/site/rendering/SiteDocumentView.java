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

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.DefaultDocumentView;
import org.nuxeo.ecm.platform.rendering.api.DocumentViewField;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.servlet.SiteObject;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;

public class SiteDocumentView extends DefaultDocumentView {

    public SiteDocumentView() {
    }

    @Override
    protected void initialize() {
        super.initialize();
        addField(SITEADAPTER);
        addField(DOCURL);
        addField(REQUEST);
    }

    protected static SiteRequest getRequest(RenderingContext ctx, DocumentModel doc) {
        if (ctx instanceof SiteObject) {
            SiteObject sCtx = (SiteObject) ctx;
            SiteRequest request = sCtx.getRequest();
            return request;
        }
        return null;
    }

    protected static final DocumentViewField SITEADAPTER = new DocumentViewField() {
        public String getName() {
            return "siteAdapter";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getAdapter(SiteAwareObject.class);
        }
    };

    protected static final DocumentViewField DOCURL = new DocumentViewField() {
        public String getName() {
            return "docURL";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return ((SiteObject)ctx).getUrlPath();
            //return doc.getAdapter(SiteAwareObject.class).getURL(getRequest(ctx, doc));
        }
    };

    protected static final DocumentViewField REQUEST = new DocumentViewField() {
        public String getName() {
            return "request";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return getRequest(ctx, doc);
        }
    };

}
