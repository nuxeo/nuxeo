/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ArrayProperty extends ScalarProperty {

    private static final long serialVersionUID = 0L;

    public ArrayProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public ListType getType() {
        return (ListType) super.getType();
    }

    @Override
    public boolean isContainer() {
        return false;
    }

    @Override
    public void setValue(Object value) throws PropertyException {
        // this code manage dirty status for the arrayproperty and its childs values
        // it checks whether the property changed, or their index changed
        if (value == null) {
            childDirty = new boolean[0];
            super.setValue(null);
        } else {
            Object[] oldValues = (Object[]) internalGetValue();
            boolean[] oldChildDirty = getChildDirty();
            super.setValue(value);
            Object[] newValues = (Object[]) internalGetValue();
            boolean[] newChildDirty = new boolean[newValues != null ? newValues.length : 0];
            for (int i = 0; i < newChildDirty.length; i++) {
                Object newValue = newValues[i]; // NOSONAR
                if (oldValues == null || i >= oldValues.length) {
                    newChildDirty[i] = true;
                } else {
                    Object oldValue = oldValues[i];
                    if (!((newValue == null && oldValue == null) || (newValue != null && newValue.equals(oldValue)))) {
                        newChildDirty[i] = true;
                    } else {
                        newChildDirty[i] = false || oldChildDirty[i];
                    }
                }
            }
            childDirty = newChildDirty;
        }
    }

    @Override
    protected boolean isSameValue(Serializable value1, Serializable value2) {
        Object[] castedtValue1 = (Object[]) value1;
        Object[] castedtValue2 = (Object[]) value2;
        return castedtValue1 == castedtValue2 || (castedtValue1 == null && castedtValue2.length == 0)
                || (castedtValue2 == null && castedtValue1.length == 0) || Arrays.equals(castedtValue1, castedtValue2);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value.getClass().isArray();
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (value == null) {
            return null;
        } else if (value.getClass().isArray()) {
            return convert(Arrays.asList((Object[]) value));
        } else if (value instanceof Collection) {
            return convert((Collection<?>) value);
        }
        throw new PropertyConversionException(value.getClass(), Object[].class, getXPath());
    }

    protected Serializable convert(Collection<?> value) throws PropertyConversionException {
        Property typedProperty = getRoot().createProperty(null, getType().getField(), NONE);
        Collection<Object> col = new ArrayList<>(value.size());
        for (Object v : value) {
            if (v == null) {
                col.add(null);
            } else {
                col.add(typedProperty.normalize(v));
            }
        }
        return col.toArray((Object[]) Array.newInstance(typedProperty.newInstance().getClass(), col.size()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType) throws PropertyConversionException {
        if (toType.isArray()) {
            return (T) PrimitiveArrays.toObjectArray(value);
        } else if (Collection.class.isAssignableFrom(toType)) {
            return (T) Arrays.asList((Object[]) value);
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() {
        return new Serializable[0];
    }

    // this boolean array managed the dirty flags for arrayproperty childs
    private boolean[] childDirty = null;

    protected boolean[] getChildDirty() {
        if (childDirty == null) {
            Object[] oldValues = (Object[]) internalGetValue();
            if (oldValues == null) {
                childDirty = new boolean[0];
            } else {
                childDirty = new boolean[oldValues.length];
                for (int i = 0; i < childDirty.length; i++) {
                    childDirty[i] = false;
                }
            }
        }
        return childDirty;
    }

    /**
     * This method provides a way to know if some arrayproperty values are dirty: value or index changed. since 7.2
     */
    public boolean isDirty(int index) {
        if (index > getChildDirty().length) {
            throw new IndexOutOfBoundsException(
                    "Index out of bounds: " + index + ". Bounds are: 0 - " + (getChildDirty().length - 1));
        }
        return getChildDirty()[index];
    }

    @Override
    public void clearDirtyFlags() {
        // even makes child properties not dirty
        super.clearDirtyFlags();
        for (int i = 0; i < getChildDirty().length; i++) {
            childDirty[i] = false;
        }
    }

}
