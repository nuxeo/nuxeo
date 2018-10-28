/*
 * (C) Copyright 2015-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.marshallers.json.document;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.impl.PropertyFactory;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.io.marshallers.json.AbstractJsonReader;
import org.nuxeo.ecm.core.io.registry.MarshallingException;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.SimpleType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;

import com.fasterxml.jackson.databind.JsonNode;

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
 * </p>
 *
 * @since 7.2
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentPropertiesJsonReader extends AbstractJsonReader<List<Property>> {

    public static final String DEFAULT_SCHEMA_NAME = "DEFAULT_SCHEMA_NAME";

    @Inject
    private SchemaManager schemaManager;

    @Override
    public List<Property> read(JsonNode jn) throws IOException {
        List<Property> properties = new ArrayList<>();
        Iterator<Entry<String, JsonNode>> propertyNodes = jn.fields();
        while (propertyNodes.hasNext()) {
            Entry<String, JsonNode> propertyNode = propertyNodes.next();
            String propertyName = propertyNode.getKey();
            Field field;
            Property parent;
            if (propertyName.contains(":")) {
                field = schemaManager.getField(propertyName);
                if (field == null) {
                    continue;
                }
                parent = new DocumentPartImpl(field.getDeclaringType().getSchema());
            } else {
                String shemaName = ctx.getParameter(DEFAULT_SCHEMA_NAME);
                Schema schema = schemaManager.getSchema(shemaName);
                if (schema == null) {
                    continue;
                }
                field = schema.getField(propertyName);
                parent = new DocumentPartImpl(schema);
            }
            if (field == null) {
                continue;
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
            } else {
                Blob blob = readEntity(Blob.class, Blob.class, jn);
                property.setValue(blob);
            }
        }
        return property;
    }

    private void fillScalarProperty(Property property, JsonNode jn) throws IOException {
        if ((property instanceof ArrayProperty) && jn.isArray()) {
            List<Object> values = new ArrayList<>();
            Iterator<JsonNode> it = jn.elements();
            JsonNode item;
            Type fieldType = ((ListType) property.getType()).getFieldType();
            while (it.hasNext()) {
                item = it.next();
                values.add(getScalarPropertyValue(property, item, fieldType));
            }
            property.setValue(castArrayPropertyValue(((SimpleType) fieldType).getPrimitiveType(), values));
        } else {
            property.setValue(getScalarPropertyValue(property, jn, property.getType()));
        }
    }

    @SuppressWarnings({ "unchecked" })
    private <T> T[] castArrayPropertyValue(Type type, List<Object> values) throws IOException {
        if (type instanceof StringType) {
            return values.toArray((T[]) Array.newInstance(String.class, values.size()));
        } else if (type instanceof BooleanType) {
            return values.toArray((T[]) Array.newInstance(Boolean.class, values.size()));
        } else if (type instanceof LongType) {
            return values.toArray((T[]) Array.newInstance(Long.class, values.size()));
        } else if (type instanceof DoubleType) {
            return values.toArray((T[]) Array.newInstance(Double.class, values.size()));
        } else if (type instanceof IntegerType) {
            return values.toArray((T[]) Array.newInstance(Integer.class, values.size()));
        } else if (type instanceof BinaryType) {
            return values.toArray((T[]) Array.newInstance(Byte.class, values.size()));
        } else if (type instanceof DateType) {
            return values.toArray((T[]) Array.newInstance(Calendar.class, values.size()));
        }
        throw new MarshallingException("Primitive type not found: " + type.getName());
    }

    private Object getScalarPropertyValue(Property property, JsonNode jn, Type type) throws IOException {
        Object value;
        if (jn.isObject()) {
            ObjectResolver resolver = type.getObjectResolver();
            if (resolver == null) {
                // Let's assume it is a blob of which content has to be stored in a string property.
                assert type.getSuperType() instanceof StringType;
                Blob blob = readEntity(Blob.class, Blob.class, jn);
                if (blob == null) {
                    throw new MarshallingException("Unable to parse the property " + property.getXPath());
                }
                return blob.getString();
            }
            Object object = null;
            for (Class<?> clazz : resolver.getManagedClasses()) {
                try {
                    object = readEntity(clazz, clazz, jn);
                } catch (Exception e) {
                    continue;
                }
            }
            if (object == null) {
                throw new MarshallingException("Unable to parse the property " + property.getXPath());
            }
            value = resolver.getReference(object);
            if (value == null) {
                throw new MarshallingException("Property " + property.getXPath()
                        + " value cannot be resolved by the matching resolver " + resolver.getName());
            }
        } else {
            value = getPropertyValue(((SimpleType) type).getPrimitiveType(), jn);
        }
        return value;
    }

    private Object getPropertyValue(Type type, JsonNode jn) throws IOException {
        Object value;
        if (jn.isNull()) {
            value = null;
        } else if (type instanceof BooleanType) {
            value = jn.asBoolean();
        } else if (type instanceof LongType) {
            value = jn.asLong();
        } else if (type instanceof DoubleType) {
            value = jn.asDouble();
        } else if (type instanceof IntegerType) {
            value = jn.asInt();
        } else if (type instanceof BinaryType) {
            value = jn.binaryValue();
        } else {
            value = type.decode(jn.asText());
        }
        return value;
    }

    private void fillListProperty(Property property, JsonNode jn) throws IOException {
        ListType listType = (ListType) property.getType();
        if (property instanceof ArrayProperty) {
            fillScalarProperty(property, jn);
        } else {
            JsonNode elNode;
            Iterator<JsonNode> it = jn.elements();
            while (it.hasNext()) {
                elNode = it.next();
                Property child = readProperty(property, listType.getField(), elNode);
                property.addValue(child.getValue());
            }
        }
    }

    private void fillComplexProperty(Property property, JsonNode jn) throws IOException {
        Entry<String, JsonNode> elNode;
        Iterator<Entry<String, JsonNode>> it = jn.fields();
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
