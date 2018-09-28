/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.server.jaxrs.adapters;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@WebAdapter(name = SearchAdapter.NAME, type = "SearchService")
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON + "+nxentity",
        MediaType.APPLICATION_JSON + "+esentity" })
public class SearchAdapter extends DocumentModelListPaginableAdapter {

    public static final String NAME = "search";

    public static final String pageProviderName = "REST_API_SEARCH_ADAPTER";

    private String extractQueryFromRequest(final HttpServletRequest request) {
        String query = request.getParameter("query");
        if (query == null) {
            String fullText = request.getParameter("fullText");
            if (fullText == null) {
                throw new IllegalParameterException("Expecting a query or a fullText parameter");
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
            query = "SELECT * FROM Document WHERE (ecm:fulltext = \"" + fullText
                    + "\") AND (ecm:isVersion = 0) AND (ecm:path STARTSWITH \"" + path + "\")" + orderClause;
        }
        return query;
    }

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {

        String query = extractQueryFromRequest(ctx.getRequest());
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        PageProviderDefinition ppdefinition = ppService.getPageProviderDefinition(pageProviderName);
        ppdefinition.setPattern(query);

        if (maxResults != null && !maxResults.isEmpty() && !maxResults.equals("-1")) {
            // set the maxResults to avoid slowing down queries
            ppdefinition.getProperties().put("maxResults", maxResults);
        }
        return ppdefinition;
    }
}
