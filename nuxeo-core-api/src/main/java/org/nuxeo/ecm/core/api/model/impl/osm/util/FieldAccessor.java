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


    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public Object get(Object instance) throws AccessException {
        try {
            return field.get(instance);
        } catch (Exception e) {
            throw new AccessException("Failed to read field: "+field, e);
        }
    }

    @Override
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

    @Override
    public Class<?> getType() {
        return field.getType();
    }

    @Override
    public Class<?> getDeclaringClass() {
        return field.getDeclaringClass();
    }

    public Field getField() {
        return field;
    }

}
