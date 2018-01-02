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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

/**
 * @since 6.0 Document query operation to perform queries on the repository.
 */
@Operation(id = DocumentPaginatedQuery.ID, category = Constants.CAT_FETCH, label = "Query", description = "Perform a query on the repository. "
        + "The document list returned will become the input for the next "
        + "operation.", since = "6.0", addToStudio = true, aliases = { "Document.Query" })
public class DocumentPaginatedQuery {

    public static final String ID = "Repository.Query";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    @Context
    protected CoreSession session;

    @Context
    protected PageProviderService pageProviderService;

    @Param(name = "query", required = true, description = "The query to " + "perform.")
    protected String query;

    @Param(name = "language", required = false, description = "The query "
            + "language.", widget = Constants.W_OPTION, values = { NXQL.NXQL })
    protected String lang = NXQL.NXQL;

    @Param(name = "currentPageIndex", required = false, description = "Target listing page.")
    protected Integer currentPageIndex;

    @Param(name = "pageSize", required = false, description = "Entries number" + " per page.")
    protected Integer pageSize;

    @Param(name = "queryParams", required = false, description = "Ordered " + "query parameters.")
    protected StringList strParameters;

    @Param(name = "sortBy", required = false, description = "Sort by " + "properties (separated by comma)")
    protected String sortBy;

    @Param(name = "sortOrder", required = false, description = "Sort order, "
            + "ASC or DESC", widget = Constants.W_OPTION, values = { ASC, DESC })
    protected String sortOrder;

    @Param(name = PageProviderService.NAMED_PARAMETERS, required = false, description = "Named parameters to pass to the page provider to "
            + "fill in query variables.")
    protected Properties namedParameters;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public DocumentModelList run() throws OperationException {
        // Ordered parameters
        Object[] orderedParameters = null;
        if (strParameters != null && !strParameters.isEmpty()) {
            orderedParameters = strParameters.toArray(new String[strParameters.size()]);
            // expand specific parameters
            for (int idx = 0; idx < orderedParameters.length; idx++) {
                String value = (String) orderedParameters[idx];
                if (value.equals(CURRENT_USERID_PATTERN)) {
                    orderedParameters[idx] = session.getPrincipal().getName();
                } else if (value.equals(CURRENT_REPO_PATTERN)) {
                    orderedParameters[idx] = session.getRepositoryName();
                }
            }
        }
        // Target query page
        Long targetPage = null;
        if (currentPageIndex != null) {
            targetPage = currentPageIndex.longValue();
        }
        // Target page size
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = pageSize.longValue();
        }

        // Sort Info Management
        List<SortInfo> sortInfoList = new ArrayList<>();
        if (!StringUtils.isBlank(sortBy)) {
            String[] sorts = sortBy.split(",");
            String[] orders = null;
            if (!StringUtils.isBlank(sortOrder)) {
                orders = sortOrder.split(",");
            }
            for (int i = 0; i < sorts.length; i++) {
                String sort = sorts[i];
                boolean sortAscending = (orders != null && orders.length > i && "asc".equals(orders[i].toLowerCase()));
                sortInfoList.add(new SortInfo(sort, sortAscending));
            }
        }

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        DocumentModel searchDocumentModel = getSearchDocumentModel(session, namedParameters);
        CoreQueryPageProviderDescriptor desc = new CoreQueryPageProviderDescriptor();
        desc.setPattern(query);
        PaginableDocumentModelListImpl res = new PaginableDocumentModelListImpl(
                (PageProvider<DocumentModel>) pageProviderService.getPageProvider(StringUtils.EMPTY, desc,
                        searchDocumentModel, sortInfoList, targetPageSize, targetPage, props, orderedParameters),
                null);
        if (res.hasError()) {
            throw new OperationException(res.getErrorMessage());
        }
        return res;

    }

    /**
     * @since 8.2
     */
    public static DocumentModel getSearchDocumentModel(CoreSession session, Properties namedParameters) {
        SimpleDocumentModel searchDocumentModel = new SimpleDocumentModel();
        if (namedParameters != null && !namedParameters.isEmpty()) {
            for (Map.Entry<String, String> entry : namedParameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                try {
                    DocumentHelper.setProperty(session, searchDocumentModel, key, value, true);
                } catch (PropertyNotFoundException | IOException e) {
                    // assume this is a "pure" named parameter, not part of the search doc schema
                    continue;
                }
            }
            searchDocumentModel.putContextData(PageProviderService.NAMED_PARAMETERS, namedParameters);
        }
        return searchDocumentModel;
    }
}
