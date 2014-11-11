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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.nuxeo.theme.Utils;
import org.nuxeo.theme.themes.ThemeIOException;

public class FieldIO {

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
                throw new ThemeIOException("Failed to set field '" + name
                        + "' on " + c.getCanonicalName());
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
                    throw new ThemeIOException(
                            "Failed to parse integer value: '" + value + "'");
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
                        throw new ThemeIOException(
                                "Only one-dimension arrays are supported.");
                    }

                    // Collection<String>
                    if (actualTypes[0].equals(String.class)) {
                        List<String> list = new ArrayList<String>();
                        try {
                            list = Utils.csvToList(value);
                        } catch (IOException e) {
                            throw new ThemeIOException(e);
                        }
                        try {
                            field.set(object, list);
                        } catch (IllegalArgumentException e) {
                            throw new ThemeIOException(e);
                        } catch (IllegalAccessException e) {
                            throw new ThemeIOException(e);
                        }
                    }
                }
            } else {
                throw new ThemeIOException("Cannot update field type '" + name
                        + "' of " + c.getCanonicalName() + " because "
                        + fieldType.getCanonicalName() + " is not supported.");
            }
        }

    }

    @SuppressWarnings("unchecked")
    public static Properties dumpFieldsToProperties(Object object)
            throws ThemeIOException {

        Properties properties = new Properties();

        Class<?> c = object.getClass();
        for (Field field : c.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            Type fieldGenericType = field.getGenericType();
            String fieldName = field.getName();

            FieldInfo fieldInfo = getFieldInfo(c, fieldName);
            if (fieldInfo == null) {
                continue;
            }

            String property = "";
            Object value;
            try {
                value = field.get(object);
            } catch (IllegalAccessException e) {
                throw new ThemeIOException(e);
            } catch (IllegalArgumentException e) {
                throw new ThemeIOException(e);
            }

            // boolean fields
            if (fieldType.equals(boolean.class)
                    || fieldType.equals(Boolean.class)) {
                if (value == null) {
                    property = "false";
                } else {
                    property = Boolean.parseBoolean(value.toString()) ? "true"
                            : "false";
                }
            }
            // string fields
            else if (fieldType.equals(String.class)) {
                property = value == null ? "" : value.toString();
            }
            // integer fields
            else if (fieldType.equals(int.class)
                    || fieldType.equals(Integer.class)) {
                property = value == null ? "" : value.toString();
            }
            // generics
            else if (fieldGenericType instanceof ParameterizedType) {
                if (fieldType.equals(ArrayList.class)
                        || fieldType.equals(List.class)
                        || fieldType.equals(Collection.class)) {

                    Type[] actualTypes = ((ParameterizedType) fieldGenericType).getActualTypeArguments();

                    if (actualTypes.length > 1) {
                        throw new ThemeIOException(
                                "Only one-dimension arrays are supported.");
                    }

                    // Collection<String>
                    if (actualTypes[0].equals(String.class)) {
                        if (value == null) {
                            property = "";
                        } else {
                            property = Utils.listToCsv((List<String>) (value));
                        }
                    } else {
                        throw new ThemeIOException(
                                "Only list of strings are supported.");
                    }

                }
            } else {
                throw new ThemeIOException(
                        "Cannot extract property from field type '" + fieldName
                                + "' of " + c.getCanonicalName() + " because "
                                + fieldType.getCanonicalName()
                                + " is not supported.");
            }

            properties.setProperty(fieldName, property);

        }
        return properties;
    }
}
