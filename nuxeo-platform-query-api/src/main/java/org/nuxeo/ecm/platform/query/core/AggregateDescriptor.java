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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
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

    @XNode("jsonProperties")
    protected String jsonProperties;

/*    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    protected Map<String, String> properties = new HashMap<String, String>();*/

    @XNode(value = "properties")
    protected PropertiesDescriptor aggregateProperties = new PropertiesDescriptor();

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
    public String getPropertiesAsJson() {
        if (jsonProperties == null) {
            StringWriter out = new StringWriter();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                objectMapper.writeValue(out, aggregateProperties.getProperties());
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            out.flush();
            jsonProperties = out.toString();
        }
        return jsonProperties;
    }

    @Override
    public Map<String, String> getProperties() {
        return aggregateProperties.getProperties();
    }

    @Override
    public void setPropertiesAsJson(String json) {
        this.jsonProperties = json;
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
        clone.jsonProperties = jsonProperties;
        if (aggregateProperties != null) {
            clone.aggregateProperties = new PropertiesDescriptor();
            clone.aggregateProperties.properties.putAll(aggregateProperties.properties);
        }
        return clone;
    }
}
