/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types.primitives;

import java.util.Calendar;
import java.util.Date;

import org.nuxeo.ecm.core.schema.types.PrimitiveType;
import org.nuxeo.ecm.core.schema.utils.DateParser;

// TODO this should be renamed to TimestampType
/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DateType extends PrimitiveType {

    public static final String ID = "date";

    public static final DateType INSTANCE = new DateType();

    private static final long serialVersionUID = 4406485984077086898L;

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
                return Integer.parseInt((String) value);
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

}
