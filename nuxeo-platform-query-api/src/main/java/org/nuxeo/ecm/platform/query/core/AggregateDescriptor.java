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
 * @since 5.9.6
 */
@XObject(value = "aggregate")
public class AggregateDescriptor implements AggregateDefinition {

    @XNode("@id")
    protected String id;

    @XNode("@type")
    protected String type;

    @XNode("@parameter")
    protected String parameter;

    @XNode("field")
    protected FieldDescriptor field;

    @XNode(value = "properties")
    protected PropertiesDescriptor aggregateProperties = new PropertiesDescriptor();

    @XNodeList(value = "ranges/range", type = ArrayList.class, componentType = AggregateRangeDescriptor.class)
    protected List<AggregateRangeDescriptor> aggregateRanges;

    @XNodeList(value = "dateRanges/dateRange", type = ArrayList.class, componentType = AggregateRangeDateDescriptor.class)
    protected List<AggregateRangeDateDescriptor> aggregateDateRanges;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setProperty(String name, String value) {
        aggregateProperties.properties.put(name, value);
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
    @SuppressWarnings("unchecked")
    public void setRanges(List<AggregateRangeDefinition> ranges) {
        aggregateRanges = (List<AggregateRangeDescriptor>) (List<?>) ranges;
    }

    @Override
    public List<AggregateRangeDateDefinition> getDateRanges() {
        @SuppressWarnings("unchecked")
        List<AggregateRangeDateDefinition> ret = (List<AggregateRangeDateDefinition>) (List<?>) aggregateDateRanges;
        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setDateRanges(List<AggregateRangeDateDefinition> ranges) {
        aggregateDateRanges = (List<AggregateRangeDateDescriptor>) (List<?>) ranges;
    }

    @Override
    public String getDocumentField() {
        return parameter;
    }

    @Override
    public void setDocumentField(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public PredicateFieldDefinition getSearchField() {
        return field;
    }

    @Override
    public void setSearchField(PredicateFieldDefinition field) {
        this.field = (FieldDescriptor) field;
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
        if (aggregateProperties != null) {
            clone.aggregateProperties = new PropertiesDescriptor();
            clone.aggregateProperties.properties
                    .putAll(aggregateProperties.properties);
        }
        if (aggregateRanges != null) {
            clone.aggregateRanges = new ArrayList<AggregateRangeDescriptor>(aggregateRanges.size());
            clone.aggregateRanges.addAll(aggregateRanges);
        }
        if (aggregateDateRanges != null) {
            clone.aggregateDateRanges = new ArrayList<AggregateRangeDateDescriptor>(aggregateDateRanges.size());
            clone.aggregateDateRanges.addAll(aggregateDateRanges);
        }
        return clone;
    }
}
