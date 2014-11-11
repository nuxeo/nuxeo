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

package org.nuxeo.ecm.core.api.model.impl.osm.util;

import java.lang.reflect.Field;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FieldAccessor implements MemberAccessor {

    private static final long serialVersionUID = 2870143167271942320L;

    private final Field field;
    private boolean readOnly = false;


    public FieldAccessor(Field field, boolean isReadOnly) {
        this.field = field;
        readOnly = isReadOnly;
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
    }

    public FieldAccessor(Field field) {
        this(field, false);
    }


    public boolean isReadOnly() {
        return readOnly;
    }

    public Object get(Object instance) throws AccessException {
        try {
            return field.get(instance);
        } catch (Exception e) {
            throw new AccessException("Failed to read field: "+field, e);
        }
    }

    public void set(Object instance, Object value) throws AccessException {
        if (readOnly) {
            throw new ReadOnlyAccessException("Attempted to write on a read only field: "+field);
        }
        try {
            field.set(instance, value);
        } catch (Exception e) {
            throw new AccessException("Failed to write field: " + field, e);
        }
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Class<?> getDeclaringClass() {
        return field.getDeclaringClass();
    }

    public Field getField() {
        return field;
    }

}
