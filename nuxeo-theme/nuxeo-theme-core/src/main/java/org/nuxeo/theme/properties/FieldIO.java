/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.properties;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.theme.themes.ThemeIOException;

public class FieldIO {

    private static final Log log = LogFactory.getLog(FieldIO.class);

    public static FieldInfo getFieldInfo(Class<?> c, String name) {
        try {
            return c.getField(name).getAnnotation(FieldInfo.class);
        } catch (SecurityException e) {
            return null;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    public static void updateFieldsFromProperties(Object object,
            Properties properties) throws ThemeIOException {
        Enumeration<?> names = properties.propertyNames();

        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = properties.getProperty(name);

            Class<?> c = object.getClass();

            FieldInfo fieldInfo = getFieldInfo(c, name);
            if (fieldInfo == null) {
                continue;
            }

            Field field;
            try {
                field = c.getField(name);
            } catch (SecurityException e) {
                throw new ThemeIOException(e);
            } catch (NoSuchFieldException e) {
                log.warn("Failed to set field '" + name + "' on "
                        + c.getCanonicalName());
                continue;
            }

            Class<?> fieldType = field.getType();
            Type fieldGenericType = field.getGenericType();

            // boolean fields
            if (fieldType.equals(boolean.class)
                    || fieldType.equals(Boolean.class)) {
                try {
                    field.set(object, Boolean.parseBoolean(value));
                } catch (IllegalArgumentException e) {
                    throw new ThemeIOException(e);
                } catch (IllegalAccessException e) {
                    throw new ThemeIOException(e);
                }
            }

            // string fields
            else if (fieldType.equals(String.class)) {
                try {
                    field.set(object, value);
                } catch (IllegalArgumentException e) {
                    throw new ThemeIOException(e);
                } catch (IllegalAccessException e) {
                    throw new ThemeIOException(e);
                }
            }

            // integer fields
            else if (fieldType.equals(int.class)
                    || fieldType.equals(Integer.class)) {
                try {
                    if ("".equals(value)) {
                        field.set(object, null);
                    } else {
                        field.set(object, Integer.valueOf(value));
                    }
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse integer value: '" + value + "'");
                } catch (IllegalArgumentException e) {
                    throw new ThemeIOException(e);
                } catch (IllegalAccessException e) {
                    throw new ThemeIOException(e);
                }
            }

            // generics
            else if (fieldGenericType instanceof ParameterizedType) {
                if (fieldType.equals(ArrayList.class)
                        || fieldType.equals(List.class)
                        || fieldType.equals(Collection.class)) {

                    Type[] actualTypes = ((ParameterizedType) fieldGenericType).getActualTypeArguments();

                    if (actualTypes.length > 1) {
                        log.warn("Only one-dimension arrays are supported.");
                        continue;
                    }

                    // Collection<String>
                    if (actualTypes[0].equals(String.class)) {
                        List<String> list = new ArrayList<String>();
                        list.addAll(Arrays.asList(value.split(",")));
                        try {
                            field.set(object, list);
                        } catch (IllegalArgumentException e) {
                            throw new ThemeIOException(e);
                        } catch (IllegalAccessException e) {
                            throw new ThemeIOException(e);
                        }
                        continue;
                    }
                }
            } else {
                log.warn("Field type '" + name + "' of " + c.getCanonicalName()
                        + " is not supported: " + fieldType.getCanonicalName());
            }
        }

    }

    public static Properties dumpFieldsToProperties(Object object)
            throws ThemeIOException {

        Properties properties = new Properties();

        Class<?> c = object.getClass();
        for (Field field : c.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            String fieldName = field.getName();

            FieldInfo fieldInfo = getFieldInfo(c, fieldName);
            if (fieldInfo == null) {
                continue;
            }

            String value;
            String property = "";
            try {
                Object v = field.get(object);
                value = v == null ? "" : v.toString();
            } catch (IllegalAccessException e) {
                throw new ThemeIOException(e);
            } catch (IllegalArgumentException e) {
                throw new ThemeIOException(e);
            }
            // boolean fields
            if (fieldType.equals(boolean.class)
                    || fieldType.equals(Boolean.class)) {
                property = Boolean.parseBoolean(value.toString()) ? "true"
                        : "false";
            }
            // string fields
            else if (fieldType.equals(String.class)) {
                property = value;
            }
            // integer fields
            else if (fieldType.equals(int.class)
                    || fieldType.equals(Integer.class)) {
                property = value;
            } else {
                log.warn("Field type '" + fieldName + "' of "
                        + c.getCanonicalName() + " is not supported: "
                        + fieldType.getCanonicalName());
                continue;
            }

            properties.setProperty(fieldName, property);

        }
        return properties;
    }

}
