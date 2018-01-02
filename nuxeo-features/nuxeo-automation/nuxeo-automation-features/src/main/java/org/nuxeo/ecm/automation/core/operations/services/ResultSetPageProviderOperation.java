/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.RecordSet;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

/**
 * Operation to execute a query or a named provider with support for Pagination
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.7
 */
@Operation(id = ResultSetPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "QueryAndFetch", description = "Perform "
        + "a query or a named provider query on the repository. Result is "
        + "paginated. The result is returned as a RecordSet (QueryAndFetch) "
        + "rather than as a List of Document"
        + "The query result will become the input for the next "
        + "operation. If no query or provider name is given, a query returning "
        + "all the documents that the user has access to will be executed.", addToStudio = false, aliases = { "Resultset.PageProvider" })
public class ResultSetPageProviderOperation {

    private static final Log log = LogFactory.getLog(ResultSetPageProviderOperation.class);

    public static final String ID = "Repository.ResultSetPageProvider";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    public static final String CMIS = "CMIS";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Context
    protected PageProviderService ppService;

    @Param(name = "providerName", required = false)
    protected String providerName;

    /**
     * @deprecated since 6.0 use instead {@link org.nuxeo.ecm.automation .core.operations.services.query.ResultSetQuery}
     */
    @Deprecated
    @Param(name = "query", required = false)
    protected String query;

    @Param(name = "language", required = false, widget = Constants.W_OPTION, values = { NXQL.NXQL, CMIS })
    protected String lang = NXQL.NXQL;

    @Param(name = "page", required = false)
    protected Integer page;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

    /**
     * @deprecated since 6.0 use instead {@link #sortBy and @link #sortOrder}
     */
    @Deprecated
    @Param(name = "sortInfo", required = false)
    protected StringList sortInfoAsStringList;

    @Param(name = "queryParams", required = false)
    protected StringList strParameters;

    @Param(name = "documentLinkBuilder", required = false)
    protected String documentLinkBuilder;

    /**
     * @since 5.7
     */
    @Param(name = "maxResults", required = false)
    protected String maxResults = "100";

    /**
     * @since 6.0
     */
    @Param(name = PageProviderService.NAMED_PARAMETERS, required = false, description = "Named parameters to pass to the page provider to "
            + "fill in query variables.")
    protected Properties namedParameters;

    /**
     * @since 6.0
     */
    @Param(name = "sortBy", required = false, description = "Sort by " + "properties (separated by comma)")
    protected String sortBy;

    /**
     * @since 6.0
     */
    @Param(name = "sortOrder", required = false, description = "Sort order, " + "ASC or DESC", widget = Constants.W_OPTION, values = {
            ASC, DESC })
    protected String sortOrder;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public RecordSet run() throws OperationException {

        List<SortInfo> sortInfos = null;
        if (sortInfoAsStringList != null) {
            sortInfos = new ArrayList<SortInfo>();
            for (String sortInfoDesc : sortInfoAsStringList) {
                SortInfo sortInfo;
                if (sortInfoDesc.contains("|")) {
                    String[] parts = sortInfoDesc.split("\\|");
                    sortInfo = new SortInfo(parts[0], Boolean.parseBoolean(parts[1]));
                } else {
                    sortInfo = new SortInfo(sortInfoDesc, true);
                }
                sortInfos.add(sortInfo);
            }
        } else {
            // Sort Info Management
            if (!StringUtils.isBlank(sortBy)) {
                sortInfos = new ArrayList<>();
                String[] sorts = sortBy.split(",");
                String[] orders = null;
                if (!StringUtils.isBlank(sortOrder)) {
                    orders = sortOrder.split(",");
                }
                for (int i = 0; i < sorts.length; i++) {
                    String sort = sorts[i];
                    boolean sortAscending = (orders != null && orders.length > i && "asc".equals(orders[i].toLowerCase()));
                    sortInfos.add(new SortInfo(sort, sortAscending));
                }
            }
        }

        Object[] parameters = null;

        if (strParameters != null && !strParameters.isEmpty()) {
            parameters = strParameters.toArray(new String[strParameters.size()]);
            // expand specific parameters
            for (int idx = 0; idx < parameters.length; idx++) {
                String value = (String) parameters[idx];
                if (value.equals(CURRENT_USERID_PATTERN)) {
                    parameters[idx] = session.getPrincipal().getName();
                } else if (value.equals(CURRENT_REPO_PATTERN)) {
                    parameters[idx] = session.getRepositoryName();
                }
            }
        }

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        if (query == null && StringUtils.isEmpty(providerName)) {
            // provide a defaut query
            query = "SELECT * from Document";
        }

        Long targetPage = null;
        if (page != null) {
            targetPage = page.longValue();
        }
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = pageSize.longValue();
        }

        DocumentModel searchDocumentModel = DocumentPageProviderOperation.getSearchDocumentModel(session, ppService,
                providerName, namedParameters);

        final class QueryAndFetchProviderDescriptor extends GenericPageProviderDescriptor {
            private static final long serialVersionUID = 1L;

            public QueryAndFetchProviderDescriptor() {
                super();
                try {
                    klass = (Class<PageProvider<?>>) Class.forName(CoreQueryAndFetchPageProvider.class.getName());
                } catch (ClassNotFoundException e) {
                    log.error(e, e);
                }
            }
        }

        PageProvider<Map<String, Serializable>> pp = null;
        if (query != null) {
            QueryAndFetchProviderDescriptor desc = new QueryAndFetchProviderDescriptor();
            desc.setPattern(query);
            if (maxResults != null && !maxResults.isEmpty() && !maxResults.equals("-1")) {
                // set the maxResults to avoid slowing down queries
                desc.getProperties().put("maxResults", maxResults);
            }
            pp = (CoreQueryAndFetchPageProvider) ppService.getPageProvider("", desc, searchDocumentModel, sortInfos,
                    targetPageSize, targetPage, props, parameters);
        } else {
            pp = (PageProvider<Map<String, Serializable>>) ppService.getPageProvider(providerName, searchDocumentModel,
                    sortInfos, targetPageSize, targetPage, props, parameters);
        }
        PaginableRecordSetImpl res = new PaginableRecordSetImpl(pp);
        if (res.hasError()) {
            throw new OperationException(res.getErrorMessage());
        }
        return res;

    }
}
