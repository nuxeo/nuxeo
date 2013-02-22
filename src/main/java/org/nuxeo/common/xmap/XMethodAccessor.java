/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common.xmap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class XMethodAccessor implements XAccessor {

    private final Method setter;
    private final Class klass;
    Method getter;

    public XMethodAccessor(Method method, Class klass) {
        setter = method;
        setter.setAccessible(true);
        //
        this.klass = klass;
    }

    public Class getType() {
        return setter.getParameterTypes()[0];
    }

    public void setValue(Object instance, Object value) {
        try {
            setter.invoke(instance, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public String toString() {
        return "XMethodSetter {method: " + setter + '}';
    }

    public Object getValue(Object instance) {
        // lazy initialization for getter to keep the compatibility
        // with current xmap definition
        if (getter == null) {
            getter = findGetter(klass);
        }
        if (getter != null) {
            try {
                return getter.invoke(instance);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new IllegalArgumentException(e);
            }
        }
        return null;
    }

    private Method findGetter(Class klass) {
        String setterName = setter.getName();
        if (setterName.toLowerCase().startsWith("set")) {
            String suffix = setterName.substring(3);
            String prefix = null;

            Class<?>[] classes = setter.getParameterTypes();
            Class<?> clazz = classes[0];
            // compute the getter name
            if (clazz == Boolean.class || clazz == Boolean.TYPE) {
                prefix = "is";
            } else {
                prefix = "get";
            }
            String getterName = prefix + suffix;
            try {
                return klass.getMethod(getterName, new Class[0]);
            } catch (SecurityException e) {
                throw new IllegalArgumentException(e);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(
                        "there is NO getter defined for annotated setter: " + setterName, e);
            }
        }
        return null;
    }

}
