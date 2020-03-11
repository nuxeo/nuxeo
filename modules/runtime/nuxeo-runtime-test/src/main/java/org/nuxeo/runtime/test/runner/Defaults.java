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
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Defaults<A extends Annotation> implements InvocationHandler {

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A of(Class<A> annotation) {
        return (A) Proxy.newProxyInstance(annotation.getClassLoader(), new Class[] { annotation }, new Defaults<A>());
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A of(Class<A> type, Iterable<A> annotations) {
        return (A) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new Defaults<>(annotations));
    }

    @SafeVarargs
    public static <A extends Annotation> A of(Class<A> type, A... annotations) {
        return of(type, Arrays.asList(annotations));
    }

    protected Defaults() {
        this.annotations = Collections.emptyList();
    }

    protected Defaults(Iterable<A> annotations) {
        this.annotations = annotations;
    }

    protected final Iterable<A> annotations;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        final Object defaultValue = method.getDefaultValue();
        for (Annotation each : annotations) {
            if (each == null) {
                continue;
            }
            Object value = method.invoke(each);
            if (value != null && !value.equals(defaultValue)) {
                return value;
            }
        }
        return defaultValue;
    }
}
