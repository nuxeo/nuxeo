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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
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
public class ContentViewServiceImpl extends DefaultComponent implements
        ContentViewService {

    public static final String CONTENT_VIEW_EP = "contentViews";

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ContentViewServiceImpl.class);

    protected ContentViewRegistry contentViewReg = new ContentViewRegistry();

    @Override
    public ContentView getContentView(String name) throws ClientException {
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
            if (ppService == null) {
                throw new ClientException(
                        "Page provider service cannot be resolved");
            }
            PageProviderDefinition def = ppService.getPageProviderDefinition(refDesc.getName());
            if (def == null) {
                log.error("Could not resolve page provider with name "
                        + refDesc.getName());
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
        ContentViewImpl contentView = new ContentViewImpl(name,
                desc.getTitle(), translateTitle.booleanValue(),
                desc.getIconPath(), desc.getSelectionListName(),
                desc.getPagination(), desc.getActionCategories(),
                desc.getSearchLayout(), desc.getResultLayouts(),
                desc.getFlags(), desc.getCacheKey(), desc.getCacheSize(),
                desc.getRefreshEventNames(), desc.getResetEventNames(),
                useGlobalPageSize.booleanValue(),
                allQueryParams.toArray(new String[] {}), searchDocBinding,
                searchDocumentType, desc.getResultColumnsBinding(),
                desc.getResultLayoutBinding(), sortInfosBinding,
                pageSizeBinding, showTitle.booleanValue(),
                showPageSizeSelector.booleanValue(),
                showRefreshPage.booleanValue(), showFilterForm.booleanValue(),
                desc.getEmptySentence(), translateEmptySentence.booleanValue());
        return contentView;
    }

    protected ContentViewHeader getContentViewHeader(ContentViewDescriptor desc) {
        return new ContentViewHeader(desc.getName(), desc.getTitle(),
                Boolean.TRUE.equals(desc.getTranslateTitle()),
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

    public Set<String> getContentViewNames(String flag) {
        Set<String> res = new HashSet<String>();
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

    public PageProvider<?> getPageProvider(String name,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            DocumentModel searchDocument, Object... parameters)
            throws ClientException {
        ContentViewDescriptor contentViewDesc = contentViewReg.getContentView(name);
        if (contentViewDesc == null) {
            return null;
        }
        PageProviderService ppService = Framework.getLocalService(PageProviderService.class);
        if (ppService == null) {
            throw new ClientException("Page provider service is null");
        }

        CoreQueryPageProviderDescriptor coreDesc = contentViewDesc.getCoreQueryPageProvider();
        GenericPageProviderDescriptor genDesc = contentViewDesc.getGenericPageProvider();
        ReferencePageProviderDescriptor refDesc = contentViewDesc.getReferencePageProvider();
        if (coreDesc != null && coreDesc.isEnabled() && genDesc != null
                && genDesc.isEnabled() && refDesc != null
                && refDesc.isEnabled()) {
            log.error(String.format(
                    "Only one page provider should be registered on "
                            + "content view '%s': take the reference "
                            + "descriptor by default, then core query descriptor, "
                            + "and then generic descriptor", name));
        }

        PageProvider<?> provider = null;
        if (refDesc != null && refDesc.isEnabled()) {
            provider = ppService.getPageProvider(refDesc.getName(),
                    searchDocument, sortInfos, pageSize, currentPage,
                    resolvePageProviderProperties(refDesc.getProperties()),
                    parameters);
        } else if (coreDesc != null && coreDesc.isEnabled()) {
            provider = ppService.getPageProvider(name, coreDesc,
                    searchDocument, sortInfos, pageSize, currentPage,
                    resolvePageProviderProperties(coreDesc.getProperties()),
                    parameters);
        } else if (genDesc != null && genDesc.isEnabled()) {
            provider = ppService.getPageProvider(name, genDesc, searchDocument,
                    sortInfos, pageSize, currentPage,
                    resolvePageProviderProperties(genDesc.getProperties()),
                    parameters);
        }

        return provider;
    }

    public Map<String, Serializable> resolvePageProviderProperties(
            Map<String, String> stringProps) throws ClientException {
        // resolve properties
        Map<String, Serializable> resolvedProps = new HashMap<String, Serializable>();
        for (Map.Entry<String, String> prop : stringProps.entrySet()) {
            resolvedProps.put(prop.getKey(), resolveProperty(prop.getValue()));
        }
        return resolvedProps;
    }

    protected Serializable resolveProperty(String elExpression) {
        FacesContext context = FacesContext.getCurrentInstance();
        Object value = ComponentTagUtils.resolveElExpression(context,
                elExpression);
        if (value != null && !(value instanceof Serializable)) {
            log.error(String.format("Error processing expression '%s', "
                    + "result is not serializable: %s", elExpression, value));
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
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONTENT_VIEW_EP.equals(extensionPoint)) {
            ContentViewDescriptor desc = (ContentViewDescriptor) contribution;
            contentViewReg.addContribution(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONTENT_VIEW_EP.equals(extensionPoint)) {
            ContentViewDescriptor desc = (ContentViewDescriptor) contribution;
            contentViewReg.removeContribution(desc);
        }
    }

    @Override
    public ContentView restoreContentView(ContentViewState contentViewState)
            throws ClientException {
        if (contentViewState == null) {
            return null;
        }
        String name = contentViewState.getContentViewName();
        ContentView cv = getContentView(name);
        if (cv != null) {
            // save some info directly on content view, they will be needed
            // when re-building the provider
            Long pageSize = contentViewState.getPageSize();
            cv.setCurrentPageSize(pageSize);
            DocumentModel searchDocument = contentViewState.getSearchDocumentModel();
            cv.setSearchDocumentModel(searchDocument);
            if (searchDocument != null) {
                // check that restored doc type is still in sync with doc type
                // set on content view
                String searchType = cv.getSearchDocumentModelType();
                if (!searchDocument.getType().equals(searchType)) {
                    log.warn(String.format(
                            "Restored document type '%s' is different from "
                                    + "the one declared on content view "
                                    + "with name '%s': should be '%s'",
                            searchDocument.getType(), name, searchType));
                }
            }
            Long currentPage = contentViewState.getCurrentPage();
            Object[] params = contentViewState.getQueryParameters();
            // init page provider
            cv.getPageProvider(searchDocument, contentViewState.getSortInfos(),
                    pageSize, currentPage, params);
            // restore rendering info
            cv.setCurrentResultLayout(contentViewState.getResultLayout());
            cv.setCurrentResultLayoutColumns(contentViewState.getResultColumns());
        } else {
            throw new ClientException(String.format(
                    "Unknown content view with name '%s'", name));
        }
        return cv;
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
        return state;
    }

}
