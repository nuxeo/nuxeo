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
 *     Benoit Delbosc
 *
 */

package org.nuxeo.ecm.platform.query.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
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

    @XNode("field")
    protected FieldDescriptor field;

    @XNode("@id")
    protected String id;

    @XNode("@parameter")
    protected String parameter;

    @XNode("@type")
    protected String type;

    @XNodeMap(value = "properties/property", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties = new HashMap<>();

    @XNodeList(value = "ranges/range", type = ArrayList.class, componentType = AggregateRangeDescriptor.class)
    protected List<AggregateRangeDefinition> aggregateRanges = new ArrayList<>();

    // cache map
    private Map<String, Integer> aggregateRangeDefinitionOrderMap;

    @XNodeList(value = "dateRanges/dateRange", type = ArrayList.class, componentType = AggregateRangeDateDescriptor.class)
    protected List<AggregateRangeDateDefinition> aggregateDateRanges = new ArrayList<>();

    // cache map
    protected Map<String, Integer> aggregateDateRangeDefinitionOrderMap;

    @Override
    public Map<String, Integer> getAggregateDateRangeDefinitionOrderMap() {
        if (aggregateDateRangeDefinitionOrderMap == null) {
            aggregateDateRangeDefinitionOrderMap = new HashMap<>(getDateRanges().size());
            for (int i = 0; i < getDateRanges().size(); i++) {
                aggregateDateRangeDefinitionOrderMap.put(getDateRanges().get(i).getKey(), i);
            }
        }
        return aggregateDateRangeDefinitionOrderMap;
    }

    @Override
    public Map<String, Integer> getAggregateRangeDefinitionOrderMap() {
        if (aggregateRangeDefinitionOrderMap == null) {
            aggregateRangeDefinitionOrderMap = new HashMap<>(getRanges().size());
            for (int i = 0; i < getRanges().size(); i++) {
                aggregateRangeDefinitionOrderMap.put(getRanges().get(i).getKey(), i);
            }
        }
        return aggregateRangeDefinitionOrderMap;
    }

    @Override
    public List<AggregateRangeDateDefinition> getDateRanges() {
        return Collections.unmodifiableList(aggregateDateRanges);
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
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public List<AggregateRangeDefinition> getRanges() {
        return Collections.unmodifiableList(aggregateRanges);
    }

    @Override
    public PredicateFieldDefinition getSearchField() {
        return field;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setDateRanges(List<AggregateRangeDateDefinition> ranges) {
        if (ranges != null) {
            aggregateDateRanges.addAll(ranges);
            aggregateDateRangeDefinitionOrderMap = null;
        }
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
        properties.put(name, value);
    }

    @Override
    public void setRanges(List<AggregateRangeDefinition> ranges) {
        if (ranges != null) {
            aggregateRanges.addAll(ranges);
            aggregateRangeDefinitionOrderMap = null;
        }
    }

    @Override
    public void setSearchField(PredicateFieldDefinition field) {
        this.field = (FieldDescriptor) field;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public AggregateDescriptor clone() {
        AggregateDescriptor clone = new AggregateDescriptor();
        clone.id = id;
        clone.parameter = parameter;
        clone.type = type;
        if (field != null) {
            clone.field = field.clone();
        }
        if (properties != null) {
            clone.properties = new HashMap<>(properties);
        }
        if (aggregateRanges != null) {
            clone.aggregateRanges = new ArrayList<>(aggregateRanges.size());
            clone.aggregateRanges.addAll(aggregateRanges);
        }
        if (aggregateDateRanges != null) {
            clone.aggregateDateRanges = new ArrayList<>(aggregateDateRanges.size());
            clone.aggregateDateRanges.addAll(aggregateDateRanges);
        }
        return clone;
    }

}
