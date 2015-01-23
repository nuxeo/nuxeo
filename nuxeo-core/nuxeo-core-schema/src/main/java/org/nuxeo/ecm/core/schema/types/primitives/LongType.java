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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */
package org.nuxeo.ecm.core.schema.types.primitives;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.EnumConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NumericIntervalConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.PatternConstraint;

/**
 * The long type.
 */
public final class LongType extends PrimitiveType {

    private static final long serialVersionUID = 1L;

    public static final String ID = "long";

    public static final LongType INSTANCE = new LongType();

    private LongType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        try {
            Object converted = convert(object);
            return converted == null || converted instanceof Number;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public Object convert(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return value;
        } else if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        } else if (value instanceof String) {
            if (StringUtils.isBlank((String) value)) {
                return null;
            }
            try {
                return Long.valueOf((String) value);
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            }
        }
        return value;
    }

    @Override
    public Object decode(String str) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            return Long.valueOf(0);
        }
    }

    @Override
    public String encode(Object object) {

        if (object instanceof Long) {
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

    @Override
    public boolean support(Class<? extends Constraint> constraint) {
        if (NotNullConstraint.class.equals(constraint)) {
            return true;
        }
        if (EnumConstraint.class.equals(constraint)) {
            return true;
        }
        if (PatternConstraint.class.equals(constraint)) {
            return true;
        }
        if (NumericIntervalConstraint.class.equals(constraint)) {
            return true;
        }
        return false;
    }

}
