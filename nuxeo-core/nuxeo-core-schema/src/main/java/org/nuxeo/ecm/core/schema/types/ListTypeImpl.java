/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
        this(schema, name, type, fieldName, defaultValue, 0, new HashSet<>(), minOccurs, maxOccurs);
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
        if (StringUtils.isBlank(string)) {
            return null;
        }
        String[] split = string.split(DEFAULT_VALUE_SEPARATOR);
        List<Object> decoded = new ArrayList<>(split.length);
        Class<?> klass = null;
        for (String s : split) {
            Object o = type.decode(s);
            if (klass == null && o != null) {
                klass = o.getClass();
            }
            decoded.add(o);
        }
        if (klass == null) {
            klass = Object.class;
        }
        // turn the list into a properly-typed array for the elements
        Object[] array = (Object[]) Array.newInstance(klass, decoded.size());
        return decoded.toArray(array);
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
            return new ArrayList<>();
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
