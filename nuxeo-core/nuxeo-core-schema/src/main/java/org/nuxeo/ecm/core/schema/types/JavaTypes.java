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

package org.nuxeo.ecm.core.schema.types;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 * Maps ECM types to Java classes.
 *
 * @author bstefanescu
 */
public final class JavaTypes {

    private static final Map<Class<?>, Type> class2Types = new Hashtable<Class<?>, Type>();

    private static final Map<Type, Class<?>> types2Class = new Hashtable<Type, Class<?>>();

    // Utility class.
    private JavaTypes() {
    }

    public static boolean isList(Object object) {
        return object instanceof List;
    }

    public static boolean isComplex(Object object) {
        return object instanceof Map;
    }

    public static Type getType(Class<?> klass) {
        return class2Types.get(klass);
    }

    public static Class<?> getClass(Type type) {
        if (type.isSimpleType() && !((SimpleType) type).isPrimitive()) {
            return getClass(((SimpleType) type).getPrimitiveType());
        }
        return types2Class.get(type);
    }

    public static Class<?> getPrimitiveClass(Type type) {
        Class<?> k = types2Class.get(type);
        if (k == Long.class) {
            return Long.TYPE;
        } else if (k == Integer.class) {
            return Integer.TYPE;
        } else if (k == Double.class) {
            return Double.TYPE;
        } else if (k == Float.class) {
            return Float.TYPE;
        } else if (k == Short.class) {
            return Short.TYPE;
        } else if (k == Byte.class) {
            return Byte.TYPE;
        } else if (k == Character.class) {
            return Character.TYPE;
        }
        return k;
    }

    public static void bind(Type type, Class<?> klass) {
        class2Types.put(klass, type);
        types2Class.put(type, klass);
    }

    static {
        bind(StringType.INSTANCE, String.class);
        bind(LongType.INSTANCE, Long.class);
        bind(IntegerType.INSTANCE, Integer.class);
        bind(DoubleType.INSTANCE, Double.class);
        bind(BooleanType.INSTANCE, Boolean.class);
        bind(BinaryType.INSTANCE, InputStream.class);
        bind(DateType.INSTANCE, Date.class);
        bind(DateType.INSTANCE, Calendar.class);

        // bind(typeService.getTypeManager().getType("content"), Blob.class);
        // bind(typeService.getTypeManager().getType("content"), List.class);
        // bind(CompType.INSTANCE, Blob.class);
    }

}
