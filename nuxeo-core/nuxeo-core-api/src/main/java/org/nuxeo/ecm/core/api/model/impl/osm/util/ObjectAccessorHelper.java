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
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ObjectAccessorHelper {

    // Utility class.
    private ObjectAccessorHelper() {
    }

    public static Method getMethod(Class<?> containerType, String name)
            throws NoSuchMethodException {
        try {
            return containerType.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = containerType.getSuperclass();
            if (superClass == null) {
                throw e;
            }
            return getMethod(superClass, name);
        }
    }

    public static Field getField(Class<?> containerType, String name)
            throws NoSuchFieldException {
        try {
            return containerType.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = containerType.getSuperclass();
            if (superClass == null) {
                throw e;
            }
            return getField(superClass, name);
        }
    }

    public static String getGetterName(String name) {
        if (name == null) {
            return "get";
        }
        int len = name.length();
        if (len == 0) {
            return "get";
        }
        StringBuilder sb = new StringBuilder(len + 3);
        sb.append("get").append(name);
        char ch = name.charAt(0);
        ch = Character.toUpperCase(ch);
        sb.setCharAt(3, ch);
        return sb.toString();
    }

    public static String getSetterName(String name) {
        if (name == null) {
            return "set";
        }
        int len = name.length();
        if (len == 0) {
            return "set";
        }
        StringBuilder sb = new StringBuilder(len + 3);
        sb.append("set").append(name);
        char ch = name.charAt(0);
        ch = Character.toUpperCase(ch);
        sb.setCharAt(3, ch);
        return sb.toString();
    }

    // FIXME: this seems buggy.
    public static String getPropertyName(String methodName) {
        if ((methodName.startsWith("get") || methodName.startsWith("get"))
                && methodName.length() > 3) {
            methodName = methodName.substring(3);
            StringBuilder sb = new StringBuilder(methodName);
            sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        }
        return methodName;
    }

    public static MemberAccessor getFieldAccessor(Class<?> containerType,
            String name) throws NoSuchFieldException {
        return new FieldAccessor(getField(containerType, name));
    }

    public static MemberAccessor getFieldAccessor(Class<?> containerType,
            String name, boolean isReadOnly) throws NoSuchFieldException {
        return new FieldAccessor(getField(containerType, name), isReadOnly);
    }

    public static MemberAccessor getPropertyAccessor(Class<?> containerType,
            String name) throws NoSuchMethodException {
        return new MethodAccessor(getMethod(containerType, getGetterName(name)),
                getMethod(containerType, getSetterName(name)));
    }

    public static MemberAccessor getPropertyAccessor(
            Class<?> containerType, String name, boolean IsReadOnly)
            throws NoSuchMethodException {
        String getter = getGetterName(name);
        String setter = IsReadOnly ? null : getSetterName(name);
        return getMethodAccessor(containerType, getter, setter);
    }

    public static MemberAccessor getMethodAccessor(Class<?> containerType,
            String getterName, String setterName) throws NoSuchMethodException {
        return new MethodAccessor(getMethod(containerType, getterName),
                setterName != null ? getMethod(containerType,
                        setterName) : null);
    }

    public static MemberAccessor getMemberAccessor(Class<?> containerType,
            String name) throws NoSuchMemberException {
        return getMemberAccessor(containerType, name, false);
    }

    public static MemberAccessor getMemberAccessor(Class<?> containerType,
            String name, boolean isReadOnly) throws NoSuchMemberException {
        try {
            return getFieldAccessor(containerType, name, isReadOnly);
        } catch (NoSuchFieldException e) {
            try {
                return getPropertyAccessor(containerType, name, isReadOnly);
            } catch (NoSuchMethodException ee) {
                throw new NoSuchMemberException("No such member: " + name);
            }
        }
    }

}
