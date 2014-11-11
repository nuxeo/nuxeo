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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.schema.SchemaNames;
import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * A Composite Type resolves fields for several schemas.
 */
public class CompositeTypeImpl extends ComplexTypeImpl implements CompositeType {

    private static final long serialVersionUID = 1L;

    /** The schemas (refs) for this composite type. */
    protected final Map<String, TypeRef<Schema>> schemas = new HashMap<String, TypeRef<Schema>>();

    /** The precomputed schema names. */
    protected String[] schemaNames = new String[0];

    /** Does some stuff need to be recomputed lazily. */
    protected volatile boolean dirty;

    /** The lazily precomputed map of prefix to schema. */
    protected volatile Map<String, Schema> prefix2schemas = Collections.emptyMap();

    // also fieldsByName

    public CompositeTypeImpl(CompositeType superType, String schema, String name,
            String[] schemas) {
        this (superType == null ? null : superType.getRef(), schema, name, schemas);
    }

    public CompositeTypeImpl(TypeRef<? extends CompositeType> superType,
            String schema, String name, String[] schemas) {
        super(superType, schema, name);
        CompositeType stype = (CompositeType) this.superType.get();
        if (stype != null) {
            for (String sname : stype.getSchemaNames()) {
                addSchema(sname);
            }
        }
        if (schemas != null) {
            for (String sname : schemas) {
                addSchema(sname);
            }
        }
    }

    @Override
    public final boolean hasSchemas() {
        return !schemas.isEmpty();
    }

    @Override
    public final void addSchema(String schema) {
        schemas.put(schema, new TypeRef<Schema>(SchemaNames.SCHEMAS, schema));
        updated();
    }

    @Override
    public final void addSchema(Schema schema) {
        schemas.put(schema.getName(), schema.getRef());
        updated();
    }

    /** Update non-lazy stuff. The rest will be done by checkDirty. */
    protected void updated() {
        schemaNames = schemas.keySet().toArray(new String[schemas.size()]);
        dirty = true;
    }

    protected void checkDirty() {
        // double-checked locking works because fields are volatile
        if (!dirty) {
            return;
        }
        synchronized(this) {
            if (!dirty) {
                return;
            }
            recompute();
            dirty = false;
        }
    }

    /** Do not call this directly, go through checkDirty. */
    protected void recompute() {
        fields.clear();
        prefix2schemas = new HashMap<String, Schema>();
        fieldsByName = new HashMap<String, Field>();
        for (TypeRef<Schema> ref : schemas.values()) {
            Schema schema = ref.get();
            if (schema == null) {
                continue;
            }
            prefix2schemas.put(schema.getNamespace().prefix, schema);
            for (Field field : schema.getFields()) {
                QName name = field.getName();
                fields.put(name, field);
                fieldsByName.put(name.getLocalName(), field);
                fieldsByName.put(name.getPrefixedName(), field);
            }
        }
        fieldsCollection = Collections.unmodifiableCollection(fields.values());
    }

    @Override
    public final Schema getSchema(String name) {
        TypeRef<Schema> ref = schemas.get(name);
        return ref == null ? null : ref.get();
    }

    @Override
    public final Schema getSchemaByPrefix(String prefix) {
        checkDirty();
        return prefix2schemas.get(prefix);
    }

    @Override
    public final boolean hasSchema(String name) {
        return schemas.containsKey(name);
    }

    @Override
    public final String[] getSchemaNames() {
        return schemaNames.clone();
    }

    @Override
    public final Collection<Schema> getSchemas() {
        List<Schema> list = new ArrayList<Schema>(schemas.size());
        for (TypeRef<Schema> ref : schemas.values()) {
            list.add(ref.get());
        }
        return Collections.unmodifiableCollection(list);
    }

    @Override
    public final Field addField(QName name, TypeRef<? extends Type> type) {
        throw new UnsupportedOperationException(
                "Cannot add fields to a composite type since it is a composition of other complex types");
    }

    @Override
    public final Field getField(String name) {
        checkDirty();
        return fieldsByName.get(name);
    }

    @Override
    public final Field getField(QName name) {
        checkDirty();
        return fieldsByName.get(name.getPrefixedName());
    }

    @Override
    public final boolean hasField(String name) {
        checkDirty();
        return fieldsByName.containsKey(name);
    }

    @Override
    public final boolean hasField(QName name) {
        checkDirty();
        return fields.containsKey(name);
    }

    @Override
    public final Collection<Field> getFields() {
        checkDirty();
        return fieldsCollection;
    }

    @Override
    public final boolean isComplexType() {
        return false;
    }

    @Override
    public final boolean isCompositeType() {
        return true;
    }

    @Override
    public final boolean validate(Object object) {
        return true;
    }

    @Override
    public TypeRef<? extends CompositeType> getRef() {
        return new TypeRef<CompositeType>(schema, name, this);
    }

}
