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
 *     Marwane Kalam-Alami
 */
package org.nuxeo.ecm.automation.core.operations.services;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.ecm.automation.core.util.PageProviderHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;

/**
 * Operation to execute a query or a named provider with support for Pagination.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
@Operation(id = DocumentPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "PageProvider", description = "Perform "
        + "a named provider query on the repository. Result is paginated. The query result will become the input for "
        + "the next operation.", aliases = { "Document.PageProvider" })
public class DocumentPageProviderOperation {

    public static final String ID = "Repository.PageProvider";

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "providerName", required = true)
    protected String providerName;

    @Param(name = "language", required = false, widget = Constants.W_OPTION, values = { NXQL.NXQL })
    protected String lang = NXQL.NXQL;

    @Param(name = "currentPageIndex", alias = "page", required = false)
    protected Integer currentPageIndex;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

    /*
     * @since 11.4
     */
    @Param(name = "offset", required = false)
    protected Integer offset;

    @Param(name = "queryParams", alias = "searchTerm", required = false)
    protected StringList strParameters;

    @Param(name = "documentLinkBuilder", required = false)
    protected String documentLinkBuilder;

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
    protected StringList sortBy;

    /**
     * @since 6.0
     */
    @Param(name = "sortOrder", required = false, description = "Sort order, "
            + "ASC or DESC", widget = Constants.W_OPTION, values = { ASC, DESC })
    protected StringList sortOrder;

    /**
     * @since 8.4
     */
    @Param(name = "quickFilters", required = false, description = "Quick filter " + "properties (separated by comma)")
    protected StringList quickFilters;

    /**
     * @since 9.1
     */
    @Param(name = "highlights", required = false, description = "Highlight properties (separated by comma)")
    protected StringList highlights;

    @SuppressWarnings("unchecked")
    @OperationMethod
    public PaginableDocumentModelListImpl run() throws OperationException {
        PageProviderDefinition def = PageProviderHelper.getPageProviderDefinition(providerName);

        Long targetPage = currentPageIndex != null ? currentPageIndex.longValue() : null;
        Long targetPageSize = pageSize != null ? pageSize.longValue() : null;
        Long currentOffset = offset != null ? offset.longValue() : null;

        Object[] parameters = strParameters != null ? strParameters.toArray(new String[0]) : null;

        String ppMethod = Framework.getProperty("org.nuxeo.web.ui.pageprovider.method");
        if (strParameters != null && ppMethod != null && "post".equals(ppMethod.toLowerCase())) {
            // Dirty hack to make NXP-29126 work with nxql search
            int count = StringUtils.countMatches(def.getPattern(), '?');
            if (count == 1 && strParameters.size() > 1) {
                // pattern has a single '?' but there are many parameters
                // let's join them to form the original NXQL query that contained commas
                parameters = new Object[] { String.join(",", strParameters.toArray(new String[0])) };
            }
        }

        ActionContext actionContext = (ActionContext) context.get(GetActions.SEAM_ACTION_CONTEXT);
        if (actionContext != null) {
            parameters = PageProviderHelper.resolveELParameters(def, parameters);
        }

        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) PageProviderHelper.getPageProvider(session, def,
                namedParameters, sortBy, sortOrder, targetPageSize, targetPage, currentOffset, highlights, quickFilters,
                parameters);

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        if (request != null) {
            String skipAggregates = request.getHeader(PageProvider.SKIP_AGGREGATES_PROP);
            if (skipAggregates != null) {
                Map<String, Serializable> props = pp.getProperties();
                props.put(PageProvider.SKIP_AGGREGATES_PROP,
                        Boolean.parseBoolean(request.getHeader(PageProvider.SKIP_AGGREGATES_PROP)));
                pp.setProperties(props);
            }
        }

        PaginableDocumentModelListImpl res = new PaginableDocumentModelListImpl(pp, documentLinkBuilder);

        if (res.hasError()) {
            throw new OperationException(res.getErrorMessage());
        }
        return res;
    }
}
