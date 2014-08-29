/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.services.query;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.operations.services.PaginableRecordSetImpl;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.RecordSet;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.PageProviderServiceImpl;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 5.9.6
 * Result set query operation to perform queries on the repository.
 */
@Operation(id = ResultSetQuery.ID, category = Constants.CAT_FETCH,
        label = "ResultSet Query", description = "Perform a query on the " +
        "repository. The result set returned will become the input for the " +
        "next operation.", since = "5.9.6", addToStudio = true)
public class ResultSetQuery {

    public static final String ID = "ResultSet.Query";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    @Context
    protected CoreSession session;

    @Context
    protected PageProviderService pageProviderService;

    @Param(name = "query", required = true, description = "The query to " +
            "perform.")
    protected String query;

    @Param(name = "language", required = false, description = "The query " +
            "language.", widget = Constants.W_OPTION,
            values = { NXQL.NXQL, "CMISQL", "ESQL" })
    protected String lang = NXQL.NXQL;

    @Param(name = PageProviderServiceImpl.NAMED_PARAMETERS, required = false,
            description = "Named parameters to pass to the page provider to " +
                    "fill in query variables.")
    protected Properties namedParameters;

    @Param(name = "currentPageIndex", required = false,
            description = "Target listing page.")
    protected Integer currentPageIndex;

    @Param(name = "pageSize", required = false, description = "Entries number" +
            " per page.")
    protected Integer pageSize;

    @Param(name = "queryParams", required = false, description = "Ordered " +
            "query parameters.")
    protected StringList strParameters;

    @Param(name = "sortBy", required = false, description = "Sort by " +
            "properties (separated by comma)")
    protected String sortBy;

    @Param(name = "sortOrder", required = false, description = "Sort order, " +
            "ASC or DESC", widget = Constants.W_OPTION,
            values = { ASC, DESC })

    protected String sortOrder;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public RecordSet run() throws Exception {
        // Ordered parameters
        Object[] orderedParameters = null;
        if (strParameters != null && !strParameters.isEmpty()) {
            orderedParameters = strParameters.toArray(new
                    String[strParameters.size
                    ()]);
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
                boolean sortAscending = (orders != null && orders.length
                        > i && "asc".equals(orders[i].toLowerCase()));
                sortInfoList.add(new SortInfo(sort, sortAscending));
            }
        }

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        SimpleDocumentModel searchDocumentModel = null;
        if (namedParameters != null && !namedParameters.isEmpty()) {
            searchDocumentModel = new SimpleDocumentModel();
            searchDocumentModel.putContextData(PageProviderServiceImpl
                            .NAMED_PARAMETERS,
                    namedParameters);
        }
        QueryAndFetchProviderDescriptor desc = new
                QueryAndFetchProviderDescriptor();
        desc.setPattern(query);
        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map
                <String, Serializable>>) pageProviderService.getPageProvider
                (StringUtils
                                .EMPTY, desc,
                        searchDocumentModel, sortInfoList, targetPageSize,
                        targetPage, props, orderedParameters);
        return new PaginableRecordSetImpl(pp);
    }

    @SuppressWarnings("unchecked")
    final class QueryAndFetchProviderDescriptor extends
            GenericPageProviderDescriptor {
        private static final long serialVersionUID = 1L;

        public QueryAndFetchProviderDescriptor() {
            super();
            try {
                this.klass = (Class<PageProvider<?>>) Class.forName
                        (CoreQueryAndFetchPageProvider.class.getName());
            } catch (ClassNotFoundException e) {

            }
        }
    }

}
