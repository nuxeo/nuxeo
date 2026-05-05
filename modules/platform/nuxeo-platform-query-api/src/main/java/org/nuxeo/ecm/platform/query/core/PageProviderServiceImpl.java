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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderClassReplacerDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderType;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
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
     * @since 2021.8
     */
    public static final String ELASTICSEARCH_NXQL_PAGE_PROVIDER_CLASS_NAME = "org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider";

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
    public PageProvider<?> getPageProvider(@Nonnull PageProviderSpec spec) {
        var desc = spec.definition();
        PageProvider<?> pageProvider = newPageProviderInstance(spec.name(), desc);
        // Definition properties are already merged into spec.properties() by the builder.
        Map<String, Serializable> allProps = new HashMap<>(spec.properties());
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
            ret = klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(String.format(
                    "Cannot create an instance of class %s for page provider definition" + " with name '%s'",
                    klass.getName(), name), e);
        }
        return ret;
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
    public void start(ComponentContext context) {
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

    @Override
    public PageProviderType getPageProviderType(PageProvider<?> pageProvider) {
        try {
            if (Class.forName(ELASTICSEARCH_NXQL_PAGE_PROVIDER_CLASS_NAME).isInstance(pageProvider)) {
                return PageProviderType.ELASTIC;
            }
        } catch (ClassNotFoundException e) {
            // just return default
        }
        return PageProviderType.DEFAULT;
    }

}
