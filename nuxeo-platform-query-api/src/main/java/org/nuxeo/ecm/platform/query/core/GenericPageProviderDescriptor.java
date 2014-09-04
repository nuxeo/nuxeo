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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;

/**
 * Page provider descriptor accepting a custom class name. The expected
 * interface is {@link ContentViewPageProvider}, all other attributes are
 * common to other page provider descriptors.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("genericPageProvider")
public class GenericPageProviderDescriptor extends BasePageProviderDescriptor
        implements PageProviderDefinition {

    private static final long serialVersionUID = 1L;

    @XNode("@class")
    protected Class<PageProvider<?>> klass;

    public Class<PageProvider<?>> getPageProviderClass() {
        return klass;
    }

    /**
     * @since 5.6
     */
    public GenericPageProviderDescriptor clone() {
        GenericPageProviderDescriptor clone = new GenericPageProviderDescriptor();
        clone.name = getName();
        clone.enabled = isEnabled();
        clone.klass = getPageProviderClass();
        Map<String, String> props = getProperties();
        if (props != null) {
            clone.properties = new HashMap<String, String>();
            clone.properties.putAll(props);
        }
        String[] params = getQueryParameters();
        if (params != null) {
            clone.queryParameters = params.clone();
        }
        clone.pageSize = getPageSize();
        clone.pageSizeBinding = getPageSizeBinding();
        clone.maxPageSize = getMaxPageSize();
        clone.sortable = isSortable();
        if (sortInfos != null) {
            clone.sortInfos = new ArrayList<SortInfoDescriptor>();
            for (SortInfoDescriptor item : sortInfos) {
                clone.sortInfos.add(item.clone());
            }
        }
        clone.sortInfosBinding = getSortInfosBinding();
        clone.pattern = getPattern();
        clone.quotePatternParameters = getQuotePatternParameters();
        clone.escapePatternParameters = getEscapePatternParameters();
        if (whereClause != null) {
            clone.whereClause = whereClause.clone();
        }
        if (aggregates != null) {
            clone.aggregates = new ArrayList<AggregateDescriptor>();
            for (AggregateDescriptor agg : aggregates) {
                clone.aggregates.add(agg.clone());
            }
        }
        return clone;
    }
}
