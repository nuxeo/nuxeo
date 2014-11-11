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

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.SchemaNames;

/**
 * The implementation of a Schema
 */
public class SchemaImpl extends ComplexTypeImpl implements Schema {

    private static final long serialVersionUID = 1L;

    private final Map<String, Type> types = new HashMap<String, Type>();

    /**
     * Constructor for a schema. Its types (fields) are then added through
     * {@link #registerType}.
     */
    public SchemaImpl(String name, Namespace ns) {
        super(null, SchemaNames.SCHEMAS, name,
                ns == null ? Namespace.DEFAULT_NS : ns);
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

}
