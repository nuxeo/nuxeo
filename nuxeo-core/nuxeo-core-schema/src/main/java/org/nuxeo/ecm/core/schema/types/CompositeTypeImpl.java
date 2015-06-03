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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A Composite Type resolves fields for several schemas.
 */
public class CompositeTypeImpl extends ComplexTypeImpl implements CompositeType {

    private static final long serialVersionUID = 1L;

    /** The schemas for this composite type. */
    protected Map<String, Schema> schemas = new LinkedHashMap<String, Schema>();

    /**
     * Constructs a composite type. Schemas must include those from the super
     * type.
     */
    public CompositeTypeImpl(CompositeType superType, String schema,
            String name, List<Schema> schemaList) {
        super(superType, schema, name);
        if (schemaList == null) {
            schemaList = Collections.emptyList();
        }
        for (Schema s : schemaList) {
            schemas.put(s.getName(), s);
            for (Field field : s.getFields()) {
                addField(field);
            }
        }
    }

    @Override
    public boolean hasSchemas() {
        return !schemas.isEmpty();
    }

    @Override
    public Schema getSchema(String name) {
        return schemas.get(name);
    }

    @Override
    public boolean hasSchema(String name) {
        return schemas.containsKey(name);
    }

    @Override
    public String[] getSchemaNames() {
        return schemas.keySet().toArray(new String[0]);
    }

    @Override
    public Collection<Schema> getSchemas() {
        return schemas.values();
    }

    @Override
    public Field getField(QName name) {
        // TODO can this be unified with super behavior?
        return fieldsByName.get(name.getPrefixedName());
    }

    @Override
    public boolean isComplexType() {
        return false;
    }

    @Override
    public boolean isCompositeType() {
        return true;
    }

    @Override
    public boolean validate(Object object) {
        return true;
    }

}
