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
package org.nuxeo.ecm.platform.query.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface PageProviderService extends Serializable {

    /**
     * Name of the search document model context map property holding named parameters.
     *
     * @since 7.1
     */
    String NAMED_PARAMETERS = "namedParameters";

    /**
     * Returns a named page provider definition.
     * <p>
     * Useful to share the definition between the page provider service, and the content view service (as content views
     * can reference a named page provider that is already registered instead of redefining it).
     *
     * @since 5.4
     * @param name the page provider name
     * @return the page provider definition or null, if no page provider with this name was found.
     */
    PageProviderDefinition getPageProviderDefinition(String name);

    /**
     * Returns an instance of page provider with given name and definition.
     * <p>
     * Useful to share the definition between the page provider service, and the content view service (as content views
     * can reference a named page provider that is already registered instead of redefining it).
     * <p>
     * If not null, parameters sortInfos and pageSize will override information computed in the XML file. If not null,
     * currentPage will override default current page (0).
     *
     * @param name the name that will be set on the provider.
     * @param desc the definition used to build the provider instance.
     * @param searchDocument the search document to be used by the provider.
     * @param sortInfos sort information to set on the provider instance.
     * @param pageSize the provider page size.
     * @param currentPage the provider current page index.
     * @param properties the provider properties
     * @param parameters the provider parameters.
     * @return the page provider instance.
     * @since 5.7
     */
    PageProvider<?> getPageProvider(String name, PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage, Map<String, Serializable> properties,
            Object... parameters);

    /**
     * Returns an instance of page provider with given name.
     *
     * @param name the page provider name
     * @param sortInfos sort information to set on the provider instance.
     * @param pageSize the provider page size.
     * @param currentPage the provider current page index.
     * @param properties the provider properties
     * @param parameters the provider parameters.
     * @return the page provider instance.
     * @since 5.4
     */
    PageProvider<?> getPageProvider(String name, List<SortInfo> sortInfos, Long pageSize, Long currentPage,
            Map<String, Serializable> properties, Object... parameters);

    /**
     * Returns an instance of page provider with given name.
     *
     * @see #getPageProvider(String, PageProviderDefinition, DocumentModel, List, Long, Long, Map, Object...)
     * @since 5.7
     */
    PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos, Long pageSize,
            Long currentPage, Map<String, Serializable> properties, Object... parameters);

    /**
     * Returns an instance of page provider with given name.
     *
     * @see #getPageProvider(String, PageProviderDefinition, DocumentModel, List, Long, Long, Map, Object...)
     * @since 8.4
     */
    PageProvider<?> getPageProvider(String name, DocumentModel searchDocument, List<SortInfo> sortInfos, Long pageSize,
            Long currentPage, Map<String, Serializable> properties, List<QuickFilter> quickFilters,
            Object... parameters);

    /**
     * Returns an instance of page provider with given name.
     *
     * @see #getPageProvider(String, PageProviderDefinition, DocumentModel, List, Long, Long, Map, Object...)
     * @since 8.4
     */
    PageProvider<?> getPageProvider(String name, PageProviderDefinition desc, DocumentModel searchDocument,
            List<SortInfo> sortInfos, Long pageSize, Long currentPage, Map<String, Serializable> properties,
            List<QuickFilter> quickFilters, Object... parameters);

    /**
     * @since 6.0
     */
    void registerPageProviderDefinition(PageProviderDefinition desc);

    /**
     * @since 6.0
     */
    void unregisterPageProviderDefinition(PageProviderDefinition desc);

    /**
     * Returns all the registered page provider names, or an empty set if no page provider is registered.
     *
     * @since 6.0
     */
    Set<String> getPageProviderDefinitionNames();

}
