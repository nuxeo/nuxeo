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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.site.api.SiteException;
import org.nuxeo.ecm.platform.site.servlet.SiteObject;
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;

public class WikiSiteObjectHandler extends FolderishSiteObjectHandler {

    private String templateName;
    private static final String CREATE_KEY = "create";
    private static final String DEFAULT_PAGE_ID = "index";

    private DocumentModel createSubPage(SiteRequest request, String pageId)
            throws Exception {
        CoreSession session = request.getCoreSession();
        DocumentModel newPage = session.createDocumentModel(sourceDocument.getPathAsString(), pageId,
                "Note");
        newPage.setProperty("dublincore", "title", pageId);
        //newPage.setProperty("note", "note", "This is new page ${title}");
        newPage.setProperty("note", "note", "");
        newPage = session.createDocument(newPage);
        session.save();
        return newPage;
    }

    @Override
    public boolean traverse(SiteRequest request, HttpServletResponse response)
            throws SiteException {
        SiteObject unresolved = request.getFirstUnresolvedObject();
        if (unresolved != null) {
            String createFlag = request.getParameter(CREATE_KEY);
            String pageId = unresolved.getName();
            if ("true".equalsIgnoreCase(createFlag)) {
                try {
                    DocumentModel newPage = createSubPage(request, pageId);
                    unresolved.resolve(newPage);
                    request.setMode(SiteRequest.EDIT_MODE);
                } catch (Exception e) {
                    throw new SiteException("Error while creating wiki page", e);
                }
            } else {
                request.setAttribute("pageToCreate", pageId);
                request.setMode(SiteRequest.CREATE_MODE);
            }
        } else if (request.isRootRequest()) {
            DocumentModel indexPage = null;
            try {
                indexPage = getCoreSession().getChild(sourceDocument.getRef(), DEFAULT_PAGE_ID);
                request.addSiteObject(DEFAULT_PAGE_ID, indexPage);
            } catch (ClientException ce) {
                try {
                    indexPage = createSubPage(request, DEFAULT_PAGE_ID);
                    request.addSiteObject(DEFAULT_PAGE_ID, indexPage);
                } catch (Exception e) {
                    throw new SiteException("Error while creating wiki page", e);
                }
            }
        }
        return true;
    }

    @Override
    public void doGet(SiteRequest request, HttpServletResponse response) {

    }

}
