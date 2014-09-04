/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 *
 *
 * @since 5.7.3
 */
@WebAdapter(name = SearchAdapter.NAME, type = "SearchService")
@Produces({ "application/json+nxentity", "application/json+esentity", MediaType.APPLICATION_JSON })
public class SearchAdapter extends DocumentModelListPaginableAdapter {

    public static final String NAME = "search";

    public static final  String pageProviderName = "REST_API_SEARCH_ADAPTER";

    private String extractQueryFromRequest(final HttpServletRequest request) {
        String query = request.getParameter("query");
        if (query == null) {
            String fullText = request.getParameter("fullText");
            if (fullText == null) {
                throw new IllegalParameterException(
                        "Expecting a query or a fullText parameter");
            }
            String orderBy = request.getParameter("orderBy");
            String orderClause = "";
            if (orderBy != null) {
                orderClause = " ORDER BY " + orderBy;
            }
            String path;

            DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
            if (doc.isFolder()) {
                path = doc.getPathAsString();
            } else {
                path = doc.getPath().removeLastSegments(1).toString();
            }
            query = "SELECT * FROM Document WHERE (ecm:fulltext = \""
                    + fullText
                    + "\") AND (ecm:isCheckedInVersion = 0) AND (ecm:path STARTSWITH \""
                    + path + "\")" + orderClause;
        }
        return query;
    }

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {

        String query = extractQueryFromRequest(ctx.getRequest());
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        PageProviderDefinition ppdefinition = ppService.getPageProviderDefinition(pageProviderName);
        ppdefinition.setPattern(query);

        if (maxResults != null && !maxResults.isEmpty()
                && !maxResults.equals("-1")) {
            // set the maxResults to avoid slowing down queries
            ppdefinition.getProperties().put("maxResults", maxResults);
        }
        return ppdefinition;
    }
}

