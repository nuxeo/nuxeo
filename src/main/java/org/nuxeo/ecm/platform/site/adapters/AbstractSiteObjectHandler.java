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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.site.api.SiteAwareObject;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.ecm.platform.site.api.SiteTemplateManager;
import org.nuxeo.ecm.platform.site.servlet.NoBodyResponse;
import org.nuxeo.ecm.platform.site.servlet.SiteConst;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;
import org.nuxeo.runtime.api.Framework;

/**
 * Abstract base class for SiteObject DocumentModel adapters
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public abstract class AbstractSiteObjectHandler implements SiteAwareObject {

    private static SiteTemplateManager templateManager;

    protected DocumentModel sourceDocument;

    protected String autoSlotId;


    public void setSourceDocument(DocumentModel doc) {
        sourceDocument = doc;

    }

    public abstract void doGet(SiteRequest request, HttpServletResponse response);

    public String getId() {
        return sourceDocument.getId();
    }

    public String getName() {
        return sourceDocument.getName();
    }

    public void doHead(SiteRequest request, HttpServletResponse response) throws SiteException {
        doGet(request, new NoBodyResponse(response));
    }

    public void doPost(SiteRequest request, HttpServletResponse response) throws SiteException {
        doGet(request, response);
    }

    public void doPut(SiteRequest request, HttpServletResponse response) throws SiteException {
        response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    public void doDelete(SiteRequest request, HttpServletResponse response) throws SiteException {
        response.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    public DocumentModelList getChildren() throws ClientException {
        return getCoreSession().getChildren(sourceDocument.getRef());
    }

    public String getSlotId() {
        return autoSlotId;
    }

    public boolean needsRendering(SiteRequest request) {
        if (request.EDIT_MODE.equals(request.getMode()) && getId().equals(
                request.getLiefSiteObjectId())) {
            // skip rendering if edit mode and last in traversal path
            return false;
        }
        return true;
    }

    public String getTitle() {
        return sourceDocument.getTitle();
    }

    public String getURL(SiteRequest request) {
        return request.getTraveredURL(this);
    }

    protected CoreSession getCoreSession() {
        return CoreInstance.getInstance().getSession(sourceDocument.getSessionId());
    }

    public boolean traverse(SiteRequest request, HttpServletResponse response)
            throws SiteException {
        if (request.getTraversalPath() == null) {
            autoSlotId = "main";
        } else {
            autoSlotId = "child" + request.getTraversalPath().size();
        }
        if (request.hasUnresolvedSubPath()) {
            throw new SiteException(
                    "Request contains unresolvedPath " + request.getUnresolvedPath(),
                    SiteConst.SC_NOT_FOUND);
        }
        return true;
    }

    protected SiteTemplateManager getTemplateManager() {
        if (templateManager == null) {
            templateManager = Framework.getLocalService(SiteTemplateManager.class);
        }
        return templateManager;
    }

    public DocumentModel getSourceDocument() {
        return sourceDocument;
    }

}
