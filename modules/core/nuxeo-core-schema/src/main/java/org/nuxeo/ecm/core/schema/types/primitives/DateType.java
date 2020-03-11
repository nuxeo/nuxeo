/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
        return object instanceof Date || object instanceof Calendar;
    }

    @Override
    public Object convert(Object value) {
        if (value instanceof Date) {
            return value;
        } else if (value instanceof Calendar) {
            return value;
        } else {
            // TODO
            try {
                return Integer.valueOf((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
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
