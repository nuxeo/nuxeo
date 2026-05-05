/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.platform.query.api;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static org.apache.commons.lang3.ArrayUtils.nullToEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.runtime.api.Framework;

/**
 * Specification of a {@link PageProvider} instantiation request, used by
 * {@link PageProviderService#getPageProvider(PageProviderSpec)}.
 * <p>
 * Instances are immutable and can only be created through the {@link Builder} returned by {@link #builder(String)}.
 *
 * @since 2025.20
 */
public class PageProviderSpec {

    // ----------------------------------------------------------------------------------------------------------------
    // SortInfo - constants for conversion from String literals (see PageProviderSpec#toSortInfos(List, List))
    // ----------------------------------------------------------------------------------------------------------------

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    // ----------------------------------------------------------------------------------------------------------------
    // Properties — keys for the page provider properties map (see Builder#property(String, Serializable))
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Property name used to pass a {@link org.nuxeo.ecm.core.api.CoreSession} to the page provider through
     * {@link Builder#property(String, java.io.Serializable) properties}.
     * <p>
     * When this property is set, {@link PageProviderService#getPageProvider(PageProviderSpec)} also substitutes any
     * occurrence of {@link #CURRENT_USER_PARAMETER_VALUE} or {@link #CURRENT_REPOSITORY_PARAMETER_VALUE} in
     * {@link #parameters()} with the session's principal name and repository name respectively.
     */
    public static final String CORE_SESSION_PROPERTY = "coreSession";

    // ----------------------------------------------------------------------------------------------------------------
    // Parameter values — placeholders for entries of the page provider parameters array (see Builder#parameters)
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Parameter placeholder substituted with the current user name when a {@link #CORE_SESSION_PROPERTY CoreSession}
     * property is provided.
     */
    public static final String CURRENT_USER_PARAMETER_VALUE = "$currentUser";

    /**
     * Parameter placeholder substituted with the current repository name when a {@link #CORE_SESSION_PROPERTY
     * CoreSession} property is provided.
     */
    public static final String CURRENT_REPOSITORY_PARAMETER_VALUE = "$currentRepository";

    protected final String name;

    protected final PageProviderDefinition definition;

    protected final DocumentModel searchDocument;

    protected final List<SortInfo> sortInfos;

    protected final Long pageSize;

    protected final Long currentPage;

    protected final Long currentPageOffset;

    protected final Map<String, Serializable> properties;

    protected final List<String> highlights;

    protected final List<QuickFilter> quickFilters;

    protected final Object[] parameters;

    protected PageProviderSpec(Builder builder) {
        this.name = builder.name;
        this.definition = builder.definition;
        this.searchDocument = builder.searchDocument;
        this.sortInfos = builder.sortInfos == null ? null : List.copyOf(builder.sortInfos);
        this.pageSize = builder.pageSize;
        this.currentPage = builder.currentPage;
        this.currentPageOffset = builder.currentPageOffset;
        this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(builder.properties));
        this.highlights = builder.highlights == null ? null : List.copyOf(builder.highlights);
        this.quickFilters = builder.quickFilters == null ? null : List.copyOf(builder.quickFilters);
        this.parameters = builder.parameters.toArray();
        // expand parameters
        if (this.properties.get(CORE_SESSION_PROPERTY) instanceof CoreSession coreSession) {
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i] instanceof String value) {
                    if (CURRENT_USER_PARAMETER_VALUE.equals(value)) {
                        parameters[i] = coreSession.getPrincipal().getName();
                    } else if (CURRENT_REPOSITORY_PARAMETER_VALUE.equals(value)) {
                        parameters[i] = coreSession.getRepositoryName();
                    }
                }
            }
        }
    }

    /**
     * Returns a new builder for the given page provider {@code name}, eagerly resolving its
     * {@link PageProviderDefinition} from the registered providers.
     *
     * @throws NuxeoException if no page provider definition is registered for {@code name}
     */
    public static Builder builder(String name) {
        var definition = Framework.getService(PageProviderService.class).getPageProviderDefinition(name);
        if (definition == null) {
            throw new NuxeoException("Could not resolve page provider with name '%s'".formatted(name), SC_BAD_REQUEST);
        }
        return new Builder(name, definition);
    }

    /**
     * Returns a new builder for the given page provider {@code definition}.
     */
    public static Builder builder(PageProviderDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Page provider definition must not be null");
        }
        return new Builder(definition.getName(), definition);
    }

    /**
     * Returns a new builder for the given page provider {@code name} and {@code definition}.
     */
    public static Builder builder(String name, PageProviderDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Page provider definition must not be null");
        }
        if (name == null) {
            name = definition.getName();
        }
        return new Builder(name, definition);
    }

    @Nonnull
    public String name() {
        return name;
    }

    @Nonnull
    public PageProviderDefinition definition() {
        return definition;
    }

    @Nullable
    public DocumentModel searchDocument() {
        return searchDocument;
    }

    @Nullable
    public List<SortInfo> sortInfos() {
        return sortInfos;
    }

    @Nullable
    public Long pageSize() {
        return pageSize;
    }

    @Nullable
    public Long currentPage() {
        return currentPage;
    }

    @Nullable
    public Long currentPageOffset() {
        return currentPageOffset;
    }

    @Nonnull
    public Map<String, Serializable> properties() {
        return properties;
    }

    @Nullable
    public List<String> highlights() {
        return highlights;
    }

    @Nullable
    public List<QuickFilter> quickFilters() {
        return quickFilters;
    }

    @Nonnull
    public Object[] parameters() {
        return parameters.clone();
    }

    /**
     * Builds a list of {@link SortInfo} from the given parallel {@code sortBys} and {@code sortOrders} lists, where
     * each order entry is either {@code "asc"} or {@code "desc"} (case-insensitive).
     *
     * @since 2025.20
     */
    public static List<SortInfo> toSortInfos(List<String> sortBys, List<String> sortOrders) {
        if (sortBys == null) {
            return List.of();
        }
        List<SortInfo> sortInfos = new ArrayList<>(sortBys.size());
        for (int i = 0; i < sortBys.size(); i++) {
            String sort = sortBys.get(i);
            if (StringUtils.isNotBlank(sort)) {
                boolean sortAscending = sortOrders != null && sortOrders.size() > i
                        && ASC.equalsIgnoreCase(sortOrders.get(i));
                sortInfos.add(new SortInfo(sort, sortAscending));
            }
        }
        return sortInfos;
    }

    public static class Builder {

        protected final String name;

        protected final PageProviderDefinition definition;

        protected final Map<String, Serializable> properties = new LinkedHashMap<>();

        protected final List<Object> parameters = new ArrayList<>();

        protected DocumentModel searchDocument;

        protected Collection<SortInfo> sortInfos;

        protected Long pageSize;

        protected Long currentPage;

        protected Long currentPageOffset;

        protected Collection<String> highlights;

        protected Collection<QuickFilter> quickFilters;

        protected Builder(String name, PageProviderDefinition definition) {
            this.name = name;
            this.definition = definition;
            var definitionProperties = definition.getProperties();
            if (definitionProperties != null) {
                this.properties.putAll(definitionProperties);
            }
        }

        public Builder searchDocument(DocumentModel searchDocument) {
            this.searchDocument = searchDocument;
            return this;
        }

        /**
         * Sets the given {@code sortInfos} to this builder's sort information.
         */
        public Builder sortInfos(Collection<SortInfo> sortInfos) {
            this.sortInfos = sortInfos;
            return this;
        }

        /**
         * Appends the given {@code sortInfo} to this builder's sort information.
         */
        public Builder sortInfo(SortInfo sortInfo) {
            this.sortInfos = Objects.requireNonNullElseGet(this.sortInfos, ArrayList::new);
            this.sortInfos.add(sortInfo);
            return this;
        }

        /**
         * Appends sort information built from parallel {@code sortBy} and {@code sortOrder} lists. Entries in
         * {@code sortBy} are field paths; the matching entry in {@code sortOrder} is matched case-insensitively against
         * {@code "asc"} (any other value, including {@code null} or a missing entry, is treated as descending). Blank
         * {@code sortBy} entries are skipped.
         */
        public Builder sortInfosByFieldsAndOrders(List<String> sortBy, List<String> sortOrder) {
            return sortInfos(toSortInfos(sortBy, sortOrder));
        }

        public Builder pageSize(Long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder currentPage(Long currentPage) {
            this.currentPage = currentPage;
            return this;
        }

        public Builder currentPageOffset(Long currentPageOffset) {
            this.currentPageOffset = currentPageOffset;
            return this;
        }

        /**
         * Puts all the given {@code properties} into this builder's properties.
         */
        public Builder properties(Map<String, Serializable> properties) {
            this.properties.putAll(emptyIfNull(properties));
            return this;
        }

        /**
         * Puts the given {@code key}/{@code value} pair into this builder's properties.
         */
        public Builder property(String key, Serializable value) {
            this.properties.put(key, value);
            return this;
        }

        /**
         * Sets the given {@code highlights} to this builder's highlights.
         */
        public Builder highlights(Collection<String> highlights) {
            this.highlights = highlights;
            return this;
        }

        /**
         * Appends the given {@code highlight} to this builder's highlights.
         */
        public Builder highlight(String highlight) {
            this.highlights = Objects.requireNonNullElseGet(this.highlights, ArrayList::new);
            this.highlights.add(highlight);
            return this;
        }

        /**
         * Sets the given {@code quickFilters} to this builder's quick filters.
         */
        public Builder quickFilters(Collection<QuickFilter> quickFilters) {
            this.quickFilters = quickFilters;
            return this;
        }

        /**
         * Appends the given {@code quickFilter} to this builder's quick filters.
         */
        public Builder quickFilter(QuickFilter quickFilter) {
            this.quickFilters = Objects.requireNonNullElseGet(this.quickFilters, ArrayList::new);
            this.quickFilters.add(quickFilter);
            return this;
        }

        /**
         * Resolves and appends the given {@code quickFilterNames} to this builder's quick filters by looking them up on
         * the page provider definition. Names that don't match any declared quick filter are silently skipped.
         * <p>
         * The argument may be {@code null}, in which case this is a no-op.
         *
         * @since 2025.20
         */
        public Builder quickFiltersByNames(List<String> quickFilterNames) {
            if (quickFilterNames == null) {
                return this;
            }
            List<QuickFilter> declared = definition.getQuickFilters();
            if (isEmpty(declared)) {
                return this;
            }
            this.quickFilters = new ArrayList<>();
            for (String name : quickFilterNames) {
                for (QuickFilter quickFilter : declared) {
                    if (quickFilter.getName().equals(name)) {
                        this.quickFilters.add(quickFilter);
                        break;
                    }
                }
            }
            return this;
        }

        /**
         * Appends the given {@code parameters} to this builder's parameters.
         */
        public Builder parameters(Object... parameters) {
            // don't use List.of here, we can have null parameter
            this.parameters.addAll(Arrays.asList(nullToEmpty(parameters)));
            return this;
        }

        /**
         * Appends the given {@code parameters} to this builder's parameters.
         */
        public Builder parameters(Collection<?> parameters) {
            this.parameters.addAll(emptyIfNull(parameters));
            return this;
        }

        /**
         * Appends the given {@code parameter} to this builder's parameters.
         */
        public Builder parameter(Object parameter) {
            this.parameters.add(parameter);
            return this;
        }

        public PageProviderSpec build() {
            return new PageProviderSpec(this);
        }
    }
}
