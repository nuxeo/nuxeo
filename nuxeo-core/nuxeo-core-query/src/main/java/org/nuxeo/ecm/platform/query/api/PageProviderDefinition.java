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
     * @since 5.6
     */
    PageProviderDefinition clone();

    /**
     * @since 6.0
     */
    List<AggregateDefinition> getAggregates();
}
