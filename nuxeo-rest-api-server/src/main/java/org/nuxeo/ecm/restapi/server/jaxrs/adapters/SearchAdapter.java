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

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;

/**
 *
 *
 * @since 5.7.3
 */
@WebAdapter(name = SearchAdapter.NAME, type = "SearchService")
@Produces({ "application/json+nxentity", MediaType.APPLICATION_JSON })
public class SearchAdapter extends PaginableAdapter {


    @Context
    UriInfo info;

    public static final String NAME = "search";


    private String getParam(String paramName) {
        return info.getQueryParameters().getFirst(paramName);
    }

    private String extractQueryFromRequest() {
        String query = getParam("query");

        if (query == null) {
            String fullText = getParam("fullText");
            if (fullText == null) {
                throw new IllegalParameterException(
                        "Expecting a query or a fullText parameter");
            }
            String orderBy = getParam("orderBy");
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
        String query = extractQueryFromRequest();
        CoreQueryPageProviderDescriptor desc = new CoreQueryPageProviderDescriptor();
        desc.setPattern(query);
        if (maxResults != null && !maxResults.isEmpty()
                && !maxResults.equals("-1")) {
            // set the maxResults to avoid slowing down queries
            desc.getProperties().put("maxResults", maxResults);
        }
        return desc;

    }
}
