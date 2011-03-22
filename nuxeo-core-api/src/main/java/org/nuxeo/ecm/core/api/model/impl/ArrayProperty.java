/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.model.impl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;

import org.nuxeo.common.collections.PrimitiveArrays;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.JavaTypes;
import org.nuxeo.ecm.core.schema.types.ListType;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
    public boolean isNormalized(Object value) {
        return value == null || value.getClass().isArray();
    }

    @Override
    public Serializable normalize(Object value)
            throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable) value;
        }
        if (value instanceof Collection) {
            Collection<?> col = (Collection<?>) value;
            Class<?> klass = JavaTypes.getClass(getType().getFieldType());
            return col.toArray((Object[]) Array.newInstance(klass, col.size()));
        }
        throw new PropertyConversionException(value.getClass(), Object[].class, getPath());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException {
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

}
