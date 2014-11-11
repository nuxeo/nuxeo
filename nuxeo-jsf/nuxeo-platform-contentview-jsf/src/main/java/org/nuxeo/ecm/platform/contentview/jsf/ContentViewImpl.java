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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for the content view object.
 * <p>
 * Provides simple getters for attributes defined in the XMap descriptor,
 * except cache key which is computed from currrent {@link FacesContext}
 * instance if cache key is an EL expression.
 * <p>
 * The page provider is initialized calling
 * {@link ContentViewService#getPageProvider}.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class ContentViewImpl implements ContentView {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ContentViewImpl.class);

    protected String name;

    protected PageProvider<?> pageProvider;

    protected String title;

    protected boolean translateTitle;

    protected String emptySentence;

    protected boolean translateEmptySentence;

    protected String iconPath;

    protected boolean showTitle;

    protected String selectionList;

    protected String pagination;

    protected List<String> actionCategories;

    protected ContentViewLayout searchLayout;

    protected List<ContentViewLayout> resultLayouts;

    protected List<String> flags;

    protected ContentViewLayout currentResultLayout;

    protected List<String> currentResultLayoutColumns;

    protected String cacheKey;

    protected Integer cacheSize;

    protected List<String> refreshEventNames;

    protected List<String> resetEventNames;

    protected boolean useGlobalPageSize;

    protected boolean showPageSizeSelector;

    protected boolean showRefreshPage;

    protected Long currentPageSize;

    protected String[] queryParameters;

    protected DocumentModel searchDocumentModel;

    protected String searchDocumentModelBinding;

    protected String searchDocumentModelType;

    protected String resultColumnsBinding;

    protected String pageSizeBinding;

    protected String sortInfosBinding;

    public ContentViewImpl(String name, String title, boolean translateTitle,
            String iconPath, String selectionList, String pagination,
            List<String> actionCategories, ContentViewLayout searchLayout,
            List<ContentViewLayout> resultLayouts, List<String> flags,
            String cacheKey, Integer cacheSize, List<String> refreshEventNames,
            List<String> resetEventNames, boolean useGlobalPageSize,
            String[] queryParameters, String searchDocumentModelBinding,
            String searchDocumentModelType, String resultColumnsBinding,
            String sortInfosBinding, String pageSizeBinding, boolean showTitle,
            boolean showPageSizeSelector, boolean showRefreshPage,
            String emptySentence, boolean translateEmptySentence) {
        this.name = name;
        this.title = title;
        this.translateTitle = translateTitle;
        this.iconPath = iconPath;
        this.selectionList = selectionList;
        this.pagination = pagination;
        this.actionCategories = actionCategories;
        this.searchLayout = searchLayout;
        this.resultLayouts = resultLayouts;
        this.flags = flags;
        this.cacheKey = cacheKey;
        this.cacheSize = cacheSize;
        this.refreshEventNames = refreshEventNames;
        this.resetEventNames = resetEventNames;
        this.useGlobalPageSize = useGlobalPageSize;
        this.queryParameters = queryParameters;
        this.searchDocumentModelBinding = searchDocumentModelBinding;
        this.searchDocumentModelType = searchDocumentModelType;
        this.resultColumnsBinding = resultColumnsBinding;
        this.pageSizeBinding = pageSizeBinding;
        this.sortInfosBinding = sortInfosBinding;
        this.showTitle = showTitle;
        this.showPageSizeSelector = showPageSizeSelector;
        this.showRefreshPage = showRefreshPage;
        this.emptySentence = emptySentence;
        this.translateEmptySentence = translateEmptySentence;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public boolean getTranslateTitle() {
        return translateTitle;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getSelectionListName() {
        return selectionList;
    }

    public String getPagination() {
        return pagination;
    }

    public List<String> getActionsCategories() {
        return actionCategories;
    }

    public ContentViewLayout getSearchLayout() {
        return searchLayout;
    }

    public List<ContentViewLayout> getResultLayouts() {
        return resultLayouts;
    }

    public ContentViewLayout getCurrentResultLayout() {
        if (currentResultLayout == null) {
            if (resultLayouts != null && !resultLayouts.isEmpty()) {
                currentResultLayout = resultLayouts.get(0);
            }
        }
        return currentResultLayout;
    }

    public void setCurrentResultLayout(ContentViewLayout layout) {
        currentResultLayout = layout;
    }

    protected boolean getParametersChanged(Object[] oldParams,
            Object[] newParams) {
        if (oldParams == null && newParams == null) {
            return true;
        } else if (oldParams != null && newParams != null) {
            if (oldParams.length != newParams.length) {
                return true;
            }
            for (int i = 0; i < oldParams.length; i++) {
                if (oldParams[i] == null && newParams[i] == null) {
                    continue;
                } else if (oldParams[i] != null
                        && !oldParams[i].equals(newParams[i])) {
                    return true;
                } else if (newParams[i] != null
                        && !newParams[i].equals(oldParams[i])) {
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Returns cached page provider if it exists or build a new one if
     * parameters have changed.
     * <p>
     * The search document, current page and page size are set on the page
     * provider anyway. Sort infos are not set again if page provider was not
     * built again (e.g if parameters did not change) to avoid erasing sort
     * infos already held by it.
     */
    public PageProvider<?> getPageProvider(DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Object... params) throws ClientException {
        // resolve search doc so that it can be used in EL expressions defined
        // in XML configuration
        DocumentModel finalSearchDocument = null;
        if (searchDocument != null) {
            finalSearchDocument = searchDocument;
        } else {
            if (pageProvider != null) {
                // try to retrieve it on current page provider
                finalSearchDocument = pageProvider.getSearchDocumentModel();
            }
            if (finalSearchDocument == null) {
                // initialize it
                finalSearchDocument = getSearchDocumentModel();
            }
        }
        // set it on content view so that it can be used when resolving EL
        // expressions
        setSearchDocumentModel(finalSearchDocument);

        // fallback on local parameters if defined in the XML configuration
        if (params == null) {
            params = getQueryParameters();
        }
        if (sortInfos == null) {
            sortInfos = resolveSortInfos();
        }
        // allow to pass negative integers instead of null: EL transforms
        // numbers into value 0 for numbers
        if (pageSize != null && pageSize.longValue() < 0) {
            pageSize = null;
        }
        if (currentPage != null && currentPage.longValue() < 0) {
            currentPage = null;
        }
        if (pageSize == null) {
            if (currentPageSize != null) {
                pageSize = currentPageSize;
            }
            if (pageSize == null) {
                pageSize = resolvePageSize();
            }
        }

        // parameters changed => reset provider.
        // do not force setting of sort infos as they can be set directly on
        // the page provider and this method will be called after so they could
        // be lost.
        if (pageProvider == null
                || getParametersChanged(pageProvider.getParameters(), params)) {
            try {
                // make the service build the provider
                ContentViewService service = Framework.getService(ContentViewService.class);
                if (service == null) {
                    throw new ClientException(
                            "Could not resolve ContentViewService");
                }
                pageProvider = service.getPageProvider(getName(), sortInfos,
                        pageSize, currentPage, params);
            } catch (Exception e) {
                throw new ClientException(e);
            }
        } else {
            if (pageSize != null) {
                pageProvider.setPageSize(pageSize.longValue());
            }
            if (currentPage != null) {
                pageProvider.setCurrentPage(currentPage.longValue());
            }
        }
        // set search doc again as page provider may not have been initialized
        // when it was first set
        if (finalSearchDocument != null) {
            setSearchDocumentModel(finalSearchDocument);
        }
        return pageProvider;
    }

    public PageProvider<?> getPageProviderWithParams(Object... params)
            throws ClientException {
        return getPageProvider(null, null, null, null, params);
    }

    public PageProvider<?> getPageProvider() throws ClientException {
        return getPageProviderWithParams((Object[]) null);
    }

    public PageProvider<?> getCurrentPageProvider() {
        return pageProvider;
    }

    public void resetPageProvider() {
        pageProvider = null;
    }

    public void refreshPageProvider() {
        if (pageProvider != null) {
            pageProvider.refresh();
        }
    }

    public void refreshAndRewindPageProvider() {
        if (pageProvider != null) {
            pageProvider.refresh();
            pageProvider.firstPage();
        }
    }

    public String getCacheKey() {
        FacesContext context = FacesContext.getCurrentInstance();
        Object value = ComponentTagUtils.resolveElExpression(context, cacheKey);
        if (value != null && !(value instanceof String)) {
            log.error(String.format("Error processing expression '%s', "
                    + "result is not a String: %s", cacheKey, value));
        }
        return (String) value;
    }

    public Integer getCacheSize() {
        return cacheSize;
    }

    public Object[] getQueryParameters() {
        if (queryParameters == null) {
            return null;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        Object previousSearchDocValue = addSearchDocumentToELContext(context);
        try {
            Object[] res = new Object[queryParameters.length];
            for (int i = 0; i < queryParameters.length; i++) {
                res[i] = ComponentTagUtils.resolveElExpression(context,
                        queryParameters[i]);
            }
            return res;
        } finally {
            removeSearchDocumentFromELContext(context, previousSearchDocValue);
        }
    }

    public List<String> getRefreshEventNames() {
        return refreshEventNames;
    }

    public List<String> getResetEventNames() {
        return resetEventNames;
    }

    public boolean getUseGlobalPageSize() {
        return useGlobalPageSize;
    }

    public Long getCurrentPageSize() {
        if (currentPageSize != null) {
            return currentPageSize;
        }
        if (pageProvider != null) {
            return Long.valueOf(pageProvider.getPageSize());
        }
        return null;
    }

    @Override
    public void setCurrentPageSize(Long pageSize) {
        this.currentPageSize = pageSize;
    }

    public DocumentModel getSearchDocumentModel() {
        if (searchDocumentModel == null) {
            // initialize from binding
            if (searchDocumentModelBinding != null) {
                FacesContext context = FacesContext.getCurrentInstance();
                Object value = ComponentTagUtils.resolveElExpression(context,
                        searchDocumentModelBinding);
                if (value != null && !(value instanceof DocumentModel)) {
                    log.error(String.format(
                            "Error processing expression '%s', "
                                    + "result is not a DocumentModel: %s",
                            searchDocumentModelBinding, value));
                } else {
                    searchDocumentModel = (DocumentModel) value;
                }
            }
        }
        return searchDocumentModel;
    }

    public void setSearchDocumentModel(DocumentModel searchDocumentModel) {
        this.searchDocumentModel = searchDocumentModel;
        if (pageProvider != null) {
            pageProvider.setSearchDocumentModel(searchDocumentModel);
        }
    }

    public void resetSearchDocumentModel() {
        searchDocumentModel = null;
        if (pageProvider != null) {
            pageProvider.setSearchDocumentModel(null);
        }
    }

    public String getSearchDocumentModelType() {
        return searchDocumentModelType;
    }

    public List<String> getFlags() {
        return flags;
    }

    @Override
    public List<String> getResultLayoutColumns() {
        return getCurrentResultLayoutColumns();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getCurrentResultLayoutColumns() {
        if (currentResultLayoutColumns != null) {
            return currentResultLayoutColumns;
        }
        // else resolve bindings
        FacesContext context = FacesContext.getCurrentInstance();
        Object previousSearchDocValue = addSearchDocumentToELContext(context);
        try {
            Object value = ComponentTagUtils.resolveElExpression(context,
                    resultColumnsBinding);
            if (value != null && !(value instanceof List)) {
                log.error(String.format("Error processing expression '%s', "
                        + "result is not a List: %s", resultColumnsBinding,
                        value));
            }
            return (List) value;
        } finally {
            removeSearchDocumentFromELContext(context, previousSearchDocValue);
        }
    }

    @Override
    public void setCurrentResultLayoutColumns(List<String> resultColumns) {
        currentResultLayoutColumns = resultColumns;
    }

    @SuppressWarnings("unchecked")
    protected List<SortInfo> resolveSortInfos() {
        if (sortInfosBinding == null) {
            return null;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        Object previousSearchDocValue = addSearchDocumentToELContext(context);
        try {
            Object value = ComponentTagUtils.resolveElExpression(context,
                    sortInfosBinding);
            if (value != null && !(value instanceof List)) {
                log.error(String.format("Error processing expression '%s', "
                        + "result is not a List: %s", sortInfosBinding, value));
            }
            if (value == null) {
                return null;
            }
            List<SortInfo> res = new ArrayList<SortInfo>();
            List listValue = (List) value;
            for (Object listItem : listValue) {
                if (listItem instanceof SortInfo) {
                    res.add((SortInfo) listItem);
                } else if (listItem instanceof Map) {
                    // XXX: MapProperty does not implement containsKey, so
                    // resolve
                    // value instead
                    if (listItem instanceof MapProperty) {
                        try {
                            listItem = ((MapProperty) listItem).getValue();
                        } catch (Exception e) {
                            log.error("Cannot resolve sort info item: "
                                    + listItem, e);
                        }
                    }
                    Map map = (Map) listItem;
                    SortInfo sortInfo = SortInfo.asSortInfo(map);
                    if (sortInfo != null) {
                        res.add(sortInfo);
                    } else {
                        log.error("Cannot resolve sort info item: " + listItem);
                    }
                } else {
                    log.error("Cannot resolve sort info item: " + listItem);
                }
            }
            if (res.isEmpty()) {
                return null;
            }
            return res;
        } finally {
            removeSearchDocumentFromELContext(context, previousSearchDocValue);
        }
    }

    protected Long resolvePageSize() {
        if (pageSizeBinding == null) {
            return null;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        Object previousSearchDocValue = addSearchDocumentToELContext(context);
        try {
            Object value = ComponentTagUtils.resolveElExpression(context,
                    pageSizeBinding);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                try {
                    return Long.valueOf((String) value);
                } catch (NumberFormatException e) {
                    log.error(String.format(
                            "Error processing expression '%s', "
                                    + "result is not a Long: %s",
                            pageSizeBinding, value));
                }
            } else if (value instanceof Number) {
                return Long.valueOf(((Number) value).longValue());
            }
            return null;
        } finally {
            removeSearchDocumentFromELContext(context, previousSearchDocValue);
        }
    }

    protected Object addSearchDocumentToELContext(FacesContext facesContext) {
        ExternalContext econtext = facesContext.getExternalContext();
        if (econtext != null) {
            Map<String, Object> requestMap = econtext.getRequestMap();
            Object previousValue = requestMap.get(SEARCH_DOCUMENT_EL_VARIABLE);
            requestMap.put(SEARCH_DOCUMENT_EL_VARIABLE, searchDocumentModel);
            return previousValue;
        } else {
            log.error(String.format(
                    "External context is null: cannot expose variable '%s' "
                            + "for content view '%s'",
                    SEARCH_DOCUMENT_EL_VARIABLE, getName()));
            return null;
        }
    }

    protected void removeSearchDocumentFromELContext(FacesContext facesContext,
            Object previousValue) {
        ExternalContext econtext = facesContext.getExternalContext();
        if (econtext != null) {
            Map<String, Object> requestMap = econtext.getRequestMap();
            requestMap.remove(SEARCH_DOCUMENT_EL_VARIABLE);
            if (previousValue != null) {
                requestMap.put(SEARCH_DOCUMENT_EL_VARIABLE, previousValue);
            }
        } else {
            log.error(String.format(
                    "External context is null: cannot dispose variable '%s' "
                            + "for content view '%s'",
                    SEARCH_DOCUMENT_EL_VARIABLE, getName()));
        }
    }

    @Override
    public boolean getShowPageSizeSelector() {
        return showPageSizeSelector;
    }

    @Override
    public boolean getShowRefreshPage() {
        return showRefreshPage;
    }

    @Override
    public boolean getShowTitle() {
        return showTitle;
    }

    public String getEmptySentence() {
        return emptySentence;
    }

    public boolean getTranslateEmptySentence() {
        return translateEmptySentence;
    }

    @Override
    public String toString() {
        return String.format("ContentViewImpl [name=%s, title=%s, "
                + "translateTitle=%s, iconPath=%s, "
                + "selectionList=%s, pagination=%s, "
                + "actionCategories=%s, searchLayout=%s, "
                + "resultLayouts=%s, currentResultLayout=%s, "
                + "flags=%s, cacheKey=%s, cacheSize=%s, currentPageSize=%s"
                + "refreshEventNames=%s, resetEventNames=%s,"
                + "useGlobalPageSize=%s, searchDocumentModel=%s]", name, title,
                Boolean.valueOf(translateTitle), iconPath, selectionList,
                pagination, actionCategories, searchLayout, resultLayouts,
                currentResultLayout, flags, cacheKey, cacheSize,
                currentPageSize, refreshEventNames, resetEventNames,
                Boolean.valueOf(useGlobalPageSize), searchDocumentModel);
    }
}
