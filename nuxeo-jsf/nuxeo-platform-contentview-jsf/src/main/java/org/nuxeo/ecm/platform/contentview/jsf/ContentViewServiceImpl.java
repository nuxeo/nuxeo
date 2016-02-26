/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.ReferencePageProviderDescriptor;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class ContentViewServiceImpl extends DefaultComponent implements ContentViewService {

    public static final String CONTENT_VIEW_EP = "contentViews";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ContentViewServiceImpl.class);

    protected ContentViewRegistry contentViewReg = new ContentViewRegistry();

    @Override
    public ContentView getContentView(String name) {
        ContentViewDescriptor desc = contentViewReg.getContentView(name);
        if (desc == null) {
            return null;
        }
        Boolean useGlobalPageSize = desc.getUseGlobalPageSize();
        if (useGlobalPageSize == null) {
            useGlobalPageSize = Boolean.FALSE;
        }
        Boolean translateTitle = desc.getTranslateTitle();
        if (translateTitle == null) {
            translateTitle = Boolean.FALSE;
        }
        Boolean translateEmptySentence = desc.getTranslateEmptySentence();
        if (translateEmptySentence == null) {
            translateEmptySentence = Boolean.FALSE;
        }
        Boolean showTitle = desc.getShowTitle();
        if (showTitle == null) {
            showTitle = Boolean.FALSE;
        }
        Boolean showPageSizeSelector = desc.getShowPageSizeSelector();
        if (showPageSizeSelector == null) {
            showPageSizeSelector = Boolean.FALSE;
        }
        Boolean showRefreshPage = desc.getShowRefreshCommand();
        if (showRefreshPage == null) {
            showRefreshPage = Boolean.TRUE;
        }
        Boolean showFilterForm = desc.getShowFilterForm();
        if (showFilterForm == null) {
            showFilterForm = Boolean.FALSE;
        }

        String[] queryParams = null;
        String searchDocumentType = null;
        String sortInfosBinding = null;
        String pageSizeBinding = null;
        CoreQueryPageProviderDescriptor coreDesc = desc.getCoreQueryPageProvider();
        GenericPageProviderDescriptor genDesc = desc.getGenericPageProvider();
        ReferencePageProviderDescriptor refDesc = desc.getReferencePageProvider();
        String[] refQueryParams = null;
        if (refDesc != null && refDesc.isEnabled()) {
            PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
            PageProviderDefinition def = ppService.getPageProviderDefinition(refDesc.getName());
            if (def == null) {
                log.error("Could not resolve page provider with name " + refDesc.getName());
            } else if (def instanceof CoreQueryPageProviderDescriptor) {
                coreDesc = (CoreQueryPageProviderDescriptor) def;
                refQueryParams = refDesc.getQueryParameters();
            } else if (def instanceof GenericPageProviderDescriptor) {
                genDesc = (GenericPageProviderDescriptor) def;
                refQueryParams = refDesc.getQueryParameters();
            }
        }
        if (coreDesc != null && coreDesc.isEnabled()) {
            queryParams = coreDesc.getQueryParameters();
            sortInfosBinding = coreDesc.getSortInfosBinding();
            pageSizeBinding = coreDesc.getPageSizeBinding();
            searchDocumentType = coreDesc.getSearchDocumentType();
        } else if (genDesc != null && genDesc.isEnabled()) {
            queryParams = genDesc.getQueryParameters();
            sortInfosBinding = genDesc.getSortInfosBinding();
            pageSizeBinding = genDesc.getPageSizeBinding();
            searchDocumentType = genDesc.getSearchDocumentType();
        }
        List<String> allQueryParams = new ArrayList<String>();
        if (queryParams != null) {
            allQueryParams.addAll(Arrays.asList(queryParams));
        }
        if (refQueryParams != null) {
            allQueryParams.addAll(Arrays.asList(refQueryParams));
        }
        String searchDocBinding = desc.getSearchDocumentBinding();
        ContentViewImpl contentView = new ContentViewImpl(name, desc.getTitle(), translateTitle.booleanValue(),
                desc.getIconPath(), desc.getSelectionListName(), desc.getPagination(), desc.getActionCategories(),
                desc.getSearchLayout(), desc.getResultLayouts(), desc.getFlags(), desc.getCacheKey(),
                desc.getCacheSize(), desc.getRefreshEventNames(), desc.getResetEventNames(),
                useGlobalPageSize.booleanValue(), allQueryParams.toArray(new String[] {}), searchDocBinding,
                searchDocumentType, desc.getResultColumnsBinding(), desc.getResultLayoutBinding(), sortInfosBinding,
                pageSizeBinding, showTitle.booleanValue(), showPageSizeSelector.booleanValue(),
                showRefreshPage.booleanValue(), showFilterForm.booleanValue(), desc.getEmptySentence(),
                translateEmptySentence.booleanValue());
        contentView.setWaitForExecutionSentence(desc.getWaitForExecutionSentence());
        if (desc.getWaitForExecution() != null) {
            contentView.setWaitForExecution(desc.getWaitForExecution().booleanValue());
        }
        return contentView;
    }

    protected ContentViewHeader getContentViewHeader(ContentViewDescriptor desc) {
        return new ContentViewHeader(desc.getName(), desc.getTitle(), Boolean.TRUE.equals(desc.getTranslateTitle()),
                desc.getIconPath());
    }

    @Override
    public ContentViewHeader getContentViewHeader(String name) {
        ContentViewDescriptor desc = contentViewReg.getContentView(name);
        if (desc == null) {
            return null;
        }
        return getContentViewHeader(desc);
    }

    @Override
    public Set<String> getContentViewNames() {
        return Collections.unmodifiableSet(contentViewReg.getContentViewNames());
    }

    @Override
    public Set<ContentViewHeader> getContentViewHeaders() {
        Set<ContentViewHeader> res = new HashSet<ContentViewHeader>();
        for (ContentViewDescriptor desc : contentViewReg.getContentViews()) {
            res.add(getContentViewHeader(desc));
        }
        return Collections.unmodifiableSet(res);
    }

    @Override
    public Set<String> getContentViewNames(String flag) {
        Set<String> res = new LinkedHashSet<String>();
        Set<String> items = contentViewReg.getContentViewsByFlag(flag);
        if (items != null) {
            res.addAll(items);
        }
        return res;
    }

    @Override
    public Set<ContentViewHeader> getContentViewHeaders(String flag) {
        Set<String> cvs = getContentViewNames(flag);
        Set<ContentViewHeader> res = new HashSet<ContentViewHeader>();
        for (String cv : cvs) {
            ContentViewHeader header = getContentViewHeader(cv);
            if (header != null) {
                res.add(header);
            }
        }
        return Collections.unmodifiableSet(res);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            DocumentModel searchDocument, Object... parameters) {
        ContentViewDescriptor contentViewDesc = contentViewReg.getContentView(name);
        if (contentViewDesc == null) {
            return null;
        }
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        String ppName = contentViewDesc.getPageProviderName();
        PageProvider<?> provider = ppService.getPageProvider(ppName, searchDocument, sortInfos, pageSize, currentPage,
                resolvePageProviderProperties(contentViewDesc.getPageProviderProperties()), parameters);
        return provider;
    }

    public Map<String, Serializable> resolvePageProviderProperties(Map<String, String> stringProps) {
        // resolve properties
        Map<String, Serializable> resolvedProps = new HashMap<String, Serializable>();
        for (Map.Entry<String, String> prop : stringProps.entrySet()) {
            resolvedProps.put(prop.getKey(), resolveProperty(prop.getValue()));
        }
        return resolvedProps;
    }

    protected Serializable resolveProperty(String elExpression) {
        FacesContext context = FacesContext.getCurrentInstance();
        Object value = ComponentTagUtils.resolveElExpression(context, elExpression);
        if (value != null && !(value instanceof Serializable)) {
            log.error("Error processing expression '" + elExpression + "', result is not serializable: " + value);
            return null;
        }
        return (Serializable) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(ContentViewService.class)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONTENT_VIEW_EP.equals(extensionPoint)) {
            ContentViewDescriptor desc = (ContentViewDescriptor) contribution;
            contentViewReg.addContribution(desc);
            registerPageProvider(desc);
        }
    }

    protected void registerPageProvider(ContentViewDescriptor desc) {
        ReferencePageProviderDescriptor refDesc = desc.getReferencePageProvider();
        if (refDesc != null && refDesc.isEnabled()) {
            // we use an already registered pp
            return;
        }
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        String name = desc.getName();
        PageProviderDefinition coreDef = getPageProviderDefWithName(name, desc.getCoreQueryPageProvider());
        PageProviderDefinition genDef = getPageProviderDefWithName(name, desc.getGenericPageProvider());
        if (coreDef != null && genDef != null) {
            log.error(String.format("Only one page provider should be registered on "
                    + "content view '%s': take the reference descriptor by default, then core query descriptor, "
                    + "and then generic descriptor", name));
        }
        PageProviderDefinition ppDef = (coreDef != null) ? coreDef : genDef;
        if (ppDef != null) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Register PageProvider from ContentView: %s %s", ppDef.getName(), ppDef));
            }
            ppService.registerPageProviderDefinition(ppDef);
        }
    }

    protected PageProviderDefinition getPageProviderDefWithName(String name, PageProviderDefinition ppDef) {
        if (ppDef != null && ppDef.isEnabled()) {
            if (ppDef.getName() == null) {
                ppDef.setName(name);
            }
            return ppDef;
        }
        return null;
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (CONTENT_VIEW_EP.equals(extensionPoint)) {
            ContentViewDescriptor desc = (ContentViewDescriptor) contribution;
            unregisterPageProvider(desc);
            contentViewReg.removeContribution(desc);
        }
    }

    protected void unregisterPageProvider(ContentViewDescriptor desc) {
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        if (ppService == null) {
            log.info("PageProviderServer is not available, failed to unregister pp of the cv");
            return;
        }
        if (desc.getCoreQueryPageProvider() != null) {

            ppService.unregisterPageProviderDefinition(desc.getCoreQueryPageProvider());
        }
        if (desc.getGenericPageProvider() != null) {
            ppService.unregisterPageProviderDefinition(desc.getGenericPageProvider());
        }
    }

    @Override
    public ContentView restoreContentView(ContentViewState contentViewState) {
        if (contentViewState == null) {
            return null;
        }
        String name = contentViewState.getContentViewName();
        ContentView cv = getContentView(name);
        if (cv != null) {
            restoreContentViewState(cv, contentViewState);
        } else {
            throw new NuxeoException("Unknown content view with name '" + name + "'");
        }
        return cv;
    }

    @Override
    public void restoreContentViewState(ContentView contentView, ContentViewState contentViewState) {
        if (contentView == null || contentViewState == null) {
            return;
        }

        // save some info directly on content view, they will be needed
        // when re-building the provider
        Long pageSize = contentViewState.getPageSize();
        contentView.setCurrentPageSize(pageSize);
        DocumentModel searchDocument = contentViewState.getSearchDocumentModel();
        contentView.setSearchDocumentModel(searchDocument);
        if (searchDocument != null) {
            // check that restored doc type is still in sync with doc type
            // set on content view
            String searchType = contentView.getSearchDocumentModelType();
            if (!searchDocument.getType().equals(searchType)) {
                log.warn(String.format(
                        "Restored document type '%s' is different from "
                                + "the one declared on content view with name '%s': should be '%s'",
                        searchDocument.getType(), contentViewState.getContentViewName(), searchType));
            }
        }
        Long currentPage = contentViewState.getCurrentPage();
        Object[] params = contentViewState.getQueryParameters();

        // init page provider
        contentView.setExecuted(contentViewState.isExecuted());
        contentView.getPageProvider(searchDocument, contentViewState.getSortInfos(), pageSize, currentPage, params);
        // restore rendering info, unless bindings are present on content
        // view configuration
        if (!contentView.hasResultLayoutBinding()) {
            contentView.setCurrentResultLayout(contentViewState.getResultLayout());
        }
        if (!contentView.hasResultLayoutColumnsBinding()) {
            contentView.setCurrentResultLayoutColumns(contentViewState.getResultColumns());
        }
    }

    @Override
    public ContentViewState saveContentView(ContentView contentView) {
        if (contentView == null) {
            return null;
        }
        ContentViewState state = new ContentViewStateImpl();
        state.setContentViewName(contentView.getName());
        state.setPageSize(contentView.getCurrentPageSize());
        // provider info
        PageProvider<?> pp = contentView.getCurrentPageProvider();
        if (pp != null) {
            state.setPageProviderName(pp.getName());
            state.setSearchDocumentModel(pp.getSearchDocumentModel());
            state.setCurrentPage(new Long(pp.getCurrentPageIndex()));
            state.setQueryParameters(pp.getParameters());
            state.setSortInfos(pp.getSortInfos());
        } else {
            // take at least info available on content view
            state.setSearchDocumentModel(contentView.getSearchDocumentModel());
            state.setQueryParameters(contentView.getQueryParameters());
        }
        // rendering info
        state.setResultLayout(contentView.getCurrentResultLayout());
        state.setResultColumns(contentView.getCurrentResultLayoutColumns());
        state.setExecuted(contentView.isExecuted());
        return state;
    }

}
