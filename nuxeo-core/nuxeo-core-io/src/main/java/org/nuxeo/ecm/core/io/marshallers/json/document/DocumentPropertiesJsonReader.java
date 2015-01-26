/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.impl.PropertyFactory;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;

import javax.inject.Inject;

/**
 * Convert Json as {@link List<Property>}.
 * <p>
 * Format is:
 *
 * <pre>
 * {
 *   "schema1Prefix:stringProperty": "stringPropertyValue", <-- each property may be marshall as object if a resolver is associated with that property and if a marshaller exists for the object, in this case, the resulting property will have the corresponding reference value.
 *   "schema1Prefix:booleanProperty": true|false,
 *   "schema2Prefix:integerProperty": 123,
 *   ...
 *   "schema3Prefix:complexProperty": {
 *      "subProperty": ...,
 *      ...
 *   },
 *   "schema4Prefix:listProperty": [
 *      ...
 *   ]
 * }
 * </pre>
 *
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentPropertiesJsonReader extends AbstractJsonReader<List<Property>> {

    public static String DEFAULT_SCHEMA_NAME = "DEFAULT_SCHEMA_NAME";

    @Inject
    private SchemaManager schemaManager;

    @Override
    public List<Property> read(JsonNode jn) throws IOException {
        List<Property> properties = new ArrayList<Property>();
        Iterator<Entry<String, JsonNode>> propertyNodes = jn.getFields();
        while (propertyNodes.hasNext()) {
            Entry<String, JsonNode> propertyNode = propertyNodes.next();
            String propertyName = propertyNode.getKey();
            Field field = null;
            Property parent = null;
            if (propertyName.contains(":")) {
                field = schemaManager.getField(propertyName);
                if (field == null) {
                    continue;
                }
                parent = new DocumentPartImpl(field.getType().getSchema());
            } else {
                String shemaName = ctx.getParameter(DEFAULT_SCHEMA_NAME);
                Schema schema = schemaManager.getSchema(shemaName);
                field = schema.getField(propertyName);
                parent = new DocumentPartImpl(schema);
            }
            Property property = readProperty(parent, field, propertyNode.getValue());
            if (property != null) {
                properties.add(property);
            }
        }
        return properties;
    }

    protected Property readProperty(Property parent, Field field, JsonNode jn) throws IOException {
        Property property = PropertyFactory.createProperty(parent, field, 0);
        if (jn.isNull()) {
            property.setValue(null);
        } else if (property.isScalar()) {
            fillScalarProperty(property, jn);
        } else if (property.isList()) {
            fillListProperty(property, jn);
        } else {
            if (!(property instanceof BlobProperty)) {
                fillComplexProperty(property, jn);
            }
        }
        return property;
    }

    private void fillScalarProperty(Property property, JsonNode jn) throws IOException {
        Type type = property.getType();
        Object value = null;
        value = getPropertyValue(type, jn);
        property.setValue(value);
    }

    private Object getPropertyValue(Type type, JsonNode jn) throws IOException {
        Object value;
        if (jn.isNull()) {
            value = null;
        } else if (type instanceof BooleanType) {
            value = jn.getValueAsBoolean();
        } else if (type instanceof LongType) {
            value = jn.getValueAsLong();
        } else if (type instanceof DoubleType) {
            value = jn.getValueAsDouble();
        } else if (type instanceof IntegerType) {
            value = jn.getValueAsInt();
        } else if (type instanceof BinaryType) {
            value = jn.getBinaryValue();
        } else {
            value = type.decode(jn.getValueAsText());
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private void fillListProperty(Property property, JsonNode jn) throws IOException {
        ListType listType = (ListType) property.getType();
        if (property instanceof ArrayProperty) {
            Type type = listType.getFieldType();
            @SuppressWarnings("rawtypes")
            List values = new ArrayList();
            JsonNode elNode = null;
            Iterator<JsonNode> it = jn.getElements();
            while (it.hasNext()) {
                elNode = it.next();
                Object value = getPropertyValue(type, elNode);
                values.add(value);
            }
            property.setValue(values);
        } else {
            JsonNode elNode = null;
            Iterator<JsonNode> it = jn.getElements();
            while (it.hasNext()) {
                elNode = it.next();
                Property child = readProperty(property, listType.getField(), elNode);
                property.addValue(child.getValue());
            }
        }
    }

    private void fillComplexProperty(Property property, JsonNode jn) throws IOException {
        Entry<String, JsonNode> elNode = null;
        Iterator<Entry<String, JsonNode>> it = jn.getFields();
        ComplexProperty complexProperty = (ComplexProperty) property;
        ComplexType type = complexProperty.getType();
        while (it.hasNext()) {
            elNode = it.next();
            String elName = elNode.getKey();
            Field field = type.getField(elName);
            if (field != null) {
                Property child = readProperty(property, field, elNode.getValue());
                property.setValue(elName, child.getValue());
            }
        }
    }

}
