/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;

/**
 * @since 5.9.6
 */
public class BasePageProviderDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties = new HashMap<String, String>();

    @XNodeList(value = "parameter", type = String[].class, componentType = String.class)
    protected String[] queryParameters;

    @XNode("pageSize")
    protected long pageSize = 0;

    @XNode("pageSizeBinding")
    protected String pageSizeBinding;

    @XNode("maxPageSize")
    protected Long maxPageSize;

    @XNode("sortable")
    protected boolean sortable = true;

    @XNodeList(value = "sort", type = ArrayList.class, componentType = SortInfoDescriptor.class)
    protected List<SortInfoDescriptor> sortInfos;

    @XNode("sortInfosBinding")
    protected String sortInfosBinding;

    protected String pattern;

    @XNode("pattern@quoteParameters")
    protected boolean quotePatternParameters = true;

    @XNode("pattern@escapeParameters")
    protected boolean escapePatternParameters = true;

    @XNode("whereClause")
    protected WhereClauseDescriptor whereClause;

    @XNode("pattern")
    public void setPattern(String pattern) {
        // remove new lines and following spaces
        if (pattern != null) {
            this.pattern = pattern.replaceAll("\r?\n\\s*", " ");
        }
    }

    @XNodeList(value = "aggregates/aggregate", type = ArrayList.class, componentType = AggregateDescriptor.class)
    protected List<AggregateDescriptor> aggregates;

    public boolean getQuotePatternParameters() {
        return quotePatternParameters;
    }

    public boolean getEscapePatternParameters() {
        return escapePatternParameters;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String[] getQueryParameters() {
        return queryParameters;
    }

    public String getPattern() {
        return pattern;
    }

    public WhereClauseDefinition getWhereClause() {
        return whereClause;
    }

    public boolean isSortable() {
        return sortable;
    }

    public List<SortInfo> getSortInfos() {
        List<SortInfo> res = new ArrayList<SortInfo>();
        if (sortInfos != null) {
            for (SortInfoDescriptor sortInfo : sortInfos) {
                res.add(sortInfo.getSortInfo());
            }
        }
        return res;
    }

    public long getPageSize() {
        return pageSize;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPageSizeBinding() {
        return pageSizeBinding;
    }

    public String getSortInfosBinding() {
        return sortInfosBinding;
    }

    public String getName() {
        return name;
    }

    public Long getMaxPageSize() {
        return maxPageSize;
    }

    /**
     * @since 5.9.6
     */
    @SuppressWarnings("unchecked")
    public List<AggregateDefinition> getAggregates() {
        return (List<AggregateDefinition>) (List<?>) aggregates;
    }

}
