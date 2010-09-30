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
package org.nuxeo.ecm.platform.ui.web.contentview;

import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PageProvider;
import org.nuxeo.ecm.core.api.SortInfo;
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

    protected String iconPath;

    protected String selectionList;

    protected String pagination;

    protected List<String> actionCategories;

    protected ContentViewLayout searchLayout;

    protected List<ContentViewLayout> resultLayouts;

    protected List<String> flags;

    protected ContentViewLayout currentResultLayout;

    protected String cacheKey;

    protected Integer cacheSize;

    protected List<String> refreshEventNames;

    protected boolean useGlobalPageSize;

    protected String[] queryParameters;

    protected DocumentModel searchDocumentModel;

    protected String searchDocumentModelBinding;

    protected String searchDocumentModelType;

    public ContentViewImpl(String name, String title, boolean translateTitle,
            String iconPath, String selectionList, String pagination,
            List<String> actionCategories, ContentViewLayout searchLayout,
            List<ContentViewLayout> resultLayouts, List<String> flags,
            String cacheKey, Integer cacheSize, List<String> refreshEventNames,
            boolean useGlobalPageSize, String[] queryParameters,
            String searchDocumentModelBinding, String searchDocumentModelType) {
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
        this.useGlobalPageSize = useGlobalPageSize;
        this.queryParameters = queryParameters;
        this.searchDocumentModelBinding = searchDocumentModelBinding;
        this.searchDocumentModelType = searchDocumentModelType;
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
                } else if (oldParams[i] != null && newParams[i] != null) {
                    if (!oldParams[i].equals(newParams[i])) {
                        return true;
                    }
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
     */
    public PageProvider<?> getPageProvider(DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Object... params) throws ClientException {
        // allow to passe negative integers instead of null: EL transforms
        // numbers into value 0 for numbers
        if (currentPage != null && currentPage.longValue() < 0) {
            currentPage = null;
        }
        if (params == null) {
            // fallback on local parameters
            params = getQueryParameters();
        }
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
            if (sortInfos != null) {
                pageProvider.setSortInfos(sortInfos);
            }
            if (pageSize != null) {
                pageProvider.setPageSize(pageSize.longValue());
            }
            if (currentPage != null) {
                pageProvider.setCurrentPage(currentPage.longValue());
            }
        }
        if (searchDocument != null) {
            pageProvider.setSearchDocumentModel(searchDocument);
        } else {
            // initialize on page provider only if not already set
            DocumentModel searchDoc = getSearchDocumentModel();
            if (searchDoc != null
                    && pageProvider.getSearchDocumentModel() == null) {
                pageProvider.setSearchDocumentModel(searchDoc);
            }
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
        Object[] res = new Object[queryParameters.length];
        for (int i = 0; i < queryParameters.length; i++) {
            res[i] = ComponentTagUtils.resolveElExpression(context,
                    queryParameters[i]);
        }
        return res;
    }

    public List<String> getRefreshEventNames() {
        return refreshEventNames;
    }

    public boolean getUseGlobalPageSize() {
        return useGlobalPageSize;
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
    public String toString() {
        return String.format("ContentViewImpl [name=%s, title=%s, "
                + "translateTitle=%s, iconPath=%s, "
                + "selectionList=%s, pagination=%s, "
                + "actionCategories=%s, searchLayout=%s, "
                + "resultLayouts=%s, currentResultLayout=%s, "
                + "flags=%s, cacheKey=%s, cacheSize=%s, refreshEventNames=%s, "
                + "useGlobalPageSize=%s, searchDocumentModel=%s]", name, title,
                Boolean.valueOf(translateTitle), iconPath, selectionList,
                pagination, actionCategories, searchLayout, resultLayouts,
                currentResultLayout, flags, cacheKey, cacheSize,
                refreshEventNames, Boolean.valueOf(useGlobalPageSize),
                searchDocumentModel);
    }
}
