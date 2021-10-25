/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.services.query;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.PageProviderHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

import java.util.Collections;
import java.util.Map;

/**
 * @since 6.0 Document query operation to perform queries on the repository.
 */
@Operation(id = DocumentPaginatedQuery.ID, category = Constants.CAT_FETCH, label = "Query", description = "Perform a query on the repository. "
        + "The document list returned will become the input for the next operation."
        + "If no provider name is given, a query returning all the documents that the user has access to will be executed.",
        since = "6.0", addToStudio = true, aliases = { "Document.Query" })
public class DocumentPaginatedQuery {

    public static final String ID = "Repository.Query";

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    @Context
    protected CoreSession session;

    @Param(name = "query", required = false, description = "The query to perform.")
    protected String query;

    @Param(name = "language", required = false, description = "The query language.",
            widget = Constants.W_OPTION, values = { NXQL.NXQL })
    protected String lang = NXQL.NXQL;

    @Param(name = "currentPageIndex", alias = "page", required = false, description = "Target listing page.")
    protected Integer currentPageIndex;

    @Param(name = "pageSize", required = false, description = "Entries number per page.")
    protected Integer pageSize;

    @Param(name = "queryParams", alias = "searchTerm", required = false, description = "Ordered query parameters.")
    protected StringList strParameters;

    @Param(name = "sortBy", required = false, description = "Sort by properties (separated by comma)")
    protected StringList sortBy;

    @Param(name = "sortOrder", required = false, description = "Sort order, ASC or DESC",
            widget = Constants.W_OPTION, values = { ASC, DESC })
    protected StringList sortOrder;

    @Param(name = PageProviderService.NAMED_PARAMETERS, required = false,
            description = "Named parameters to pass to the page provider to fill in query variables.")
    protected Properties namedParameters;

    /**
     * @since 10.3
     */
    @Param(name = "maxResults", required = false)
    protected Integer maxResults;

    /**
     * @since 11,1
     */
    @Param(name = "quotePatternParameters", required = false)
    protected Boolean quotePatternParameters = true;

    /**
     * @since 11.1
     */
    @Param(name = "escapePatternParameters", required = false)
    protected Boolean escapePatternParameters = true;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public DocumentModelList run() throws OperationException {
        if (query == null) {
            // provide a default query
            query = "SELECT * from Document";
        }
        Map<String, String> properties = null;
        if (maxResults != null) {
            properties = Collections.singletonMap("maxResults", maxResults.toString());
        }
        PageProviderDefinition def = PageProviderHelper.getQueryPageProviderDefinition(query, properties,
                escapePatternParameters, quotePatternParameters);

        Long targetPage = currentPageIndex != null ? currentPageIndex.longValue() : null;
        Long targetPageSize = pageSize != null ? pageSize.longValue() : null;

        // NXP-29097: handle empty "searchTerm" parameters in query (referenced by ? character)
        // test if the query does not wait for any parameter, then it probably contains the searchTerm set to
        // "", let's nullify "strParameters" variable
        if (query.indexOf("?") == -1 && strParameters != null && strParameters.size() == 1
                && Framework.getService(ConfigurationService.class)
                            .isBooleanTrue("org.nuxeo.ignore.empty.searchterm")) {
            strParameters = null;
        }

        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) PageProviderHelper.getPageProvider(session, def,
                namedParameters, sortBy, sortOrder, targetPageSize, targetPage,
                strParameters != null ? strParameters.toArray(new String[0]) : null);

        PaginableDocumentModelListImpl res = new PaginableDocumentModelListImpl(pp);
        if (res.hasError()) {
            throw new OperationException(res.getErrorMessage());
        }
        return res;
    }
}
