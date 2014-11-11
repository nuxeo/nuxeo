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
 *
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
    public Serializable normalize(Object value)
            throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable)value;
        }
        if (value.getClass() == Date.class) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date)value);
            return cal;
        }
        if (value instanceof String) {
            String string = (String) value;
            if (string.length() == 0) {
                return null;
            }
            return (Calendar) field.getType().decode(value.toString());
        }
        throw new PropertyConversionException(value.getClass(), Calendar.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException {
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
