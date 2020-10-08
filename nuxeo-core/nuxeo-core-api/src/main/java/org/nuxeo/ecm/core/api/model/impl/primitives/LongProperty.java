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

import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class LongProperty extends ScalarProperty {

    private static final long serialVersionUID = 4071336664294418041L;

    public LongProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value instanceof Long || value instanceof DeltaLong;
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
            try {
                return Long.valueOf(string);
            } catch (NumberFormatException e) {
                throw new PropertyConversionException(value.getClass(), Long.class, e.getMessage());
            }
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        throw new PropertyConversionException(value.getClass(), Long.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType) throws PropertyConversionException {
        if (value == null || Long.class == toType) {
            return (T) value;
        }
        Long v = (Long) value;
        if (toType == Integer.class) {
            return (T) new Integer(v.intValue());
        }
        if (toType == String.class) {
            return (T) v.toString();
        }
        if (toType == Double.class) {
            return (T) new Double(v.doubleValue());
        }
        if (toType == Float.class) {
            return (T) new Float(v.floatValue());
        }
        if (toType == Short.class) {
            return (T) new Short(v.shortValue());
        }
        if (toType == Byte.class) {
            return (T) new Byte(v.byteValue());
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() {
        return (long) 0;
    }

}
