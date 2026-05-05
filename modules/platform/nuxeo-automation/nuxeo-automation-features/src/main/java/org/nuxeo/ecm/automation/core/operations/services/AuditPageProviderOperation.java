/*
 * (C) Copyright 2011-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.query.api.PageProviderSpec.CORE_SESSION_PROPERTY;
import static org.nuxeo.ecm.platform.query.api.PageProviderSpec.CURRENT_REPOSITORY_PARAMETER_VALUE;
import static org.nuxeo.ecm.platform.query.api.PageProviderSpec.CURRENT_USER_PARAMETER_VALUE;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.api.PaginableLogEntryList;
import org.nuxeo.audit.provider.AuditPageProvider;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Paginable;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;

/**
 * Operation to execute a query or a named provider against Audit with support for Pagination
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.8
 */
@Operation(id = AuditPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "Audit Query With Page Provider", description = "Perform "
        + "a query or a named provider query against Audit logs. Result is "
        + "paginated. The query result will become the input for the next "
        + "operation. If no query or provider name is given, a query based on default Audit page provider will be executed.", addToStudio = false, aliases = {
                "Audit.PageProvider" })
public class AuditPageProviderOperation {

    public static final String ID = "Audit.QueryWithPageProvider";

    /**
     * @deprecated since 2025.20, use
     *             {@link org.nuxeo.ecm.platform.query.api.PageProviderSpec#CURRENT_USER_PARAMETER_VALUE} instead
     */
    @Deprecated(since = "2025.20", forRemoval = true)
    public static final String CURRENT_USERID_PATTERN = PageProviderSpec.CURRENT_USER_PARAMETER_VALUE;

    /**
     * @deprecated since 2025.20, use
     *             {@link org.nuxeo.ecm.platform.query.api.PageProviderSpec#CURRENT_REPOSITORY_PARAMETER_VALUE} instead
     */
    @Deprecated(since = "2025.20", forRemoval = true)
    public static final String CURRENT_REPO_PATTERN = PageProviderSpec.CURRENT_REPOSITORY_PARAMETER_VALUE;

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Context
    protected PageProviderService ppService;

    @Param(name = "backendName", required = false)
    protected String backendName;

    @Param(name = "providerName", required = false)
    protected String providerName;

    /** A NXQL query for LogEntry, for example: {@code SELECT * FROM LogEntry} */
    @Param(name = "query", required = false)
    protected String query;

    @Param(name = "language", required = false, widget = Constants.W_OPTION, values = { NXQL.NXQL })
    protected String lang = NXQL.NXQL;

    @Param(name = "currentPageIndex", required = false)
    protected Integer currentPageIndex;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

    @Param(name = "queryParams", required = false)
    protected StringList strParameters;

    @Param(name = "namedQueryParams", required = false)
    protected Properties namedQueryParams;

    /**
     * @since 6.0
     */
    @Param(name = "sortBy", required = false, description = "Sort by " + "properties (separated by comma)")
    protected StringList sortBy;

    /**
     * @since 6.0
     */
    @Param(name = "sortOrder", required = false, description = "Sort order, "
            + "ASC or DESC", widget = Constants.W_OPTION, values = { ASC, DESC })
    protected StringList sortOrder;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public Paginable<LogEntry> run() throws IOException {

        Map<String, Serializable> props = new HashMap<>();
        props.put(CORE_SESSION_PROPERTY, (Serializable) session);
        props.put(AuditPageProvider.BACKEND_NAME_PROPERTY, backendName);

        if (query == null && StringUtils.isEmpty(providerName)) {
            // provide a default provider
            providerName = "AUDIT_BROWSER";
        }

        long targetPage = Objects.requireNonNullElse(currentPageIndex, 0).longValue();
        long targetPageSize = Objects.requireNonNullElse(pageSize, 0).longValue();

        if (query != null) {
            // build and configure the AuditPageProvider directly, bypassing the service: parameter substitution and
            // sortInfos conversion must therefore be performed locally
            Object[] parameters = strParameters == null || strParameters.isEmpty() ? new Object[0]
                    : strParameters.toArray(String[]::new);
            for (int idx = 0; idx < parameters.length; idx++) {
                String value = (String) parameters[idx];
                if (value.equals(CURRENT_USER_PARAMETER_VALUE)) {
                    parameters[idx] = session.getPrincipal().getName();
                } else if (value.equals(CURRENT_REPOSITORY_PARAMETER_VALUE)) {
                    parameters[idx] = session.getRepositoryName();
                }
            }

            AuditPageProvider app = new AuditPageProvider();
            app.setProperties(props);
            GenericPageProviderDescriptor desc = new GenericPageProviderDescriptor();
            desc.setPattern(query);
            app.setParameters(parameters);
            app.setDefinition(desc);
            app.setSortInfos(PageProviderSpec.toSortInfos(sortBy, sortOrder));
            app.setPageSize(targetPageSize);
            app.setCurrentPage(targetPage);
            return new PaginableLogEntryList(app);
        } else {
            DocumentModel searchDoc = null;
            if (namedQueryParams != null && !namedQueryParams.isEmpty()) {
                String docType = ppService.getPageProviderDefinition(providerName).getSearchDocumentType();
                searchDoc = session.createDocumentModel(docType);
                DocumentHelper.setProperties(session, searchDoc, namedQueryParams);
            }

            PageProvider<LogEntry> pp = (PageProvider<LogEntry>) ppService.getPageProvider(
                    PageProviderSpec.builder(providerName)
                                    .searchDocument(searchDoc)
                                    .sortInfosByFieldsAndOrders(sortBy, sortOrder)
                                    .pageSize(targetPageSize)
                                    .currentPage(targetPage)
                                    .properties(props)
                                    .parameters(strParameters)
                                    .build());
            return new PaginableLogEntryList(pp);
        }
    }
}
