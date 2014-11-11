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
public class StringProperty extends ScalarProperty {

    private static final long serialVersionUID = -3586917845281930368L;


    public StringProperty(Property parent, Field field, int flags) {
        super (parent, field, flags);
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

    @Override
    public <T> T convertTo(Serializable value, Class<T> toType)
            throws PropertyConversionException {
        if (toType == String.class) {
            return (T)value;
        }
        throw new PropertyConversionException(value.getClass(), toType);
    }

    @Override
    public Object newInstance() throws InstantiationException,
            IllegalAccessException {
        return "";
    }

}
