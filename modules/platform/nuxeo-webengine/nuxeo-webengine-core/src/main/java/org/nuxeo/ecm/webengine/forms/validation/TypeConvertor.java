/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.forms.validation;

import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class TypeConvertor<T> {

    public abstract Class<?> getType();

    public abstract T convert(String value) throws ValidationException;

    public Object[] newArray(int length) {
        return (Object[]) Array.newInstance(getType(), length);
    }

    @SuppressWarnings("unchecked")
    public static <T> TypeConvertor<T> getConvertor(Class<T> type) {
        if (type == String.class) {
            return null;
        }
        TypeConvertor<?> result = null;
        if (type == Boolean.class) {
            result = BOOLEAN;
        } else if (type == Date.class) {
            result = DATE;
        } else if (type == Integer.class) {
            result = INTEGER;
        } else if (type == Float.class) {
            result = FLOAT;
        } else if (type == Long.class) {
            result = LONG;
        } else if (type == Double.class) {
            result = DOUBLE;
        } else if (type == Class.class) {
            result = CLASS;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
        return (TypeConvertor<T>) result;
    }

    public static final TypeConvertor<Boolean> BOOLEAN = new TypeConvertor<Boolean>() {
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        @Override
        public Boolean convert(String value) throws ValidationException {
            if ("true".equals(value)) {
                return Boolean.TRUE;
            } else if ("false".equals(value)) {
                return Boolean.FALSE;
            } else {
                throw new ValidationException();
            }
        }
    };

    public static final TypeConvertor<Integer> INTEGER = new TypeConvertor<Integer>() {
        @Override
        public Class<?> getType() {
            return Integer.class;
        }

        @Override
        public Integer convert(String value) throws ValidationException {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                throw new ValidationException();
            }
        }
    };

    public static final TypeConvertor<Long> LONG = new TypeConvertor<Long>() {
        @Override
        public Class<?> getType() {
            return Long.class;
        }

        @Override
        public Long convert(String value) throws ValidationException {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new ValidationException();
            }
        }
    };

    public static final TypeConvertor<Float> FLOAT = new TypeConvertor<Float>() {
        @Override
        public Class<?> getType() {
            return Float.class;
        }

        @Override
        public Float convert(String value) throws ValidationException {
            try {
                return Float.valueOf(value);
            } catch (NumberFormatException e) {
                throw new ValidationException();
            }
        }
    };

    public static final TypeConvertor<Double> DOUBLE = new TypeConvertor<Double>() {
        @Override
        public Class<?> getType() {
            return Double.class;
        }

        @Override
        public Double convert(String value) throws ValidationException {
            try {
                return Double.valueOf(value);
            } catch (NumberFormatException e) {
                throw new ValidationException();
            }
        }
    };

    public static final TypeConvertor<Date> DATE = new TypeConvertor<Date>() {
        @Override
        public Class<?> getType() {
            return Date.class;
        }

        @Override
        public Date convert(String value) throws ValidationException {
            try {
                return parseDate(value);
            } catch (IllegalArgumentException e) {
                throw new ValidationException();
            }
        }
    };

    public static final TypeConvertor<Class<?>> CLASS = new TypeConvertor<Class<?>>() {
        @Override
        public Class<?> getType() {
            return Class.class;
        }

        @Override
        public Class<?> convert(String value) throws ValidationException {
            try {
                return loadClass(value);
            } catch (ReflectiveOperationException e) {
                throw new ValidationException();
            }
        }
    };

    public static Class<?> loadClass(String name) throws ReflectiveOperationException {
        return Framework.getService(WebEngine.class).loadClass(name);
    }

    private static final Pattern PATTERN = Pattern.compile("(\\d{4})(?:-(\\d{2}))?(?:-(\\d{2}))?(?:[Tt](?:(\\d{2}))?(?::(\\d{2}))?(?::(\\d{2}))?(?:\\.(\\d{3}))?)?([Zz])?(?:([+-])(\\d{2}):(\\d{2}))?");

    /**
     * Parse the serialized string form into a java.util.Date
     *
     * @param date The serialized string form of the date
     * @return The created java.util.Date
     */
    public static Date parseDate(String date) {
        Matcher m = PATTERN.matcher(date);
        if (m.find()) {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            int hoff = 0, moff = 0, doff = -1;
            if (m.group(9) != null) {
                doff = m.group(9).equals("-") ? 1 : -1;
                hoff = doff * (m.group(10) != null ? Integer.parseInt(m.group(10)) : 0);
                moff = doff * (m.group(11) != null ? Integer.parseInt(m.group(11)) : 0);
            }
            c.set(Calendar.YEAR, Integer.parseInt(m.group(1)));
            c.set(Calendar.MONTH, m.group(2) != null ? Integer.parseInt(m.group(2)) - 1 : 0);
            c.set(Calendar.DATE, m.group(3) != null ? Integer.parseInt(m.group(3)) : 1);
            c.set(Calendar.HOUR_OF_DAY, m.group(4) != null ? Integer.parseInt(m.group(4)) + hoff : 0);
            c.set(Calendar.MINUTE, m.group(5) != null ? Integer.parseInt(m.group(5)) + moff : 0);
            c.set(Calendar.SECOND, m.group(6) != null ? Integer.parseInt(m.group(6)) : 0);
            c.set(Calendar.MILLISECOND, m.group(7) != null ? Integer.parseInt(m.group(7)) : 0);
            return c.getTime();
        } else {
            throw new IllegalArgumentException("Invalid Date Format");
        }
    }

}
