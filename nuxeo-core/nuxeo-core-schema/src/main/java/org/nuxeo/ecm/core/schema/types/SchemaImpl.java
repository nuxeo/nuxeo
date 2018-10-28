/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

/**
 * The implementation of a Schema
 */
public class SchemaImpl extends ComplexTypeImpl implements Schema {

    private static final long serialVersionUID = 1L;

    private final Map<String, Type> types = new HashMap<String, Type>();

    public boolean isVersionWritabe;

    /**
     * Constructor for a schema. Its types (fields) are then added through {@link #registerType}.
     */
    public SchemaImpl(String name, Namespace ns) {
        this(name, ns, false);
    }

    public SchemaImpl(String name, Namespace ns, boolean isVersionWritabe) {
        super(null, SchemaNames.SCHEMAS, name, ns == null ? Namespace.DEFAULT_NS : ns);
        this.isVersionWritabe = isVersionWritabe;
    }

    /**
     * Create a schema from a ComplexType
     *
     * @since 5.7
     * @param complexType
     * @param name
     * @param ns
     */
    public SchemaImpl(ComplexType complexType, String name, Namespace ns, boolean isVersionWritabe) {
        super(null, SchemaNames.SCHEMAS, name, ns);
        this.isVersionWritabe = isVersionWritabe;
        if (complexType != null) {
            for (Field field : complexType.getFields()) {
                QName fieldname = QName.valueOf(field.getName().getLocalName(), ns.prefix);
                Type type = field.getType();
                String defaultValue = type.encode(field.getDefaultValue());
                Set<Constraint> constraint = field.getConstraints();
                FieldImpl newField = new FieldImpl(fieldname, this, type, defaultValue, 0, constraint);
                newField.setConstant(field.isConstant());
                addField(newField);
            }
        }
    }

    @Override
    public Type getType(String typeName) {
        return types.get(typeName);
    }

    @Override
    public Type[] getTypes() {
        return types.values().toArray(new Type[types.size()]);
    }

    @Override
    public void registerType(Type type) {
        types.put(type.getName(), type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

    @Override
    public Schema getSchema() {
        return this;
    }

    @Override
    public boolean isVersionWritabe() {
        return isVersionWritabe;
    }

}
