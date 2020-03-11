/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.api.model.impl.primitives;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class BooleanProperty extends ScalarProperty {

    private static final long serialVersionUID = -6408890276716577303L;

    public BooleanProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value.getClass() == Boolean.class;
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable) value;
        }
        if (value.getClass() == String.class) {
            String string = (String) value;
            if (string.length() == 0) {
                return null;
            }
            return Boolean.valueOf(value.toString());
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
        }
        throw new PropertyConversionException(value.getClass(), Boolean.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType) throws PropertyConversionException {
        if (value == null || Boolean.class == toType) {
            return (T) value;
        }
        Boolean v = (Boolean) value;
        if (toType == String.class) {
            return (T) v.toString();
        }
        byte n = (byte) (v ? 1 : 0);
        if (toType == Integer.class) {
            return (T) Integer.valueOf(n);
        }
        if (toType == Long.class) {
            return (T) Long.valueOf(n);
        }
        if (toType == Double.class) {
            return (T) Double.valueOf(n);
        }
        if (toType == Float.class) {
            return (T) Float.valueOf(n);
        }
        if (toType == Short.class) {
            return (T) Short.valueOf(n);
        }
        if (toType == Byte.class) {
            return (T) Byte.valueOf(n);
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() {
        return Boolean.FALSE;
    }

}
