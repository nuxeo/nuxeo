/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.schema.types;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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

    private static final Map<Class<?>, Type> class2Types = new HashMap<Class<?>, Type>();

    private static final Map<Type, Class<?>> types2Class = new HashMap<Type, Class<?>>();

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
