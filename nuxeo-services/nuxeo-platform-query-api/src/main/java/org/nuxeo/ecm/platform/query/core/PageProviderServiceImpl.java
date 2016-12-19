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
package org.nuxeo.ecm.platform.query.core;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderClassReplacerDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class PageProviderServiceImpl extends DefaultComponent implements PageProviderService {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDER_EP = "providers";

    // @since 6.0
    public static final String REPLACER_EP = "replacers";

    /**
     * @deprecated since 6.0, use {@link PageProviderService#NAMED_PARAMETERS} instead.
     */
    @Deprecated
    public static final String NAMED_PARAMETERS = "namedParameters";

    protected PageProviderRegistry providerReg = new PageProviderRegistry();

    // @since 6.0
    protected PageProviderClassReplacerRegistry replacersReg = new PageProviderClassReplacerRegistry();

    @Override
    public PageProviderDefinition getPageProviderDefinition(String name) {
        PageProviderDefinition def = providerReg.getPageProvider(name);
        if (def == null) {
            return null;
        }
        return def.clone();
    }

    @Override
    public PageProvider<?> getPageProvider(String name, PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage, Map<String, Serializable> properties,
            List<QuickFilter> quickFilters, Object... parameters) {

        if (desc == null) {
            return null;
        }
        PageProvider<?> pageProvider = newPageProviderInstance(name, desc);
        // XXX: set local properties without resolving, and merge with given
        // properties.
        Map<String, Serializable> allProps = new HashMap<>();
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
        pageProvider.setPageSizeOptions(desc.getPageSizeOptions());
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

        if (quickFilters != null) {
            pageProvider.setQuickFilters(quickFilters);
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
    public PageProvider<?> getPageProvider(String name, PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage, Map<String, Serializable> properties,
            Object... parameters) {

        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, null,
                parameters);
    }

    protected PageProvider<?> newPageProviderInstance(String name, PageProviderDefinition desc) {
        PageProvider<?> ret;
        if (desc instanceof CoreQueryPageProviderDescriptor) {
            ret = newCoreQueryPageProviderInstance(name);
        } else if (desc instanceof GenericPageProviderDescriptor) {
            Class<PageProvider<?>> klass = ((GenericPageProviderDescriptor) desc).getPageProviderClass();
            ret = newPageProviderInstance(name, klass);
        } else {
            throw new NuxeoException(String.format("Invalid page provider definition with name '%s'", name));
        }
        ret.setName(name);
        ret.setDefinition(desc);
        return ret;
    }

    protected PageProvider<?> newCoreQueryPageProviderInstance(String name) {
        PageProvider<?> ret;
        Class<? extends PageProvider<?>> klass = replacersReg.getClassForPageProvider(name);
        if (klass == null) {
            ret = new CoreQueryDocumentPageProvider();
        } else {
            ret = newPageProviderInstance(name, klass);
        }
        return ret;
    }

    protected PageProvider<?> newPageProviderInstance(String name, Class<? extends PageProvider<?>> klass) {
        PageProvider<?> ret;
        if (klass == null) {
            throw new NuxeoException(String.format(
                    "Cannot find class for page provider definition with name '%s': check" + " ERROR logs at startup",
                    name));
        }
        try {
            ret = klass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(String.format(
                    "Cannot create an instance of class %s for page provider definition" + " with name '%s'",
                    klass.getName(), name), e);
        }
        return ret;
    }

    @Override
    public PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Map<String, Serializable> properties, Object... parameters) {
        PageProviderDefinition desc = providerReg.getPageProvider(name);
        if (desc == null) {
            throw new NuxeoException(String.format("Could not resolve page provider with name '%s'", name));
        }
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Map<String, Serializable> properties, List<QuickFilter> quickFilters,
            Object... parameters) {
        PageProviderDefinition desc = providerReg.getPageProvider(name);
        if (desc == null) {
            throw new NuxeoException(String.format("Could not resolve page provider with name '%s'", name));
        }
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, quickFilters,
                parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters) {
        return getPageProvider(name, (DocumentModel) null, sortInfos, pageSize, currentPage, properties, parameters);
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PROVIDER_EP.equals(extensionPoint)) {
            PageProviderDefinition desc = (PageProviderDefinition) contribution;
            registerPageProviderDefinition(desc);
        } else if (REPLACER_EP.equals(extensionPoint)) {
            PageProviderClassReplacerDefinition desc = (PageProviderClassReplacerDefinition) contribution;
            replacersReg.addContribution(desc);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (PROVIDER_EP.equals(extensionPoint)) {
            PageProviderDefinition desc = (PageProviderDefinition) contribution;
            unregisterPageProviderDefinition(desc);
        }
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        super.applicationStarted(context);
        replacersReg.dumpReplacerMap();
    }

    @Override
    public void registerPageProviderDefinition(PageProviderDefinition desc) {
        providerReg.addContribution(desc);
    }

    @Override
    public void unregisterPageProviderDefinition(PageProviderDefinition desc) {
        providerReg.removeContribution(desc);
    }

    @Override
    public Set<String> getPageProviderDefinitionNames() {
        return Collections.unmodifiableSet(providerReg.providers.keySet());
    }

}
