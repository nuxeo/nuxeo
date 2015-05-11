/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    /**
     * Constructor for a schema. Its types (fields) are then added through {@link #registerType}.
     */
    public SchemaImpl(String name, Namespace ns) {
        super(null, SchemaNames.SCHEMAS, name, ns == null ? Namespace.DEFAULT_NS : ns);
    }

    /**
     * Create a schema from a ComplexType
     *
     * @since 5.7
     * @param complexType
     * @param name
     * @param ns
     */
    public SchemaImpl(ComplexType complexType, String name, Namespace ns) {
        super(null, SchemaNames.SCHEMAS, name, ns == null ? Namespace.DEFAULT_NS : ns);
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

}
