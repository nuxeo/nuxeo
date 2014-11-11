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

import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyConversionException;
import org.nuxeo.ecm.core.api.model.impl.ScalarProperty;
import org.nuxeo.ecm.core.schema.types.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class StringProperty extends ScalarProperty {

    private static final long serialVersionUID = -3586917845281930368L;

    public StringProperty(Property parent, Field field, int flags) {
        super(parent, field, flags);
    }

    @Override
    public boolean isNormalized(Object value) {
        return value == null || value.getClass() == String.class;
    }

    @Override
    public Serializable normalize(Object value)
            throws PropertyConversionException {
        if (isNormalized(value)) {
            return (Serializable)value;
        }
        throw new PropertyConversionException(value.getClass(), String.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException {
        if (toType == String.class) {
            return (T) value;
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() {
        return "";
    }

}
