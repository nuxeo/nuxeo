/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.nuxeo.ecm.core.api.SortInfo;

/**
 * Page provider descriptor interface handling all attributes common to a {@link PageProvider} generation.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface PageProviderDefinition extends Serializable {

    String getName();

    /**
     * @since 6.0
     */
    void setName(String name);

    boolean isEnabled();

    /**
     * @since 5.6
     */
    void setEnabled(boolean enabled);

    Map<String, String> getProperties();

    String[] getQueryParameters();

    boolean getQuotePatternParameters();

    boolean getEscapePatternParameters();

    void setPattern(String pattern);

    String getPattern();

    WhereClauseDefinition getWhereClause();

    /**
     * Returns the search document type used for wher clause, aggregates and named parameters.
     *
     * @since 6.0
     */
    String getSearchDocumentType();

    boolean isSortable();

    List<SortInfo> getSortInfos();

    String getSortInfosBinding();

    long getPageSize();

    String getPageSizeBinding();

    Long getMaxPageSize();

    /**
     * Returns the list of page size options to present to users.
     * <p>
     * Uses an hardcoded list of values, and adds up the page provider initial page size to it.
     *
     * @since 7.3
     */
    List<Long> getPageSizeOptions();

    /**
     * @since 5.6
     */
    PageProviderDefinition clone();

    /**
     * @since 6.0
     */
    List<AggregateDefinition> getAggregates();

    /**
     * @since 7.4
     */
    public boolean isUsageTrackingEnabled();

    /**
     * @since 8.4
     */
    List<QuickFilterDefinition> getQuickFilters();

}
