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
import java.util.HashMap;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.PageProvider;
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
        ContentViewImpl contentView = new ContentViewImpl(name,
                desc.getSelectionListName(), desc.getPagination(),
                desc.getActionCategories(), desc.getSearchLayoutName(),
                desc.getResultLayoutName(), desc.getCacheKey(),
                desc.getRefreshEventNames());
        return contentView;
    }

    public PageProvider<?> getPageProvider(String name) throws ClientException {
        ContentViewDescriptor contentViewDesc = contentViews.get(name);
        if (contentViewDesc == null) {
            return null;
        }
        CoreQueryPageProviderDescriptor coreDesc = contentViewDesc.getCoreQueryPageProvider();
        GenericPageProviderDescriptor genDesc = contentViewDesc.getGenericPageProvider();
        ContentViewPageProvider<?> pageProvider = null;
        if (coreDesc != null && genDesc != null) {
            log.error(String.format(
                    "Only one page provider should be registered on "
                            + "content view '%s': take the core query "
                            + "descriptor by default", name));

        }

        PageProviderDescriptor pageDesc = null;
        if (coreDesc != null) {
            pageProvider = new CoreQueryDocumentPageProvider();
            pageDesc = coreDesc;
        } else if (genDesc != null) {
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
        pageProvider.setSortInfos(pageDesc.getSortInfos());
        pageProvider.setPageSize(pageDesc.getPageSize());

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
            }
            contentViews.put(name, desc);
        }
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
