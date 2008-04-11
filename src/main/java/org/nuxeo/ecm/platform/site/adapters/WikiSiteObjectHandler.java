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
import org.nuxeo.ecm.platform.site.servlet.SiteRequest;
import org.nuxeo.ecm.platform.site.template.SiteObject;

public class WikiSiteObjectHandler extends FolderishSiteObjectHandler {

    private String templateName;
    private static final String CREATE_KEY = "create";
    private static final String DEFAULT_PAGE_ID = "index";

    private DocumentModel createSubPage(SiteRequest request, String pageId)
            throws Exception {
        CoreSession dm = request.getDocumentManager();
        DocumentModel newPage = dm.createDocumentModel(sourceDocument.getPathAsString(), pageId,
                "Note");
        newPage.setProperty("dublincore", "title", pageId);
        //newPage.setProperty("note", "note", "This is new page ${title}");
        newPage.setProperty("note", "note", "");
        newPage = dm.createDocument(newPage);
        dm.save();
        return newPage;
    }

    @Override
    public boolean traverse(SiteRequest request, HttpServletResponse response)
            throws SiteException {

        if (request.getTraversalPath() == null || request.getTraversalPath().isEmpty()) {
            autoSlotId = "main";
        } else {
            autoSlotId = "child" + request.getTraversalPath().size();
        }

        if (request.hasUnresolvedSubPath()) {
            String createFlag = request.getParameter(CREATE_KEY);
            String pageId = request.getUnresolvedPath().get(0);
            if ("true".equalsIgnoreCase(createFlag)) {
                try {
                    DocumentModel newPage = createSubPage(request, pageId);
                    request.getDocsToTraverse().add(newPage);
                    request.getUnresolvedPath().remove(0);
                    request.setMode(SiteRequest.EDIT_MODE);
                } catch (Exception e) {
                    throw new SiteException("Error while creating wiki page", e);
                }
            } else {
                request.setAttribute("pageToCreate", pageId);
                request.setMode(SiteRequest.CREATE_MODE);
            }
        } else if (request.getDocsToTraverse() == null || request.getDocsToTraverse().isEmpty()) {
            DocumentModel indexPage = null;
            try {
                indexPage = getCoreSession().getChild(sourceDocument.getRef(), DEFAULT_PAGE_ID);
                //request.getTraversalPath().add(indexPage.getAdapter(SiteAwareObject.class));
                request.getDocsToTraverse().add(indexPage);
            } catch (ClientException ce) {
                try {
                    indexPage = createSubPage(request, DEFAULT_PAGE_ID);
                    request.getDocsToTraverse().add(indexPage);
                } catch (Exception e) {
                    throw new SiteException("Error while creating wiki page", e);
                }
            }
            return true;
        }

        return true;
    }

    @Override
    public void doGet(SiteRequest request, HttpServletResponse response) {
//        if request.getT

    }

}
