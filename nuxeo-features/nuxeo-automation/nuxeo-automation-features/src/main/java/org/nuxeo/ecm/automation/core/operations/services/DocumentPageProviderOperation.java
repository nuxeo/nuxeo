/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 *     Marwane Kalam-Alami
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ValueExpression;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.lang.FunctionMapperImpl;
import org.jboss.seam.el.EL;
import org.nuxeo.ecm.automation.OperationContext;
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
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.actions.seam.SeamActionContext;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;

/**
 * Operation to execute a query or a named provider with support for Pagination.
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
@Operation(id = DocumentPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "PageProvider", description = "Perform "
        + "a query or a named provider query on the repository. Result is "
        + "paginated. The query result will become the input for the next "
        + "operation. If no query or provider name is given, a query returning "
        + "all the documents that the user has access to will be executed.", aliases = { "Document.PageProvider" })
public class DocumentPageProviderOperation {

    public static final String ID = "Repository.PageProvider";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    private static final String SORT_PARAMETER_SEPARATOR = " ";

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    private static final Log log = LogFactory.getLog(DocumentPageProviderOperation.class);

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Context
    protected PageProviderService ppService;

    @Param(name = "providerName", required = false)
    protected String providerName;

    /**
     * @deprecated since 6.0 use instead {@link org.nuxeo.ecm.automation .core.operations.services.query.DocumentQuery}.
     */
    @Deprecated
    @Param(name = "query", required = false)
    protected String query;

    @Param(name = "language", required = false, widget = Constants.W_OPTION, values = { NXQL.NXQL })
    protected String lang = NXQL.NXQL;

    @Param(name = "page", required = false)
    @Deprecated
    protected Integer page;

    @Param(name = "currentPageIndex", required = false)
    protected Integer currentPageIndex;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

    /**
     * @deprecated since 6.0 use instead {@link #sortBy and @link #sortOrder}.
     */
    @Deprecated
    @Param(name = "sortInfo", required = false)
    protected StringList sortInfoAsStringList;

    @Param(name = "queryParams", alias = "searchTerm", required = false)
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
    public PaginableDocumentModelListImpl run() throws OperationException {
        List<SortInfo> sortInfos = null;
        if (sortInfoAsStringList != null) {
            // BBB
            sortInfos = new ArrayList<SortInfo>();
            for (String sortInfoDesc : sortInfoAsStringList) {
                SortInfo sortInfo;
                if (sortInfoDesc.contains(SORT_PARAMETER_SEPARATOR)) {
                    String[] parts = sortInfoDesc.split(SORT_PARAMETER_SEPARATOR);
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

        if (query == null && StringUtils.isBlank(providerName)) {
            // provide a defaut query
            query = "SELECT * from Document";
        }

        Long targetPage = null;
        if (page != null) {
            targetPage = page.longValue();
        }
        if (currentPageIndex != null) {
            targetPage = currentPageIndex.longValue();
        }
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = pageSize.longValue();
        }

        DocumentModel searchDocumentModel = getSearchDocumentModel(session, ppService, providerName, namedParameters);

        PaginableDocumentModelListImpl res;
        if (query != null) {
            CoreQueryPageProviderDescriptor desc = new CoreQueryPageProviderDescriptor();
            desc.setPattern(query);
            if (maxResults != null && !maxResults.isEmpty() && !maxResults.equals("-1")) {
                // set the maxResults to avoid slowing down queries
                desc.getProperties().put("maxResults", maxResults);
            }
            PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider("", desc,
                    searchDocumentModel, sortInfos, targetPageSize, targetPage, props, parameters);
            res = new PaginableDocumentModelListImpl(pp, documentLinkBuilder);
        } else {
            PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) ppService.getPageProvider(providerName,
                    searchDocumentModel, sortInfos, targetPageSize, targetPage, props,
                    context.containsKey("seamActionContext") ? getParameters(providerName, parameters) : parameters);
            res = new PaginableDocumentModelListImpl(pp, documentLinkBuilder);
        }
        if (res.hasError()) {
            throw new OperationException(res.getErrorMessage());
        }
        return res;
    }

    /**
     * Resolves additional parameters that could have been defined in the contribution.
     *
     * @param pageProviderName name of the Page Provider
     * @param givenParameters parameters from the operation
     * @since 5.8
     */
    private Object[] getParameters(final String pageProviderName, final Object[] givenParameters) {
        // resolve additional parameters
        PageProviderDefinition ppDef = ppService.getPageProviderDefinition(pageProviderName);
        String[] params = ppDef.getQueryParameters();
        if (params == null) {
            params = new String[0];
        }

        Object[] resolvedParams = new Object[params.length + (givenParameters != null ? givenParameters.length : 0)];

        ELContext elContext = EL.createELContext(SeamActionContext.EL_RESOLVER, new FunctionMapperImpl());

        int i = 0;
        if (givenParameters != null) {
            i = givenParameters.length;
            System.arraycopy(givenParameters, 0, resolvedParams, 0, i);
        }
        for (int j = 0; j < params.length; j++) {
            ValueExpression ve = SeamActionContext.EXPRESSION_FACTORY.createValueExpression(elContext, params[j],
                    Object.class);
            resolvedParams[i + j] = ve.getValue(elContext);
        }
        return resolvedParams;
    }

    /**
     * @since 7.1
     */
    public static DocumentModel getSearchDocumentModel(CoreSession session, PageProviderService pps,
            String providerName, Properties namedParameters) {
        // generate search document model if type specified on the definition
        DocumentModel searchDocumentModel = null;
        if (!StringUtils.isBlank(providerName)) {
            PageProviderDefinition pageProviderDefinition = pps.getPageProviderDefinition(providerName);
            if (pageProviderDefinition != null) {
                String searchDocType = pageProviderDefinition.getSearchDocumentType();
                if (searchDocType != null) {
                    searchDocumentModel = session.createDocumentModel(searchDocType);
                } else if (pageProviderDefinition.getWhereClause() != null) {
                    // avoid later error on null search doc, in case where clause is only referring to named parameters
                    // (and no namedParameters are given)
                    searchDocumentModel = new SimpleDocumentModel();
                }
            } else {
                log.error("No page provider definition found for " + providerName);
            }
        }

        if (namedParameters != null && !namedParameters.isEmpty()) {
            // fall back on simple document if no type defined on page provider
            if (searchDocumentModel == null) {
                searchDocumentModel = new SimpleDocumentModel();
            }
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