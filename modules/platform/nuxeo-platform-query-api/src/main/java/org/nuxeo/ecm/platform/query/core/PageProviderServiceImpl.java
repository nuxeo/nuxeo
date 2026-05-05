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

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderCheckRequest;
import org.nuxeo.ecm.platform.query.api.PageProviderCheckResult;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
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
    public PageProvider<?> getPageProvider(@Nonnull PageProviderSpec spec) {
        var desc = spec.definition();
        if (desc == null) {
            desc = providers.get(spec.name());
            if (desc == null) {
                throw new NuxeoException("Could not resolve page provider with name '%s'".formatted(spec.name()));
            }
        }
        PageProvider<?> pageProvider = newPageProviderInstance(spec.name(), desc);
        // Definition properties are already merged into spec.properties() by the builder when the definition was set
        // there. When the definition was resolved late (by name) we still need to fold them in here, with explicit
        // spec-provided properties winning.
        Map<String, Serializable> allProps = new HashMap<>();
        if (spec.definition() == null) {
            Map<String, String> localProps = desc.getProperties();
            if (localProps != null) {
                allProps.putAll(localProps);
            }
        }
        allProps.putAll(spec.properties());
        pageProvider.setProperties(allProps);
        pageProvider.setSortable(desc.isSortable());
        pageProvider.setParameters(spec.parameters());
        pageProvider.setPageSizeOptions(desc.getPageSizeOptions());
        if (spec.searchDocument() != null) {
            pageProvider.setSearchDocumentModel(spec.searchDocument());
        }

        Long maxPageSize = desc.getMaxPageSize();
        if (maxPageSize != null) {
            pageProvider.setMaxPageSize(maxPageSize.longValue());
        }

        if (spec.sortInfos() != null) {
            pageProvider.setSortInfos(spec.sortInfos());
        }

        if (spec.quickFilters() != null) {
            pageProvider.setQuickFilters(spec.quickFilters());
        }

        if (spec.highlights() != null) {
            pageProvider.setHighlights(spec.highlights());
        }

        var pageSize = spec.pageSize();
        if (pageSize == null || pageSize.longValue() < 0) {
            pageProvider.setPageSize(desc.getPageSize());
        } else {
            pageProvider.setPageSize(pageSize.longValue());
        }
        var currentPage = spec.currentPage();
        if (currentPage != null && currentPage.longValue() > 0) {
            pageProvider.setCurrentPage(currentPage.longValue());
        }
        var currentOffset = spec.currentPageOffset();
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
            throw new NuxeoException("Invalid page provider definition with name '%s'".formatted(name));
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
            throw new NuxeoException(
                    "Cannot find class for page provider definition with name: '%s', check ERROR logs at startup".formatted(
                            name));
        }
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(
                    "Cannot create an instance of class: %s for page provider definition with name: '%s'".formatted(
                            klass.getName(), name),
                    e);
        }
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
            var pageProvider = getPageProvider(PageProviderSpec.builder(request.name())
                                                               .pageSize(request.pageSize())
                                                               .currentPage(0L)
                                                               .properties(executionRequest.properties())
                                                               .parameters(executionRequest.parameters())
                                                               .build());
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
