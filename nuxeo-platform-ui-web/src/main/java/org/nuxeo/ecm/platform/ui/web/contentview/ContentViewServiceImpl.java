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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PageProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.ui.web.util.ComponentTagUtils;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 */
public class ContentViewServiceImpl extends DefaultComponent implements
        ContentViewService {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ContentViewServiceImpl.class);

    public static final String CONTENT_VIEW_EP = "contentViews";

    protected Map<String, ContentViewDescriptor> contentViews = new HashMap<String, ContentViewDescriptor>();

    public ContentView getContentView(String name) throws ClientException {
        ContentViewDescriptor desc = contentViews.get(name);
        if (desc == null) {
            return null;
        }
        Boolean useGlobalPageSize = desc.getUseGlobalPageSize();
        if (useGlobalPageSize == null) {
            // default value
            useGlobalPageSize = Boolean.FALSE;
        }
        Boolean translateTitle = desc.getTranslateTitle();
        if (translateTitle == null) {
            // default value
            translateTitle = Boolean.FALSE;
        }
        ContentViewImpl contentView = new ContentViewImpl(name,
                desc.getTitle(), translateTitle.booleanValue(),
                desc.getIconPath(), desc.getSelectionListName(),
                desc.getPagination(), desc.getActionCategories(),
                desc.getSearchLayout(), desc.getResultLayouts(),
                desc.getCacheKey(), desc.getCacheSize(),
                desc.getRefreshEventNames(), useGlobalPageSize.booleanValue(),
                desc.getQueryParameters(), desc.getSearchDocumentBinding(),
                desc.getSearchDocumentType());
        return contentView;
    }

    public PageProvider<?> getPageProvider(String name,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Object... parameters) throws ClientException {
        ContentViewDescriptor contentViewDesc = contentViews.get(name);
        if (contentViewDesc == null) {
            return null;
        }
        CoreQueryPageProviderDescriptor coreDesc = contentViewDesc.getCoreQueryPageProvider();
        GenericPageProviderDescriptor genDesc = contentViewDesc.getGenericPageProvider();
        ContentViewPageProvider<?> pageProvider = null;
        if (coreDesc != null && coreDesc.isEnabled() && genDesc != null
                && genDesc.isEnabled()) {
            log.error(String.format(
                    "Only one page provider should be registered on "
                            + "content view '%s': take the core query "
                            + "descriptor by default", name));

        }

        PageProviderDescriptor pageDesc = null;
        if (coreDesc != null && coreDesc.isEnabled()) {
            pageProvider = new CoreQueryDocumentPageProvider();
            pageDesc = coreDesc;
        } else if (genDesc != null && genDesc.isEnabled()) {
            Class<ContentViewPageProvider<?>> klass = genDesc.getPageProviderClass();
            try {
                pageProvider = klass.newInstance();
            } catch (Exception e) {
                throw new ClientException(e);
            }
            pageDesc = genDesc;
        } else {
            throw new ClientException(String.format(
                    "No page provider defined on content view '%s'", name));
        }

        // set same name than content view
        pageProvider.setName(name);
        // set resolved properties
        pageProvider.setProperties(resolvePageProviderProperties(pageDesc.getProperties()));
        // set descriptor, used to build the query
        pageProvider.setPageProviderDescriptor(pageDesc);
        pageProvider.setSortable(pageDesc.isSortable());
        pageProvider.setParameters(parameters);
        if (sortInfos == null) {
            pageProvider.setSortInfos(pageDesc.getSortInfos());
        } else {
            pageProvider.setSortInfos(sortInfos);
        }
        if (pageSize == null) {
            pageProvider.setPageSize(pageDesc.getPageSize());
        } else {
            pageProvider.setPageSize(pageSize.longValue());
        }
        if (currentPage != null && currentPage.longValue() != 0) {
            pageProvider.setCurrentPage(currentPage.longValue());
        }

        return pageProvider;
    }

    public Map<String, Serializable> resolvePageProviderProperties(
            Map<String, String> stringProps) throws ClientException {
        try {
            // resolve properties
            Map<String, Serializable> resolvedProps = new HashMap<String, Serializable>();
            for (Map.Entry<String, String> prop : stringProps.entrySet()) {
                resolvedProps.put(prop.getKey(),
                        resolveProperty(prop.getValue()));
            }
            return resolvedProps;
        } catch (Exception e) {
            throw new ClientException(e);
        }
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
            String name = desc.getName();
            if (name == null) {
                log.error("Cannot register content view without a name");
                return;
            }
            if (contentViews.containsKey(name)) {
                log.info("Overriding content view with name " + name);
                desc = mergeContentViews(contentViews.get(name), desc);
            }
            contentViews.put(name, desc);
        }
    }

    protected ContentViewDescriptor mergeContentViews(
            ContentViewDescriptor oldDesc, ContentViewDescriptor newDesc) {
        String title = newDesc.getTitle();
        if (title != null) {
            oldDesc.title = title;
        }

        Boolean translateTitle = newDesc.getTranslateTitle();
        if (translateTitle != null) {
            oldDesc.translateTitle = translateTitle;
        }

        String iconPath = newDesc.getIconPath();
        if (iconPath != null) {
            oldDesc.iconPath = iconPath;
        }

        List<String> actions = newDesc.getActionCategories();
        if (actions != null && !actions.isEmpty()) {
            oldDesc.actionCategories = actions;
        }

        String cacheKey = newDesc.getCacheKey();
        if (cacheKey != null) {
            oldDesc.cacheKey = cacheKey;
        }

        Integer cacheSize = newDesc.getCacheSize();
        if (cacheSize != null) {
            oldDesc.cacheSize = cacheSize;
        }

        CoreQueryPageProviderDescriptor coreDesc = newDesc.getCoreQueryPageProvider();
        if (coreDesc != null && coreDesc.isEnabled()) {
            oldDesc.coreQueryPageProvider = coreDesc;
        }

        GenericPageProviderDescriptor genDesc = newDesc.getGenericPageProvider();
        if (genDesc != null && genDesc.isEnabled()) {
            oldDesc.genericPageProvider = genDesc;
        }

        String pagination = newDesc.getPagination();
        if (pagination != null) {
            oldDesc.pagination = pagination;
        }

        List<String> events = newDesc.getRefreshEventNames();
        if (events != null && !events.isEmpty()) {
            oldDesc.eventNames = events;
        }
        ContentViewLayoutImpl searchLayout = newDesc.getSearchLayout();
        if (searchLayout != null) {
            oldDesc.searchLayout = searchLayout;
        }
        List<ContentViewLayout> resultLayouts = newDesc.getResultLayouts();
        if (resultLayouts != null) {
            Boolean appendResultLayout = newDesc.appendResultLayouts;
            if (Boolean.TRUE.equals(appendResultLayout)) {
                List<ContentViewLayout> allLayouts = new ArrayList<ContentViewLayout>();
                if (oldDesc.resultLayouts != null) {
                    allLayouts.addAll(oldDesc.resultLayouts);
                }
                allLayouts.addAll(resultLayouts);
                oldDesc.resultLayouts = allLayouts;
            } else {
                oldDesc.resultLayouts = resultLayouts;
            }
        }
        String selectionList = newDesc.getSelectionListName();
        if (selectionList != null) {
            oldDesc.selectionList = selectionList;
        }

        Boolean useGlobalPageSize = newDesc.getUseGlobalPageSize();
        if (useGlobalPageSize != null) {
            oldDesc.useGlobalPageSize = useGlobalPageSize;
        }

        return oldDesc;
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONTENT_VIEW_EP.equals(extensionPoint)) {
            ContentViewDescriptor desc = (ContentViewDescriptor) contribution;
            contentViews.remove(desc.getName());
        }
    }

}
