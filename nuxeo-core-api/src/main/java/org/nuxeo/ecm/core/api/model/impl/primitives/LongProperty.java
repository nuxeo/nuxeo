/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 *
 */
public class LongProperty extends ScalarProperty {

    private static final long serialVersionUID = 4071336664294418041L;

    public LongProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value.getClass() == Long.class;
    }

    @Override
    public Serializable normalize(Object value)
            throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable)value;
        }
        if (value.getClass() == String.class) {
            String string = (String)value;
            if (string.length() == 0) {
                return null;
            }
            return Long.valueOf(value.toString());
        }
        if (value instanceof Number) {
            return ((Number)value).longValue();
        }
        throw new PropertyConversionException(value.getClass(), Long.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException {
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
