/*
 * (C) Copyright 2010-2017 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.registry.MapRegistry;
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
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class PageProviderServiceImpl extends DefaultComponent implements PageProviderService {

    private static final Logger log = LogManager.getLogger(PageProviderServiceImpl.class);

    private static final long serialVersionUID = 1L;

    public static final String PROVIDER_EP = "providers";

    // @since 6.0
    public static final String REPLACER_EP = "replacers";

    // definitions added through API, kept for compatibility with JSF ContentViewService
    protected Map<String, PageProviderDefinition> programmaticDefinitions;

    protected Map<String, Class<? extends PageProvider<?>>> replacerMap;

    @Override
    public void activate(ComponentContext context) {
        programmaticDefinitions = new ConcurrentHashMap<>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        programmaticDefinitions = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start(ComponentContext context) {
        replacerMap = new HashMap<>();
        this.<PageProviderClassReplacerDefinition> getRegistryContributions(REPLACER_EP).forEach(desc -> {
            String className = desc.getPageProviderClassName();
            if (className == null) {
                log.error("Cannot register page provider class replacer without class name");
            } else {
                Class<? extends PageProvider<?>> klass;
                try {
                    klass = (Class<? extends PageProvider<?>>) Class.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(String.format("Class %s not found", className));
                }
                if (!PageProvider.class.isAssignableFrom(klass)) {
                    throw new IllegalStateException(
                            String.format("Class %s does not implement PageProvider interface", className));
                }
                for (String providerName : desc.getPageProviderNames()) {
                    replacerMap.put(providerName, klass);
                }
            }
        });
        dumpReplacerMap();
    }

    public void dumpReplacerMap() {
        if (log.isInfoEnabled()) {
            if (replacerMap.isEmpty()) {
                log.info("No page provider has been superseded");
                return;
            }
            StringBuilder out = new StringBuilder();
            out.append("List of page provider names that are superseded: \n");
            for (Map.Entry<String, Class<? extends PageProvider<?>>> entry : replacerMap.entrySet()) {
                out.append(String.format("  - %s: %s%n", entry.getKey(), entry.getValue().getName()));
            }
            log.info(out.toString());
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        replacerMap = null;
    }

    /**
     * Returns clones as definition API accepts setters.
     */
    protected Optional<PageProviderDefinition> getOptionalPageProviderDefinition(String name) {
        return Optional.ofNullable(programmaticDefinitions.get(name))
                       .or(() -> getRegistryContribution(PROVIDER_EP, name))
                       // returns clones as definition API accepts setters
                       .map(PageProviderDefinition::clone);
    }

    protected PageProviderDefinition getPageProviderDefinitionOrElseThrow(String name) {
        return getOptionalPageProviderDefinition(name).orElseThrow(
                () -> new NuxeoException(String.format("Could not resolve page provider with name '%s'", name)));
    }

    @Override
    public Set<String> getPageProviderDefinitionNames() {
        return Stream.of(programmaticDefinitions.keySet(),
                this.<MapRegistry> getExtensionPointRegistry(PROVIDER_EP).getContributions().keySet())
                     .flatMap(Set::stream)
                     .collect(Collectors.toSet());
    }

    @Override
    public PageProviderDefinition getPageProviderDefinition(String name) {
        return getOptionalPageProviderDefinition(name).orElse(null);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage, Map<String, Serializable> properties,
            List<String> highlights, List<QuickFilter> quickFilters, Object... parameters) {
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, null, properties,
                highlights, quickFilters, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage, Long currentOffset,
            Map<String, Serializable> properties, List<String> highlights, List<QuickFilter> quickFilters,
            Object... parameters) {

        if (desc == null) {
            return null;
        }
        PageProvider<?> pageProvider = newPageProviderInstance(name, desc);
        // set local properties without resolving, and merge with given properties
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

        if (highlights != null) {
            pageProvider.setHighlights(highlights);
        }

        if (pageSize == null || pageSize.longValue() < 0) {
            pageProvider.setPageSize(desc.getPageSize());
        } else {
            pageProvider.setPageSize(pageSize.longValue());
        }
        if (currentPage != null && currentPage.longValue() > 0) {
            pageProvider.setCurrentPage(currentPage.longValue());
        }
        if (currentOffset != null && currentOffset.longValue() >= 0) {
            pageProvider.setCurrentPageOffset(currentOffset.longValue());
        }

        return pageProvider;
    }

    @Override
    public PageProvider<?> getPageProvider(String name, PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage, Map<String, Serializable> properties,
            List<QuickFilter> quickFilters, Object... parameters) {
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, null,
                quickFilters, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Map<String, Serializable> properties, List<String> highlights, List<QuickFilter> quickFilters,
            Object... parameters) {
        return getPageProvider(name, (DocumentModel) null, sortInfos, pageSize, currentPage, properties, highlights,
                quickFilters, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage, Map<String, Serializable> properties,
            Object... parameters) {

        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, null, null,
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
        Class<? extends PageProvider<?>> klass = replacerMap.get(name);
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
            ret = klass.getDeclaredConstructor().newInstance();
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
        PageProviderDefinition desc = getPageProviderDefinitionOrElseThrow(name);
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, null, null,
                parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Map<String, Serializable> properties, List<String> highlights,
            List<QuickFilter> quickFilters, Object... parameters) {
        PageProviderDefinition desc = getPageProviderDefinitionOrElseThrow(name);
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, highlights,
                quickFilters, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Long currentOffset, Map<String, Serializable> properties,
            List<String> highlights, List<QuickFilter> quickFilters, Object... parameters) {
        PageProviderDefinition desc = getPageProviderDefinitionOrElseThrow(name);
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, currentOffset, properties,
                highlights, quickFilters, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Map<String, Serializable> properties, List<QuickFilter> quickFilters,
            Object... parameters) {
        PageProviderDefinition desc = getPageProviderDefinitionOrElseThrow(name);
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, quickFilters,
                parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters) {
        return getPageProvider(name, (DocumentModel) null, sortInfos, pageSize, currentPage, properties, parameters);
    }

    @Override
    public void registerPageProviderDefinition(PageProviderDefinition desc) {
        if (desc != null) {
            programmaticDefinitions.put(desc.getName(), desc);
        }
    }

    @Override
    public void unregisterPageProviderDefinition(PageProviderDefinition desc) {
        if (desc != null) {
            programmaticDefinitions.remove(desc.getName());
        }
    }

}
