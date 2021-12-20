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
import java.util.Calendar;
import java.util.Date;

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DateProperty extends ScalarProperty {

    private static final long serialVersionUID = -7344978155078073495L;

    public DateProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value instanceof Calendar;
    }

    @Override
    public Serializable normalize(Object value) throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable) value;
        }
        if (value.getClass() == Date.class) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) value);
            return cal;
        }
        if (value instanceof String) {
            String string = (String) value;
            if (string.length() == 0) {
                return null;
            }
            try {
                return (Calendar) field.getType().decode(value.toString());
            } catch (IllegalArgumentException e) {
                throw new PropertyConversionException(value.getClass(), Calendar.class, e.getMessage());
            }
        }
        throw new PropertyConversionException(value.getClass(), Calendar.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType) throws PropertyConversionException {
        if (value == null || toType == Calendar.class) {
            return (T) value;
        }
        if (toType == Date.class) {
            return (T) ((Calendar) value).getTime();
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() {
        return Calendar.getInstance();
    }

}
