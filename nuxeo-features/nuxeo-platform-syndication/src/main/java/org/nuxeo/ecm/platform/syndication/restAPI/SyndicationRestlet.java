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
 *     bchaffangeon
 *
 * $Id: SyndicationRestlet.java 30155 2008-02-13 18:38:48Z troger $
 */

package org.nuxeo.ecm.platform.syndication.restAPI;

import java.util.Calendar;
import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.syndication.serializer.ResultSummary;
import org.nuxeo.ecm.platform.syndication.serializer.SerializerHelper;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * @author bchaffangeon
 *
 */
public class SyndicationRestlet extends BaseStatelessNuxeoRestlet {

    private String syndicationFormat = "RSS";

    @Override
    protected void doHandleStatelessRequest(Request request, Response response) {
        String repo = (String) request.getAttributes().get("repo");
        String docid = (String) request.getAttributes().get("docid");

        initializeSyndicationFormat(request);

        ResultSummary summary = new ResultSummary();
        try {
            super.initRepositoryAndTargetDocument(response, repo, docid);
            DocumentModel currentDocument = session.getDocument(
                    new IdRef(docid));

            summary.setAuthor(((String[]) currentDocument.getProperty(
                    "dublincore", "contributors"))[0]);
            summary.setDescription((String) currentDocument.getProperty(
                    "dublincore", "description"));
            summary.setTitle((String) currentDocument.getProperty(
                    "dublincore", "title"));
            Date modDate = ((Calendar) currentDocument.getProperty(
                    "dublincore", "modified")).getTime();
            summary.setModificationDate(modDate);
            summary.setLink(getRestletFullUrl(request));

            DocumentModelList currentDocumentChildren = new DocumentModelListImpl();
            // currentDocumentChildren.add(currentDocument);
            currentDocumentChildren.addAll(
                    getChildrenDocument(session.getChildren(currentDocument.getRef())));
            SerializerHelper.formatResult(summary, currentDocumentChildren,
                    response, syndicationFormat, null,getHttpRequest(request));
        } catch (Exception e) {
            response.setEntity(e.getMessage(), MediaType.TEXT_PLAIN);
        }
    }

    private DocumentModelList getChildrenDocument(DocumentModelList children)
            throws ClientException {
        DocumentModelList allChildren = new DocumentModelListImpl();
        for (DocumentModel child : children) {
            if (child.getRef() != null) {
                allChildren.add(session.getDocument(child.getRef()));
            }
        }

        return allChildren;
    }

    private void initializeSyndicationFormat(Request request) {
        String format = request.getResourceRef().getSegments().get(4);
        if (format.equals("atom")) {
            syndicationFormat = "ATOM";
        } else if (format.equals("rss")) {
            syndicationFormat = "RSS";
        }
    }

}
