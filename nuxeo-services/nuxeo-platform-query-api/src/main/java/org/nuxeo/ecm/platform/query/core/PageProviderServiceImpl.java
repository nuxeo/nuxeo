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
package org.nuxeo.ecm.platform.query.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class PageProviderServiceImpl extends DefaultComponent implements
        PageProviderService {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PageProviderServiceImpl.class);

    public static final String PROVIDER_EP = "providers";

    Map<String, PageProviderDefinition> providers = new HashMap<String, PageProviderDefinition>();

    public PageProviderDefinition getPageProviderDefinition(String name) {
        return providers.get(name);
    }

    public PageProvider<?> getPageProvider(String name,
            PageProviderDefinition desc, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        if (desc == null) {
            return null;
        }
        PageProvider<?> pageProvider;
        if (desc instanceof CoreQueryPageProviderDescriptor) {
            pageProvider = new CoreQueryDocumentPageProvider();
        } else if (desc instanceof GenericPageProviderDescriptor) {
            Class<PageProvider<?>> klass = ((GenericPageProviderDescriptor) desc).getPageProviderClass();
            try {
                pageProvider = klass.newInstance();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        } else {
            throw new ClientException(String.format(
                    "Invalid page definition with name '%s'", name));
        }

        pageProvider.setName(name);
        // set descriptor, used to build the query
        pageProvider.setDefinition(desc);
        // XXX: set local properties without resolving, and merge with given
        // properties.
        Map<String, Serializable> allProps = new HashMap<String, Serializable>();
        Map<String, String> localProps = desc.getProperties();
        if (localProps != null) {
            allProps.putAll(localProps);
        }
        if (properties != null) {
            allProps.putAll(properties);
        }
        pageProvider.setProperties(properties);
        pageProvider.setSortable(desc.isSortable());
        pageProvider.setParameters(parameters);

        Long maxPageSize = desc.getMaxPageSize();
        if (maxPageSize != null) {
            pageProvider.setMaxPageSize(maxPageSize.longValue());
        }

        if (sortInfos == null) {
            pageProvider.setSortInfos(desc.getSortInfos());
        } else {
            pageProvider.setSortInfos(sortInfos);
        }
        if (pageSize == null || pageSize.longValue() < 0) {
            pageProvider.setPageSize(desc.getPageSize());
        } else {
            pageProvider.setPageSize(pageSize.longValue());
        }
        if (currentPage != null && currentPage.longValue() > 0) {
            pageProvider.setCurrentPage(currentPage.longValue());
        }

        return pageProvider;
    }

    @Override
    public PageProvider<?> getPageProvider(String name,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        PageProviderDefinition desc = providers.get(name);
        return getPageProvider(name, desc, sortInfos, pageSize, currentPage,
                properties, parameters);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROVIDER_EP.equals(extensionPoint)) {
            PageProviderDefinition desc = (PageProviderDefinition) contribution;
            String name = desc.getName();
            if (name == null) {
                log.error("Cannot register page provider without a name");
                return;
            }
            boolean enabled = desc.isEnabled();
            if (providers.containsKey(name)) {
                log.info("Overriding page provider with name " + name);
                if (!enabled) {
                    providers.remove(name);
                    log.info("Disabled page provider with name " + name);
                }
            }
            if (enabled) {
                log.info("Registering page provider with name " + name);
                providers.put(name, desc);
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROVIDER_EP.equals(extensionPoint)) {
            PageProviderDefinition desc = (PageProviderDefinition) contribution;
            String name = desc.getName();
            providers.remove(name);
            log.info("Unregistering page provider with name " + name);
        }
    }

}
