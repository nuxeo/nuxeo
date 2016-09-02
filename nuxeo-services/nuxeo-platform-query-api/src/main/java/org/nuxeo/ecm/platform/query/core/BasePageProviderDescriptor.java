/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.platform.query.core;

import java.util.*;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;

/**
 * Base class for page provider descriptors.
 *
 * @since 6.0
 */
public abstract class BasePageProviderDescriptor {

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

    /**
     * @since 7.3
     */
    @XNodeList(value = "pageSizeOptions/option", type = ArrayList.class, componentType = Long.class)
    protected List<Long> pageSizeOptions;

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

    /**
     * @since 6.0
     */
    @XNode("searchDocumentType")
    protected String searchDocumentType;

    @XNode("pattern")
    public void setPattern(String pattern) {
        // remove new lines and following spaces
        if (pattern != null) {
            this.pattern = pattern.replaceAll("\r?\n\\s*", " ");
        }
    }

    /**
     * @since 6.0
     */
    @XNodeList(value = "aggregates/aggregate", type = ArrayList.class, componentType = AggregateDescriptor.class)
    protected List<AggregateDescriptor> aggregates;

    /**
     * @since 7.4
     */
    @XNode("trackUsage")
    protected boolean trackUsage = false;

    /**
     * @since 7.4
     */
    public boolean isUsageTrackingEnabled() {
        return trackUsage;
    }

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

    public List<Long> getPageSizeOptions() {
        List<Long> res = new ArrayList<Long>();
        if (pageSizeOptions == null || pageSizeOptions.isEmpty()) {
            res.addAll(Arrays.asList(5L, 10L, 20L, 30L, 40L, 50L));
        } else {
            res.addAll(pageSizeOptions);
        }
        long defaultPageSize = getPageSize();
        if (!res.contains(defaultPageSize)) {
            res.add(defaultPageSize);
        }
        Collections.sort(res);
        return res;
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

    public void setName(String name) {
        this.name = name;
    }

    public Long getMaxPageSize() {
        return maxPageSize;
    }

    /**
     * @since 7.10
     */
    public void setQuotePatternParameters(boolean quotePatternParameters) {
        this.quotePatternParameters = quotePatternParameters;
    }

    /**
     * @since 7.10
     */
    public void setEscapePatternParameters(boolean escapePatternParameters) {
        this.escapePatternParameters = escapePatternParameters;
    }

    /**
     * @since 6.0
     */
    @SuppressWarnings("unchecked")
    public List<AggregateDefinition> getAggregates() {
        return (List<AggregateDefinition>) (List<?>) aggregates;
    }

    /**
     * Returns the search document type used for wher clause, aggregates and named parameters.
     *
     * @since 6.0
     */
    public String getSearchDocumentType() {
        if (searchDocumentType == null) {
            // BBB
            WhereClauseDefinition wc = getWhereClause();
            if (wc != null) {
                return wc.getDocType();
            }
        }
        return searchDocumentType;
    }

    protected BasePageProviderDescriptor cloneDescriptor() {
        BasePageProviderDescriptor clone = newInstance();
        clone.name = getName();
        clone.enabled = isEnabled();
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
        clone.pageSizeOptions = getPageSizeOptions();
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
        clone.searchDocumentType = searchDocumentType;
        clone.trackUsage = trackUsage;
        return clone;
    }

    protected abstract BasePageProviderDescriptor newInstance();

}
