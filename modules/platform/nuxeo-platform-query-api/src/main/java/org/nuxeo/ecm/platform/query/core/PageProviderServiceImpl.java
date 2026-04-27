/*
 * (C) Copyright 2010-2026 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderCheckRequest;
import org.nuxeo.ecm.platform.query.api.PageProviderCheckResult;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.query.nxql.SearchServicePageProvider;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class PageProviderServiceImpl extends DefaultComponent implements PageProviderService {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(PageProviderServiceImpl.class);

    public static final String PROVIDER_EP = "providers";

    // @since 6.0
    public static final String REPLACER_EP = "replacers";

    protected Map<String, PageProviderDefinition> providers;

    protected Map<String, Class<? extends PageProvider<?>>> replacers;

    @Override
    public PageProviderDefinition getPageProviderDefinition(String name) {
        PageProviderDefinition def = providers.get(name);
        if (def == null) {
            return null;
        }
        return def.clone();
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

        if (sortInfos != null) {
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

    protected PageProvider<?> newPageProviderInstance(String name, PageProviderDefinition desc) {
        PageProvider<?> ret;
        if (desc instanceof CoreQueryPageProviderDescriptor) {
            ret = newCoreQueryPageProviderInstance(name);
        } else if (desc instanceof GenericPageProviderDescriptor descriptor) {
            Class<PageProvider<?>> klass = descriptor.getPageProviderClass();
            ret = newPageProviderInstance(name, klass);
        } else if (desc instanceof SearchServicePageProviderDescriptor) {
            ret = new SearchServicePageProvider();
        } else {
            throw new NuxeoException(String.format("Invalid page provider definition with name '%s'", name));
        }
        ret.setName(name);
        ret.setDefinition(desc);
        return ret;
    }

    protected PageProvider<?> newCoreQueryPageProviderInstance(String name) {
        PageProvider<?> ret;
        Class<? extends PageProvider<?>> klass = replacers.get(name);
        if (klass == null) {
            ret = new CoreQueryDocumentPageProvider();
        } else {
            ret = newPageProviderInstance(name, klass);
        }
        return ret;
    }

    protected PageProvider<?> newPageProviderInstance(String name, Class<? extends PageProvider<?>> klass) {
        if (klass == null) {
            throw new NuxeoException(String.format(
                    "Cannot find class for page provider definition with name: '%s', check ERROR logs at startup",
                    name));
        }
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(
                    String.format("Cannot create an instance of class: %s for page provider definition with name: '%s'",
                            klass.getName(), name),
                    e);
        }
    }

    @Override
    public PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Long currentOffset, Map<String, Serializable> properties,
            List<String> highlights, List<QuickFilter> quickFilters, Object... parameters) {
        PageProviderDefinition desc = providers.get(name);
        if (desc == null) {
            throw new NuxeoException(String.format("Could not resolve page provider with name '%s'", name));
        }
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, currentOffset, properties,
                highlights, quickFilters, parameters);
    }

    @Override
    public PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos,
            Long pageSize, Long currentPage, Map<String, Serializable> properties, List<QuickFilter> quickFilters,
            Object... parameters) {
        PageProviderDefinition desc = providers.get(name);
        if (desc == null) {
            throw new NuxeoException(String.format("Could not resolve page provider with name '%s'", name));
        }
        return getPageProvider(name, desc, searchDocument, sortInfos, pageSize, currentPage, properties, quickFilters,
                parameters);
    }

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.PAGE_PROVIDER;
    }

    @Override
    public void start(ComponentContext context) {
        providers = this.<PageProviderDefinition> getDescriptors(PROVIDER_EP)
                        .stream()
                        .filter(PageProviderDefinition::isEnabled)
                        .collect(Collectors.toMap(PageProviderDefinition::getName, Function.identity()));
        replacers = this.<PageProviderClassReplacerDescriptor> getDescriptors(REPLACER_EP)
                        .stream()
                        .filter(PageProviderClassReplacerDescriptor::isEnabled)
                        .<PageProviderReplacerWithName> mapMulti((descriptor, consumer) -> {
                            var pageProviderClass = descriptor.getPageProviderClass();
                            for (var replacedName : descriptor.getPageProviderNames()) {
                                consumer.accept(new PageProviderReplacerWithName(replacedName, pageProviderClass));
                            }
                        })
                        .collect(Collectors.toMap(PageProviderReplacerWithName::replacedName,
                                PageProviderReplacerWithName::providerClass, (a, b) -> {
                                    log.warn("The PageProvider: {} is overriding: {}, check your contributions", b, a);
                                    return b;
                                }));
    }

    @Override
    public void stop(ComponentContext context) {
        providers = null;
        replacers = null;
    }

    @Override
    public Set<String> getPageProviderDefinitionNames() {
        return Set.copyOf(providers.keySet());
    }

    @Override
    public PageProviderCheckResult runPageProviderCheck(PageProviderCheckRequest request) {
        var executionsResult = new LinkedHashMap<String, PageProviderCheckResult.Execution>();
        List<SortInfo> orders = null;
        for (var entry : request.executions().entrySet()) {
            var executionRequest = entry.getValue();
            var pageProvider = getPageProvider(request.name(), null, null, request.pageSize(), 0L, null,
                    executionRequest.properties(), null, null, executionRequest.parameters());
            var watch = StopWatch.createStarted();
            var result = pageProvider.getCurrentPage();
            watch.stop();
            executionsResult.put(entry.getKey(),
                    new PageProviderCheckResult.Execution(watch.getDuration(), pageProvider.getResultsCount(),
                            pageProvider.getResultsCountLimit(), result.stream().map(request.resultMapper()).toList()));
            orders = pageProvider.getSortInfos();
        }
        return new PageProviderCheckResult(request.name(), orders, request.pageSize(), executionsResult);
    }

    record PageProviderReplacerWithName(String replacedName, Class<? extends PageProvider<?>> providerClass) {
    }
}
