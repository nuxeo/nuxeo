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

import java.util.Calendar;
import java.util.Date;

import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.types.constraints.Constraint;
import org.nuxeo.ecm.core.schema.types.constraints.DateIntervalConstraint;
import org.nuxeo.ecm.core.schema.types.constraints.NotNullConstraint;
import org.nuxeo.ecm.core.schema.utils.DateParser;

/**
 * The date (actually timestamp) type.
 */
public class DateType extends PrimitiveType {

    private static final long serialVersionUID = 1L;

    public static final String ID = "date";

    public static final DateType INSTANCE = new DateType();

    public DateType() {
        super(ID);
    }

    @Override
    public boolean validate(Object object) {
        Object converted = convert(object);
        return converted == null || converted instanceof Date || converted instanceof Calendar;
    }

    @Override
    public Object convert(Object value) {
        return value;
    }

    @Override
    public Object decode(String str) {
        Date date = DateParser.parseW3CDateTime(str);
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return cal;
        }
        return null;
    }

    @Override
    public String encode(Object object) {
        if (object instanceof Date) {
            return DateParser.formatW3CDateTime((Date) object);
        } else if (object instanceof Calendar) {
            return DateParser.formatW3CDateTime(((Calendar) object).getTime());
        } else {
            return null;
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
        if (DateIntervalConstraint.class.equals(constraint)) {
            return true;
        }
        return false;
    }

}
