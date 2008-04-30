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

package org.nuxeo.ecm.webengine.rendering;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.rendering.api.DocumentContextView;
import org.nuxeo.ecm.platform.rendering.api.DocumentField;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.webengine.SiteObject;
import org.nuxeo.ecm.webengine.SiteRequest;

public class SiteDocumentView extends DocumentContextView {

    public SiteDocumentView() {
    }

    @Override
    protected void initialize() {
        super.initialize();
        addField(SITEADAPTER);
        addField(DOCURL);
        addField(REQUEST);
        addField(ACTIONS);
        addField(DESCRIPTOR);
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
            return "page";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            return ctx;
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

    protected static final DocumentField ACTIONS = new DocumentField() {
        public String getName() {
            return "actions";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            if (ctx instanceof SiteObject) {
                SiteObject sCtx = (SiteObject) ctx;
                return sCtx.getDescriptor().getEnabledActions(sCtx);
            }
            return null;
        }
    };

    protected static final DocumentField DESCRIPTOR = new DocumentField() {
        public String getName() {
            return "descriptor";
        }

        public Object getValue(DocumentModel doc, RenderingContext ctx) throws Exception {
            if (ctx instanceof SiteObject) {
                SiteObject sCtx = (SiteObject) ctx;
                return sCtx.getDescriptor();
            }
            return null;
        }
    };

    /**
     * The singleton instance that should be used by clients.
     * Warn that this static field must be defined at the end of the class after any other field class
     * since it will try to register these fields (otherwise fields will not be defined yet at the time of
     * the initialization of that static member
     */
    public static final SiteDocumentView INSTANCE = new SiteDocumentView();

}
