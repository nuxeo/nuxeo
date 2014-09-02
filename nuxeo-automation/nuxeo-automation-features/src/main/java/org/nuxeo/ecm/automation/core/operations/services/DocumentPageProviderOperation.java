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

import org.jboss.el.lang.FunctionMapperImpl;
import org.jboss.seam.el.EL;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.jaxrs.io.documents
        .PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.actions.seam.SeamActionContext;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

import javax.el.ELContext;
import javax.el.ValueExpression;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Operation to execute a query or a named provider with support for Pagination
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
@Operation(id = DocumentPageProviderOperation.ID, category = Constants
        .CAT_FETCH, label = "PageProvider", description = "Perform "
        + "a query or a named provider query on the repository. Result is "
        + "paginated. The query result will become the input for the next "
        + "operation. If no query or provider name is given, a query returning "
        + "all the documents that the user has access to will be executed.")
public class DocumentPageProviderOperation {

    public static final String ID = "Document.PageProvider";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    private static final String SORT_PARAMETER_SEPARATOR = " ";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Context
    protected PageProviderService ppService;

    @Param(name = "providerName", required = false)
    protected String providerName;

    @Param(name = "query", required = false)
    protected String query;

    @Param(name = "language", required = false, widget = Constants.W_OPTION,
            values = { NXQL.NXQL })
    protected String lang = NXQL.NXQL;

    @Param(name = "page", required = false)
    @Deprecated
    protected Integer page;

    @Param(name = "currentPageIndex", required = false)
    protected Integer currentPageIndex;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

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

    @SuppressWarnings("unchecked")
    @OperationMethod
    public PaginableDocumentModelListImpl run() throws Exception {

        PageProviderService pps = Framework.getLocalService
                (PageProviderService.class);

        List<SortInfo> sortInfos = null;
        if (sortInfoAsStringList != null) {
            sortInfos = new ArrayList<SortInfo>();
            for (String sortInfoDesc : sortInfoAsStringList) {
                SortInfo sortInfo;
                if (sortInfoDesc.contains(SORT_PARAMETER_SEPARATOR)) {
                    String[] parts = sortInfoDesc.split
                            (SORT_PARAMETER_SEPARATOR);
                    sortInfo = new SortInfo(parts[0],
                            Boolean.parseBoolean(parts[1]));
                } else {
                    sortInfo = new SortInfo(sortInfoDesc, true);
                }
                sortInfos.add(sortInfo);
            }
        }

        Object[] parameters = null;

        if (strParameters != null && !strParameters.isEmpty()) {
            parameters = strParameters.toArray(new String[strParameters.size
                    ()]);
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
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);

        if (query == null
                && (providerName == null || providerName.length() == 0)) {
            // provide a defaut query
            query = "SELECT * from Document";
        }

        Long targetPage = null;
        if (page != null) {
            targetPage = Long.valueOf(page.longValue());
        }
        if (currentPageIndex != null) {
            targetPage = currentPageIndex.longValue();
        }
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = Long.valueOf(pageSize.longValue());
        }

        if (query != null) {
            CoreQueryPageProviderDescriptor desc = new
                    CoreQueryPageProviderDescriptor();
            desc.setPattern(query);
            if (maxResults != null && !maxResults.isEmpty()
                    && !maxResults.equals("-1")) {
                // set the maxResults to avoid slowing down queries
                desc.getProperties().put("maxResults", maxResults);
            }
            return new PaginableDocumentModelListImpl(
                    (PageProvider<DocumentModel>) pps.getPageProvider("", desc,
                            null, sortInfos, targetPageSize, targetPage, props,
                            parameters), documentLinkBuilder);
        } else {
            return new PaginableDocumentModelListImpl(
                    (PageProvider<DocumentModel>) pps.getPageProvider(
                            providerName,
                            sortInfos,
                            targetPageSize,
                            targetPage,
                            props,
                            context.containsKey("seamActionContext") ?
                                    getParameters(
                                    providerName, parameters) : parameters),
                    documentLinkBuilder);
        }

    }

    /**
     * Resolves additional parameters that could have been defined in the
     * contribution.
     *
     * @param pageProviderName name of the Page Provider
     * @param givenParameters  parameters from the operation
     * @since 5.8
     */
    private Object[] getParameters(final String pageProviderName,
            final Object[] givenParameters) {
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        // resolve additional parameters
        PageProviderDefinition ppDef = ppService.getPageProviderDefinition
                (pageProviderName);
        String[] params = ppDef.getQueryParameters();
        if (params == null) {
            params = new String[0];
        }

        Object[] resolvedParams = new Object[params.length
                + (givenParameters != null ? givenParameters.length : 0)];

        ELContext elContext = EL.createELContext(SeamActionContext.EL_RESOLVER,
                new FunctionMapperImpl());

        int i = 0;
        if (givenParameters != null) {
            i = givenParameters.length;
            System.arraycopy(givenParameters, 0, resolvedParams, 0, i);
        }
        for (int j = 0; j < params.length; j++) {
            ValueExpression ve = SeamActionContext.EXPRESSION_FACTORY
                    .createValueExpression(
                            elContext, params[j], Object.class);
            resolvedParams[i + j] = ve.getValue(elContext);
        }
        return resolvedParams;
    }
}
