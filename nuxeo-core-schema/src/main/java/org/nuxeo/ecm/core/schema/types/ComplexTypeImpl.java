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
import java.util.Map;

import org.nuxeo.ecm.core.schema.Namespace;
import org.nuxeo.ecm.core.schema.TypeConstants;
import org.nuxeo.ecm.core.schema.TypeRef;

/**
 * A Complex Type holds several fields.
 */
public class ComplexTypeImpl extends AbstractType implements ComplexType {

    public static final int F_UNSTRUCT_DEFAULT = 0;

    public static final int F_UNSTRUCT_FALSE = 1;

    public static final int F_UNSTRUCT_TRUE = 2;

    private static final long serialVersionUID = 1L;

    /** The fields held by this complex type. */
    protected final Map<QName, Field> fields = new HashMap<QName, Field>();

    /** The map of name or prefixed name to field. */
    protected volatile Map<String, Field> fieldsByName = new HashMap<String, Field>();

    /** The collection of fields, unmodifiable. */
    protected Collection<Field> fieldsCollection = Collections.emptySet();

    protected final Namespace ns;

    protected int unstructured; // 0 - inherit, 1 - structured, 2 - unstructured

    public ComplexTypeImpl(TypeRef<? extends ComplexType> superType,
            String schema, String name, Namespace ns, int struct) {
        super(superType, schema, name);
        ComplexType stype = (ComplexType) getSuperType();
        if (stype != null) {
            for (Field field : stype.getFields()) {
                addField(field);
            }
        }
        unstructured = struct;
        this.ns = ns;
    }

    protected void addField(Field field) {
        QName name = field.getName();
        fields.put(name, field);
        fieldsByName.put(name.getLocalName(), field);
        fieldsByName.put(name.getPrefixedName(), field);
        fieldsCollection = Collections.unmodifiableCollection(fields.values());
    }

    public ComplexTypeImpl(ComplexType superType, String schema, String name) {
        this(superType == null ? null : superType.getRef(), schema, name,
                Namespace.DEFAULT_NS, F_UNSTRUCT_DEFAULT);
    }

    public ComplexTypeImpl(ComplexType superType, String schema, String name,
            Namespace ns) {
        this(superType == null ? null : superType.getRef(), schema, name, ns,
                F_UNSTRUCT_DEFAULT);
    }

    public ComplexTypeImpl(TypeRef<? extends ComplexType> superType,
            String schema, String name) {
        this(superType, schema, name, Namespace.DEFAULT_NS, F_UNSTRUCT_DEFAULT);
    }

    public ComplexTypeImpl(TypeRef<? extends ComplexType> superType,
            String schema, String name, Namespace ns) {
        this(superType, schema, name, Namespace.DEFAULT_NS, F_UNSTRUCT_DEFAULT);
    }

    @Override
    public Namespace getNamespace() {
        return ns;
    }

    @Override
    public boolean isUnstructured() {
        if (unstructured == F_UNSTRUCT_DEFAULT) {
            unstructured = hasFields() ? F_UNSTRUCT_FALSE : F_UNSTRUCT_TRUE;
        }
        return unstructured == F_UNSTRUCT_TRUE;
    }

    @Override
    public Field getField(String name) {
        return fieldsByName.get(name);
    }

    @Override
    public Field getField(QName name) {
        return fields.get(name);
    }

    @Override
    public Collection<Field> getFields() {
        return fieldsCollection;
    }

    @Override
    public int getFieldsCount() {
        return fields.size();
    }

    @Override
    public Field addField(String name, TypeRef<? extends Type> type) {
        return addField(QName.valueOf(name, ns.prefix), type, null, 0);
    }

    @Override
    public Field addField(QName name, TypeRef<? extends Type> type) {
        return addField(name, type, null, 0);
    }

    @Override
    public Field addField(String name, TypeRef<? extends Type> type,
            String defaultValue, int flags) {
        return addField(QName.valueOf(name, ns.prefix), type, defaultValue,
                flags);
    }

    @Override
    public Field addField(QName name, TypeRef<? extends Type> type,
            String defaultValue, int flags) {
        FieldImpl field = new FieldImpl(name, getRef(), type, defaultValue,
                flags);
        addField(field);
        return field;
    }

    @Override
    public boolean hasField(String name) {
        return fieldsByName.containsKey(name);
    }

    @Override
    public boolean hasField(QName name) {
        return fields.containsKey(name);
    }

    @Override
    public boolean hasFields() {
        return !fields.isEmpty();
    }

    @Override
    public boolean isComplexType() {
        return true;
    }

    @Override
    public boolean validate(Object object) throws TypeException {
        if (object == null && isNotNull()) {
            return false;
        }
        if (object == null) {
            return true;
        }
        if (object instanceof Map) {
            return validateMap((Map) object);
        }
        return false;
    }

    protected boolean validateMap(Map map) {
        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

    @Override
    public Map<String, Object> newInstance() {
        if (TypeConstants.isContentType(this)) {
            // NXP-912: should return null for a blob. Since there is no
            // pluggable adapter mechanism on types, and since document model
            // properties consider that every complex property named "content"
            // should be dealt with a BlobProperty, this is hardcoded here.
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        for (Field field : fields.values()) {
            Object value;
            Type type = field.getType();
            if (type.isComplexType()) {
                value = type.newInstance();
            } else if (type.isListType()) {
                value = new ArrayList<Object>();
            } else {
                value = field.getDefaultValue();
            }
            map.put(field.getName().getLocalName(), value);
        }
        return map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convert(Object object) throws TypeException {
        if (object instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) object;
            for (Map.Entry entry : map.entrySet()) {
                String key = entry.getKey().toString();
                Field field = getField(key);
                if (field == null) {
                    throw new IllegalArgumentException("Field " + key
                            + " is not defined for the complex type "
                            + getName());
                }
                entry.setValue(field.getType().convert(entry.getValue()));
            }
            return object;
        }
        throw new TypeException("Incompatible object: " + object.getClass()
                + " for type " + this);
    }

    @Override
    public TypeRef<? extends ComplexType> getRef() {
        return new TypeRef<ComplexType>(schema, name, this);
    }
}
