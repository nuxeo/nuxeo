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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.PageProviderHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.RecordSet;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;

/**
 * Operation to execute a query or a named provider with support for Pagination
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.7
 */
@Operation(id = ResultSetPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "QueryAndFetch", description = "Perform "
        + "a named provider query on the repository. Result is paginated."
        + "The result is returned as a RecordSet (QueryAndFetch) rather than as a List of Document"
        + "The query result will become the input for the next operation.", addToStudio = false, aliases = {
                "Resultset.PageProvider" })
public class ResultSetPageProviderOperation {

    public static final String ID = "Repository.ResultSetPageProvider";

    public static final String DESC = "DESC";

    public static final String ASC = "ASC";

    public static final String CMIS = "CMIS";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "providerName", required = false)
    protected String providerName;

    @Param(name = "language", required = false, widget = Constants.W_OPTION, values = { NXQL.NXQL, CMIS })
    protected String lang = NXQL.NXQL;

    @Param(name = "currentPageIndex", alias = "page", required = false)
    protected Integer page;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

    @Param(name = "queryParams", required = false)
    protected StringList strParameters;

    /**
     * @since 5.7
     */
    @Param(name = "maxResults", required = false)
    protected String maxResults = "100";

    /**
     * @since 6.0
     */
    @Param(name = PageProviderService.NAMED_PARAMETERS, required = false,
            description = "Named parameters to pass to the page provider to fill in query variables.")
    protected Properties namedParameters;

    /**
     * @since 6.0
     */
    @Param(name = "sortBy", required = false, description = "Sort by properties (separated by comma)")
    protected StringList sortBy;

    /**
     * @since 6.0
     */
    @Param(name = "sortOrder", required = false, description = "Sort order, ASC or DESC",
            widget = Constants.W_OPTION, values = { ASC, DESC })
    protected StringList sortOrder;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public RecordSet run() throws OperationException {
        PageProviderDefinition def = PageProviderHelper.getPageProviderDefinition(providerName);

        Long targetPage = page != null ? page.longValue() : null;
        Long targetPageSize = pageSize != null ? pageSize.longValue() : null;

        PageProvider<Map<String, Serializable>> pp = (PageProvider<Map<String, Serializable>>) PageProviderHelper.getPageProvider(
                session, def, namedParameters, sortBy, sortOrder, targetPageSize, targetPage,
                strParameters != null ? strParameters.toArray(new String[0]) : null);

        PaginableRecordSetImpl res = new PaginableRecordSetImpl(pp);
        if (res.hasError()) {
            throw new OperationException(res.getErrorMessage());
        }
        return res;
    }
}
