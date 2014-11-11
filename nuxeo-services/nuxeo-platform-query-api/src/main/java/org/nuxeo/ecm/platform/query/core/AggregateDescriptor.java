/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 *
 */

package org.nuxeo.ecm.platform.query.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDateDefinition;
import org.nuxeo.ecm.platform.query.api.AggregateRangeDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;

/**
 * @since 6.0
 */
@XObject(value = "aggregate")
public class AggregateDescriptor implements AggregateDefinition {

    @XNodeList(value = "dateRanges/dateRange", type = ArrayList.class, componentType = AggregateRangeDateDescriptor.class)
    protected List<AggregateRangeDateDescriptor> aggregateDateRanges;

    @XNode(value = "properties")
    protected PropertiesDescriptor aggregateProperties = new PropertiesDescriptor();

    protected Map<String, Integer> aggregateDateRangeDefinitionOrderMap;

    private Map<String, Integer> aggregateRangeDefinitionOrderMap;

    @XNodeList(value = "ranges/range", type = ArrayList.class, componentType = AggregateRangeDescriptor.class)
    protected List<AggregateRangeDescriptor> aggregateRanges;

    @XNode("field")
    protected FieldDescriptor field;

    @XNode("@id")
    protected String id;

    @XNode("@parameter")
    protected String parameter;

    @XNode("@type")
    protected String type;

    @Override
    public AggregateDescriptor clone() {
        AggregateDescriptor clone = new AggregateDescriptor();
        clone.id = id;
        clone.parameter = parameter;
        clone.type = type;
        if (field != null) {
            clone.field = field.clone();
        }
        if (aggregateProperties != null) {
            clone.aggregateProperties = new PropertiesDescriptor();
            clone.aggregateProperties.properties.putAll(aggregateProperties.properties);
        }
        if (aggregateRanges != null) {
            clone.aggregateRanges = new ArrayList<AggregateRangeDescriptor>(
                    aggregateRanges.size());
            clone.aggregateRanges.addAll(aggregateRanges);
        }
        if (aggregateDateRanges != null) {
            clone.aggregateDateRanges = new ArrayList<AggregateRangeDateDescriptor>(
                    aggregateDateRanges.size());
            clone.aggregateDateRanges.addAll(aggregateDateRanges);
        }
        return clone;
    }

    @Override
    public Map<String, Integer> getAggregateDateRangeDefinitionOrderMap() {
        if (aggregateDateRangeDefinitionOrderMap == null) {
            aggregateDateRangeDefinitionOrderMap = new HashMap<String, Integer>(
                    getDateRanges().size());
            for (int i = 0; i < getDateRanges().size(); i++) {
                aggregateDateRangeDefinitionOrderMap.put(
                        getDateRanges().get(i).getKey(), i);
            }
        }
        return aggregateDateRangeDefinitionOrderMap;
    }

    @Override
    public Map<String, Integer> getAggregateRangeDefinitionOrderMap() {
        if (aggregateRangeDefinitionOrderMap == null) {
            aggregateRangeDefinitionOrderMap = new HashMap<String, Integer>(
                    getRanges().size());
            for (int i = 0; i < getRanges().size(); i++) {
                aggregateRangeDefinitionOrderMap.put(
                        getRanges().get(i).getKey(), i);
            }
        }
        return aggregateRangeDefinitionOrderMap;
    }

    @Override
    public List<AggregateRangeDateDefinition> getDateRanges() {
        @SuppressWarnings("unchecked")
        List<AggregateRangeDateDefinition> ret = (List<AggregateRangeDateDefinition>) (List<?>) aggregateDateRanges;
        return ret;
    }

    @Override
    public String getDocumentField() {
        return parameter;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, String> getProperties() {
        return aggregateProperties.getProperties();
    }

    @Override
    public List<AggregateRangeDefinition> getRanges() {
        @SuppressWarnings("unchecked")
        List<AggregateRangeDefinition> ret = (List<AggregateRangeDefinition>) (List<?>) aggregateRanges;
        return ret;
    }

    @Override
    public PredicateFieldDefinition getSearchField() {
        return field;
    }

    public String getType() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setDateRanges(List<AggregateRangeDateDefinition> ranges) {
        aggregateDateRanges = (List<AggregateRangeDateDescriptor>) (List<?>) ranges;
        this.aggregateDateRangeDefinitionOrderMap = null;
    }

    @Override
    public void setDocumentField(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setProperty(String name, String value) {
        aggregateProperties.properties.put(name, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setRanges(List<AggregateRangeDefinition> ranges) {
        aggregateRanges = (List<AggregateRangeDescriptor>) (List<?>) ranges;
        this.aggregateRangeDefinitionOrderMap = null;
    }

    @Override
    public void setSearchField(PredicateFieldDefinition field) {
        this.field = (FieldDescriptor) field;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

}
