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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.schema.types.constraints.Constraint;

/**
 * The implementation for a List type.
 */
public class ListTypeImpl extends AbstractType implements ListType {

    private static final long serialVersionUID = 1L;

    protected static final String DEFAULT_VALUE_SEPARATOR = " ";

    protected final Type type;

    protected final Field field;

    // TODO: should be removed. use field.defaultvalue instead
    protected String defaultValue;

    protected int minOccurs;

    protected int maxOccurs;

    protected boolean isArray = false;

    public ListTypeImpl(String schema, String name, Type type, String fieldName, String defaultValue, int flags,
            Set<Constraint> constraints, int minOccurs, int maxOccurs) {
        super(null, schema, name);
        if (fieldName == null) {
            isArray = true;
            fieldName = "item";
        }
        this.type = type;
        // if the list is an array, there's no field constraint (notnull)
        Collection<Constraint> computedConstraints = isArray ? type.getConstraints() : constraints;
        field = new FieldImpl(QName.valueOf(fieldName), this, type, defaultValue, flags, computedConstraints);
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.defaultValue = defaultValue;
    }

    public ListTypeImpl(String schema, String name, Type type, String fieldName, String defaultValue, int minOccurs,
            int maxOccurs) {
        this(schema, name, type, fieldName, defaultValue, 0, new HashSet<Constraint>(), minOccurs, maxOccurs);
    }

    public ListTypeImpl(String schema, String name, Type type) {
        this(schema, name, type, null, null, 0, -1);
    }

    @Override
    public void setLimits(int minOccurs, int maxOccurs) {
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
    }

    @Override
    public void setDefaultValue(String value) {
        defaultValue = value;
    }

    @Override
    public String getFieldName() {
        return field.getName().getLocalName();
    }

    @Override
    public Type getFieldType() {
        return field.getType();
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public Object getDefaultValue() {
        return type.decode(defaultValue);
    }

    public Type getType() {
        return type;
    }

    @Override
    public int getMinCount() {
        return minOccurs;
    }

    @Override
    public int getMaxCount() {
        return maxOccurs;
    }

    @Override
    public boolean isListType() {
        return true;
    }

    @Override
    public Object decode(String string) {
        // XXX: OG: I do not really know how this is suppose to work
        // I need this to decode default values and I could
        // not find how XMLSchema defines default values for sequences thus the
        // following naive splitting of the string representation of the default
        // value
        if (string != null) {
            List<Object> decoded = new ArrayList<Object>();
            for (String component : string.split(DEFAULT_VALUE_SEPARATOR)) {
                decoded.add(type.decode(component));
            }
            return decoded;
        } else {
            return null;
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean validate(Object object) throws TypeException {
        if (object == null) {
            return true;
        }
        if (object instanceof Collection) {
            return validateCollection((Collection) object);
        } else if (object.getClass().isArray()) {
            return validateArray((Object[]) object);
        }
        return false;
    }

    protected boolean validateArray(Object[] array) {
        return true; // TODO
    }

    @SuppressWarnings("rawtypes")
    protected boolean validateCollection(Collection col) {
        return true; // TODO
    }

    @Override
    public Object newInstance() {
        Object defaultValue = this.defaultValue;
        if (defaultValue != null) {
            return defaultValue;
        } else {
            // XXX AT: maybe use the type to be more specific on list elements
            return new ArrayList<Object>();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convert(Object object) throws TypeException {
        if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            for (int i = 0, len = list.size(); i < len; i++) {
                Object value = list.get(i);
                list.set(i, type.convert(value));
            }
            return object;
        }
        throw new TypeException("Incompatible object: " + object.getClass() + " for type " + getName());
    }

    @Override
    public boolean isArray() {
        return isArray;
    }

    @Override
    public boolean isScalarList() {
        return field.getType().isSimpleType();
    }
}
