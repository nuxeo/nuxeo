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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
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

    public static final String PROVIDER_EP = "providers";

    public static final String NAMED_PARAMETERS = "namedParameters";

    protected PageProviderRegistry providerReg = new PageProviderRegistry();

    public PageProviderDefinition getPageProviderDefinition(String name) {
        PageProviderDefinition def = providerReg.getPageProvider(name);
        if (def != null) {
            return def.clone();
        }
        return def;
    }

    public PageProvider<?> getPageProvider(String name,
            PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
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
            if (klass == null) {
                throw new ClientException(String.format(
                        "Cannot find class for page provider "
                                + "definition with name '%s': check"
                                + " ERROR logs at startup", name));
            }
            try {
                pageProvider = klass.newInstance();
            } catch (Exception e) {
                throw new ClientException(e);
            }
        } else {
            throw new ClientException(String.format(
                    "Invalid page provider definition with name '%s'", name));
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
        pageProvider.setProperties(allProps);
        pageProvider.setSortable(desc.isSortable());
        pageProvider.setParameters(parameters);
        if (searchDocument != null) {
            pageProvider.setSearchDocumentModel(searchDocument);
        }

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
            PageProviderDefinition desc, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        return getPageProvider(name, desc, null, sortInfos, pageSize,
                currentPage, properties, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name,
            DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        PageProviderDefinition desc = providerReg.getPageProvider(name);
        if (desc == null) {
            throw new ClientException(String.format(
                    "Could not resolve page provider with name '%s'", name));
        }
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize,
                currentPage, properties, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters)
            throws ClientException {
        return getPageProvider(name, (DocumentModel) null, sortInfos, pageSize,
                currentPage, properties, parameters);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROVIDER_EP.equals(extensionPoint)) {
            PageProviderDefinition desc = (PageProviderDefinition) contribution;
            providerReg.addContribution(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROVIDER_EP.equals(extensionPoint)) {
            PageProviderDefinition desc = (PageProviderDefinition) contribution;
            providerReg.removeContribution(desc);
        }
    }
}
