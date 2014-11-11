/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.schema.types.primitives;

import org.nuxeo.ecm.core.schema.types.PrimitiveType;

/**
 * The integer type.
 */
public final class IntegerType extends PrimitiveType {

    private static final long serialVersionUID = 1L;

    public static final String ID = "integer";

    public static final IntegerType INSTANCE = new IntegerType();

    private IntegerType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        return object instanceof Number;
    }

    @Override
    public Object convert(Object value) {
        if (value instanceof Integer) {
            return value;
        } else if (value instanceof Number) {
            return Integer.valueOf(((Number) value).intValue());
        } else {
            try {
                return Integer.valueOf((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    @Override
    public Object decode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return Integer.valueOf(0);
        }
    }

    @Override
    public String encode(Object object) {
        if (object instanceof Integer) {
            return object.toString();
        } else if (object instanceof Number) {
            return object.toString();
        } else {
            return object != null ? (String) object : "";
        }
    }

    protected Object readResolve() {
        return INSTANCE;
    }

}
