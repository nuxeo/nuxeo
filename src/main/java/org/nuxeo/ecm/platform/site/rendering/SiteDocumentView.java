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
import org.nuxeo.ecm.platform.rendering.api.DocumentContextView;
import org.nuxeo.ecm.platform.rendering.api.DocumentField;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.servlet.SiteObject;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;

public class SiteDocumentView extends DocumentContextView {


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
            SiteRequest request = sCtx.getSiteRequest();
            return request;
        }
        return null;
    }

    protected static final DocumentField SITEADAPTER = new DocumentField() {
        public String getName() {
            return "siteAdapter";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return doc.getAdapter(SiteAwareObject.class);
        }
    };

    protected static final DocumentField DOCURL = new DocumentField() {
        public String getName() {
            return "docURL";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return ((SiteObject)ctx).getAbsolutePath();
        }
    };

    protected static final DocumentField REQUEST = new DocumentField() {
        public String getName() {
            return "request";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return getRequest(ctx, doc);
        }
    };

    /**
     * The singleton instance that should be used by clients.
     * Warn that this static field must be defined at the end of the class after any other field class
     * since it will try to register these fields (otherwise fields will not be defined yet at the time of
     * the initialization of that static member
     */
    public final static SiteDocumentView INSTANCE = new SiteDocumentView();

}
