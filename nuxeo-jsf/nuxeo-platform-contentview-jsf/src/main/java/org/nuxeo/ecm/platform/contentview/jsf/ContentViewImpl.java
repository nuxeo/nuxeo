/*
 * (C) Copyright 2010-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelFactory;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.model.impl.MapProperty;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderChangedListener;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.api.Framework;

import com.google.common.base.Function;

/**
 * Default implementation for the content view object.
 * <p>
 * Provides simple getters for attributes defined in the XMap descriptor, except cache key which is computed from
 * currrent {@link FacesContext} instance if cache key is an EL expression.
 * <p>
 * The page provider is initialized calling {@link ContentViewService#getPageProvider}.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class ContentViewImpl implements ContentView, PageProviderChangedListener {

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

    protected boolean currentResultLayoutSet = false;

    protected ContentViewLayout currentResultLayout;

    protected List<String> currentResultLayoutColumns;

    protected String cacheKey;

    protected Integer cacheSize;

    protected List<String> refreshEventNames;

    protected List<String> resetEventNames;

    protected boolean useGlobalPageSize;

    protected boolean showPageSizeSelector;

    protected boolean showRefreshCommand;

    protected boolean showFilterForm;

    protected Long currentPageSize;

    protected String[] queryParameters;

    protected DocumentModel searchDocumentModel;

    protected String searchDocumentModelBinding;

    protected String searchDocumentModelType;

    protected String resultColumnsBinding;

    protected String resultLayoutBinding;

    protected String pageSizeBinding;

    protected String sortInfosBinding;

    protected boolean waitForExecution = false;

    protected String waitForExecutionSentence;

    protected boolean executed = false;

    /**
     * @since 8.4
     */
    protected List<QuickFilter> quickFilters;

    public ContentViewImpl(String name, String title, boolean translateTitle, String iconPath, String selectionList,
            String pagination, List<String> actionCategories, ContentViewLayout searchLayout,
            List<ContentViewLayout> resultLayouts, List<String> flags, String cacheKey, Integer cacheSize,
            List<String> refreshEventNames, List<String> resetEventNames, boolean useGlobalPageSize,
            String[] queryParameters, String searchDocumentModelBinding, String searchDocumentModelType,
            String resultColumnsBinding, String resultLayoutBinding, String sortInfosBinding, String pageSizeBinding,
            boolean showTitle, boolean showPageSizeSelector, boolean showRefreshCommand, boolean showFilterForm,
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
        if (cacheSize != null && cacheSize.intValue() <= 0) {
            // force a static cache key
            this.cacheKey = "static_key_no_cache";
        }
        this.refreshEventNames = refreshEventNames;
        this.resetEventNames = resetEventNames;
        this.useGlobalPageSize = useGlobalPageSize;
        this.queryParameters = queryParameters;
        this.searchDocumentModelBinding = searchDocumentModelBinding;
        this.searchDocumentModelType = searchDocumentModelType;
        this.resultColumnsBinding = resultColumnsBinding;
        this.resultLayoutBinding = resultLayoutBinding;
        this.pageSizeBinding = pageSizeBinding;
        this.sortInfosBinding = sortInfosBinding;
        this.showTitle = showTitle;
        this.showPageSizeSelector = showPageSizeSelector;
        this.showRefreshCommand = showRefreshCommand;
        this.showFilterForm = showFilterForm;
        this.emptySentence = emptySentence;
        this.translateEmptySentence = translateEmptySentence;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean getTranslateTitle() {
        return translateTitle;
    }

    @Override
    public String getIconPath() {
        return iconPath;
    }

    @Override
    public String getSelectionListName() {
        return selectionList;
    }

    @Override
    public String getPagination() {
        return pagination;
    }

    @Override
    public List<String> getActionsCategories() {
        return actionCategories;
    }

    @Override
    public ContentViewLayout getSearchLayout() {
        return searchLayout;
    }

    @Override
    public List<ContentViewLayout> getResultLayouts() {
        return resultLayouts;
    }

    @Override
    public ContentViewLayout getCurrentResultLayout() {
        // resolve binding if it is set
        if (!currentResultLayoutSet && !StringUtils.isBlank(resultLayoutBinding)) {
            Object res = resolveWithSearchDocument(
                    ctx -> ComponentTagUtils.resolveElExpression(ctx, resultLayoutBinding));
            if (res != null && res instanceof String) {
                setCurrentResultLayout((String) res);
                currentResultLayoutSet = true;
            }
        }
        if (currentResultLayout == null && resultLayouts != null && !resultLayouts.isEmpty()) {
            // resolve first current result layout
            return resultLayouts.get(0);
        }
        return currentResultLayout;
    }

    @Override
    public void setCurrentResultLayout(final ContentViewLayout layout) {
        setCurrentResultLayout(layout, true);
    }

    public void setCurrentResultLayout(final ContentViewLayout layout, boolean resetLayoutColumn) {
        if (!isBlank(resultLayoutBinding) && ComponentTagUtils.isStrictValueReference(resultLayoutBinding)) {
            resolveWithSearchDocument(ctx -> {
                ComponentTagUtils.applyValueExpression(ctx, resultLayoutBinding,
                        layout == null ? null : layout.getName());
                return null;
            });
        }
        // still set current result layout value
        currentResultLayoutSet = true;
        currentResultLayout = layout;

        if (resetLayoutColumn) {
            // reset corresponding columns
            setCurrentResultLayoutColumns(null);
        }
    }

    protected Object resolveWithSearchDocument(Function<FacesContext, Object> func) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (getSearchDocumentModel() == null) {
            return func.apply(ctx);
        } else {
            Object previousSearchDocValue = addSearchDocumentToELContext(ctx);
            try {
                return func.apply(ctx);
            } finally {
                removeSearchDocumentFromELContext(ctx, previousSearchDocValue);
            }
        }
    }

    @Override
    public void setCurrentResultLayout(String resultLayoutName) {
        if (resultLayoutName != null) {
            for (ContentViewLayout layout : resultLayouts) {
                if (resultLayoutName.equals(layout.getName())) {
                    setCurrentResultLayout(layout, false);
                }
            }
        }
    }

    @Override
    public boolean hasResultLayoutBinding() {
        return !isBlank(resultLayoutBinding);
    }

    /**
     * Returns cached page provider if it exists or build a new one if parameters have changed.
     * <p>
     * The search document, current page and page size are set on the page provider anyway. Sort infos are not set again
     * if page provider was not built again (e.g if parameters did not change) to avoid erasing sort infos already held
     * by it.
     */
    @Override
    public PageProvider<?> getPageProvider(DocumentModel searchDocument, List<SortInfo> sortInfos, Long pageSize,
            Long currentPage, Object... params) {
        // do not return any page provider if filter has not been done yet
        if (isWaitForExecution() && !isExecuted()) {
            return null;
        }

        // resolve search doc so that it can be used in EL expressions defined
        // in XML configuration
        boolean setSearchDoc = false;
        DocumentModel finalSearchDocument = null;
        if (searchDocument != null) {
            setSearchDoc = true;
            finalSearchDocument = searchDocument;
        } else if (searchDocumentModel == null) {
            setSearchDoc = true;
            if (pageProvider != null) {
                // try to retrieve it on current page provider
                finalSearchDocument = pageProvider.getSearchDocumentModel();
            }
            if (finalSearchDocument == null) {
                // initialize it and set it => do not need to set it again
                finalSearchDocument = getSearchDocumentModel();
                setSearchDoc = false;
            }
        } else {
            finalSearchDocument = searchDocumentModel;
        }
        if (setSearchDoc) {
            // set it on content view so that it can be used when resolving EL
            // expressions
            setSearchDocumentModel(finalSearchDocument);
        }

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
            if (currentPageSize != null && currentPageSize.longValue() >= 0) {
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
        if (pageProvider == null || pageProvider.hasChangedParameters(params)) {
            // make the service build the provider
            ContentViewService service = Framework.getLocalService(ContentViewService.class);
            pageProvider = service.getPageProvider(getName(), sortInfos, pageSize, currentPage, finalSearchDocument,
                    params);
        } else {
            if (pageSize != null) {
                pageProvider.setPageSize(pageSize.longValue());
            }
            if (currentPage != null) {
                pageProvider.setCurrentPage(currentPage.longValue());
            }
        }

        pageProvider.setQuickFilters(new ArrayList<>());

        // Register listener to be notified when the page has changed on the
        // page provider
        pageProvider.setPageProviderChangedListener(this);
        return pageProvider;
    }

    @Override
    public PageProvider<?> getPageProviderWithParams(Object... params) {
        return getPageProvider(null, null, null, null, params);
    }

    @Override
    public PageProvider<?> getPageProvider() {
        return getPageProviderWithParams((Object[]) null);
    }

    @Override
    public PageProvider<?> getCurrentPageProvider() {
        return pageProvider;
    }

    @Override
    public void resetPageProvider() {
        pageProvider = null;
    }

    @Override
    public void refreshPageProvider() {
        if (pageProvider != null) {
            pageProvider.refresh();
        }
        setExecuted(true);
    }

    @Override
    public void refreshAndRewindPageProvider() {
        if (pageProvider != null) {
            pageProvider.refresh();
            pageProvider.firstPage();
        }
        setExecuted(true);
    }

    @Override
    public String getCacheKey() {
        FacesContext context = FacesContext.getCurrentInstance();
        Object value = ComponentTagUtils.resolveElExpression(context, cacheKey);
        if (value != null && !(value instanceof String)) {
            log.error("Error processing expression '" + cacheKey + "', result is not a String: " + value);
        }
        return (String) value;
    }

    @Override
    public Integer getCacheSize() {
        return cacheSize;
    }

    @Override
    public Object[] getQueryParameters() {
        if (queryParameters == null) {
            return null;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        Object previousSearchDocValue = addSearchDocumentToELContext(context);
        try {
            Object[] res = new Object[queryParameters.length];
            for (int i = 0; i < queryParameters.length; i++) {
                res[i] = ComponentTagUtils.resolveElExpression(context, queryParameters[i]);
            }
            return res;
        } finally {
            removeSearchDocumentFromELContext(context, previousSearchDocValue);
        }
    }

    @Override
    public List<String> getRefreshEventNames() {
        return refreshEventNames;
    }

    @Override
    public List<String> getResetEventNames() {
        return resetEventNames;
    }

    @Override
    public boolean getUseGlobalPageSize() {
        return useGlobalPageSize;
    }

    @Override
    public Long getCurrentPageSize() {
        // take actual value on page provider first in case it's reached its
        // max page size
        if (pageProvider != null) {
            long pageSize = pageProvider.getPageSize();
            long maxPageSize = pageProvider.getMaxPageSize();
            if (pageSize > 0 && maxPageSize > 0 && maxPageSize < pageSize) {
                return Long.valueOf(maxPageSize);
            }
            return Long.valueOf(pageSize);
        }
        if (currentPageSize != null && currentPageSize.longValue() >= 0) {
            return currentPageSize;
        }
        return null;
    }

    @Override
    public void setCurrentPageSize(Long pageSize) {
        currentPageSize = pageSize;
        raiseEvent(CONTENT_VIEW_PAGE_SIZE_CHANGED_EVENT);
    }

    @Override
    public DocumentModel getSearchDocumentModel() {
        if (searchDocumentModel == null) {
            if (searchDocumentModelBinding != null) {
                // initialize from binding
                FacesContext context = FacesContext.getCurrentInstance();
                Object value = ComponentTagUtils.resolveElExpression(context, searchDocumentModelBinding);
                if (value != null && !(value instanceof DocumentModel)) {
                    log.error("Error processing expression '" + searchDocumentModelBinding
                            + "', result is not a DocumentModel: " + value);
                } else {
                    setSearchDocumentModel((DocumentModel) value);
                }
            }
            if (searchDocumentModel == null) {
                // generate a bare document model of given type
                String docType = getSearchDocumentModelType();
                if (docType != null) {
                    DocumentModel bareDoc = DocumentModelFactory.createDocumentModel(docType);
                    setSearchDocumentModel(bareDoc);
                }
            }
        }
        return searchDocumentModel;
    }

    @Override
    public void setSearchDocumentModel(DocumentModel searchDocumentModel) {
        this.searchDocumentModel = searchDocumentModel;
        if (pageProvider != null) {
            pageProvider.setSearchDocumentModel(searchDocumentModel);
        }
    }

    @Override
    public void resetSearchDocumentModel() {
        searchDocumentModel = null;
        if (pageProvider != null) {
            pageProvider.setSearchDocumentModel(null);
        }
    }

    @Override
    public String getSearchDocumentModelType() {
        return searchDocumentModelType;
    }

    @Override
    public List<String> getFlags() {
        return flags;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<String> getCurrentResultLayoutColumns() {
        // always resolve binding if it is set
        if (!StringUtils.isBlank(resultColumnsBinding)) {
            Object res = resolveWithSearchDocument(ctx -> {
                Object value = ComponentTagUtils.resolveElExpression(ctx, resultColumnsBinding);
                if (value != null && !(value instanceof List)) {
                    log.error("Error processing expression '" + resultColumnsBinding + "', result is not a List: "
                            + value);
                }
                return value;
            });
            if (res != null && res instanceof List) {
                return ((List) res).isEmpty() ? null : (List) res;
            }
        }
        return currentResultLayoutColumns;
    }

    @Override
    public void setCurrentResultLayoutColumns(final List<String> resultColumns) {
        if (isBlank(resultColumnsBinding) || !ComponentTagUtils.isStrictValueReference(resultColumnsBinding)) {
            // set local values
            currentResultLayoutColumns = resultColumns;
        } else {
            resolveWithSearchDocument(ctx -> {
                ComponentTagUtils.applyValueExpression(ctx, resultColumnsBinding, resultColumns);
                return null;
            });
        }
    }

    @Override
    public boolean hasResultLayoutColumnsBinding() {
        return !isBlank(resultColumnsBinding);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<SortInfo> resolveSortInfos() {
        if (sortInfosBinding == null) {
            return null;
        }
        FacesContext context = FacesContext.getCurrentInstance();
        Object previousSearchDocValue = addSearchDocumentToELContext(context);
        try {
            Object value = ComponentTagUtils.resolveElExpression(context, sortInfosBinding);
            if (value != null && !(value instanceof List)) {
                log.error("Error processing expression '" + sortInfosBinding + "', result is not a List: '" + value
                        + "'");
            }
            if (value == null) {
                return null;
            }
            List<SortInfo> res = new ArrayList<>();
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
                        } catch (ClassCastException | PropertyException e) {
                            log.error("Cannot resolve sort info item: " + listItem, e);
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
            Object value = ComponentTagUtils.resolveElExpression(context, pageSizeBinding);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                try {
                    return Long.valueOf((String) value);
                } catch (NumberFormatException e) {
                    log.error("Error processing expression '" + pageSizeBinding + "', result is not a Long: '" + value
                            + "'");
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
        ExternalContext econtext = null;
        if (facesContext != null) {
            econtext = facesContext.getExternalContext();
        }
        if (facesContext == null || econtext == null) {
            log.error("JSF context is null: cannot expose variable '" + SEARCH_DOCUMENT_EL_VARIABLE
                    + "' for content view '" + getName() + "'");
            return null;
        }
        Map<String, Object> requestMap = econtext.getRequestMap();
        Object previousValue = requestMap.get(SEARCH_DOCUMENT_EL_VARIABLE);
        requestMap.put(SEARCH_DOCUMENT_EL_VARIABLE, searchDocumentModel);
        return previousValue;
    }

    protected void removeSearchDocumentFromELContext(FacesContext facesContext, Object previousValue) {
        if (facesContext == null) {
            // ignore
            return;
        }
        ExternalContext econtext = facesContext.getExternalContext();
        if (econtext != null) {
            Map<String, Object> requestMap = econtext.getRequestMap();
            requestMap.remove(SEARCH_DOCUMENT_EL_VARIABLE);
            if (previousValue != null) {
                requestMap.put(SEARCH_DOCUMENT_EL_VARIABLE, previousValue);
            }
        } else {
            log.error("External context is null: cannot dispose variable '" + SEARCH_DOCUMENT_EL_VARIABLE
                    + "' for content view '" + getName() + "'");
        }
    }

    @Override
    public boolean getShowPageSizeSelector() {
        return showPageSizeSelector;
    }

    @Override
    public boolean getShowRefreshCommand() {
        return showRefreshCommand;
    }

    @Override
    public boolean getShowFilterForm() {
        return showFilterForm;
    }

    @Override
    public boolean getShowTitle() {
        return showTitle;
    }

    @Override
    public String getEmptySentence() {
        return emptySentence;
    }

    @Override
    public boolean getTranslateEmptySentence() {
        return translateEmptySentence;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("ContentViewImpl")
           .append(" {")
           .append(" name=")
           .append(name)
           .append(", title=")
           .append(title)
           .append(", translateTitle=")
           .append(translateTitle)
           .append(", iconPath=")
           .append(iconPath)
           .append(", selectionList=")
           .append(selectionList)
           .append(", pagination=")
           .append(pagination)
           .append(", actionCategories=")
           .append(actionCategories)
           .append(", searchLayout=")
           .append(searchLayout)
           .append(", resultLayouts=")
           .append(resultLayouts)
           .append(", currentResultLayout=")
           .append(currentResultLayout)
           .append(", flags=")
           .append(flags)
           .append(", cacheKey=")
           .append(cacheKey)
           .append(", cacheSize=")
           .append(cacheSize)
           .append(", currentPageSize=")
           .append(currentPageSize)
           .append(", refreshEventNames=")
           .append(refreshEventNames)
           .append(", resetEventNames=")
           .append(resetEventNames)
           .append(", useGlobalPageSize=")
           .append(useGlobalPageSize)
           .append(", searchDocumentModel=")
           .append(searchDocumentModel)
           .append('}');
        return buf.toString();
    }

    /*
     * ----- PageProviderChangedListener -----
     */

    protected void raiseEvent(String eventName, Object... params) {
        if (Events.exists()) {
            Events.instance().raiseEvent(eventName, params);
        }
    }

    protected void raiseEvent(String eventName) {
        raiseEvent(eventName, name);
    }

    @Override
    public void pageChanged(PageProvider<?> pageProvider) {
        raiseEvent(CONTENT_VIEW_PAGE_CHANGED_EVENT);
    }

    @Override
    public void refreshed(PageProvider<?> pageProvider) {
        raiseEvent(CONTENT_VIEW_REFRESH_EVENT);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void resetPageProviderAggregates() {
        if (pageProvider != null && pageProvider.hasAggregateSupport()) {
            Map<String, ? extends Aggregate> aggs = pageProvider.getAggregates();
            for (Aggregate agg : aggs.values()) {
                agg.resetSelection();
            }
        }
    }

    @Override
    public String getWaitForExecutionSentence() {
        return waitForExecutionSentence;
    }

    /**
     * @since 7.4
     */
    public void setWaitForExecutionSentence(String waitForExecutionSentence) {
        this.waitForExecutionSentence = waitForExecutionSentence;
    }

    @Override
    public boolean isWaitForExecution() {
        return waitForExecution;
    }

    /**
     * @since 7.4
     */
    public void setWaitForExecution(boolean waitForExecution) {
        this.waitForExecution = waitForExecution;
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @Override
    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    /**
     * @since 8.4
     */
    public void setQuickFilters(List<QuickFilter> quickFilters) {
        this.quickFilters = quickFilters;
    }

    /**
     * @since 8.4
     */
    public List<QuickFilter> getQuickFilters() {
        return this.quickFilters;
    }
}
