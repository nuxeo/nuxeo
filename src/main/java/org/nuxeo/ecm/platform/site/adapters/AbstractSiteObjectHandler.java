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

package org.nuxeo.ecm.platform.site.adapters;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.SiteManager;
import org.nuxeo.ecm.platform.site.SiteRequest;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract base class for SiteObject DocumentModel adapters
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public abstract class AbstractSiteObjectHandler implements SiteAwareObject {

    private SiteManager siteManager;

    protected DocumentModel sourceDocument;


    public AbstractSiteObjectHandler() {
    }

    public AbstractSiteObjectHandler(DocumentModel doc) {
        sourceDocument = doc;
    }

    public void setSourceDocument(DocumentModel doc) {
        sourceDocument = doc;

    }

    public String getId() {
        return sourceDocument.getId();
    }

    public String getName() {
        return sourceDocument.getName();
    }

    public void doGet(SiteRequest request) throws SiteException {

    }

    public void doHead(SiteRequest request) throws SiteException {
        doGet(request);
    }

    public void doPost(SiteRequest request) throws SiteException {
        doGet(request);
    }

    public void doPut(SiteRequest request) throws SiteException {
        request.getResponse().setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    public void doDelete(SiteRequest request) throws SiteException {
        request.getResponse().setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    public String getURL(SiteRequest request) {
        return "TODO"; //TODO
        //return request.getURL(this);
    }

    protected CoreSession getCoreSession() {
        return CoreInstance.getInstance().getSession(sourceDocument.getSessionId());
    }

    public boolean traverse(SiteRequest request)
            throws SiteException {
        return true;
    }


    protected SiteManager getSiteManager() {
        if (siteManager == null) {
            siteManager = Framework.getLocalService(SiteManager.class);
        }
        return siteManager;
    }

    public DocumentModel getSourceDocument() {
        return sourceDocument;
    }



}
