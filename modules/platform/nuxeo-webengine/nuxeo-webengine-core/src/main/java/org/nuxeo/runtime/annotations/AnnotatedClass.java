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
 *
 * $Id$
 */

package org.nuxeo.runtime.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AnnotatedClass<T> {

    protected AnnotatedClass<?> superClass;

    protected final Class<T> clazz;

    protected final Map<Class<? extends Annotation>, Annotation> annotations;

    protected final Map<Method, AnnotatedMethod> methods;

    public AnnotatedClass(Class<T> clazz) {
        this.clazz = clazz;
        methods = new HashMap<>();
        annotations = new HashMap<>();
    }

    public Class<?> getAnnotatedClass() {
        return clazz;
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return (A) annotations.get(annotationClass);
    }

    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        return clazz.getAnnotation(annotationClass);
    }

    public Annotation[] getAnnotations() {
        return annotations.values().toArray(new Annotation[annotations.size()]);
    }

    public Annotation[] getDeclaredAnnotations() {
        return clazz.getDeclaredAnnotations();
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return annotations.containsKey(annotationClass);
    }

    public boolean isDeclaringAnnotation(Class<? extends Annotation> annotationClass) {
        return clazz.isAnnotationPresent(annotationClass);
    }

    public AnnotatedMethod getAnnotatedMethod(Method method) {
        return methods.get(method);
    }

    public AnnotatedMethod getAnnotatedMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return getAnnotatedMethod(clazz.getMethod(name, parameterTypes));
    }

    public AnnotatedMethod getDeclaredAnnotatedMethod(Method method) {
        AnnotatedMethod am = methods.get(method);
        return am != null && am.method.getDeclaringClass() == clazz ? am : null;
    }

    public boolean hasAnnotatedMethods() {
        return !methods.isEmpty();
    }

    public boolean isDeclaringAnnotatedMethods() {
        if (methods.isEmpty()) {
            return false;
        }
        for (AnnotatedMethod am : methods.values()) {
            if (am.method.getDeclaringClass() == clazz) {
                return true;
            }
        }
        return false;
    }

    public AnnotatedMethod[] getAnnotatedMethods() {
        return methods.values().toArray(new AnnotatedMethod[methods.size()]);
    }

    public AnnotatedMethod[] getDeclaredAnnotatedMethods() {
        ArrayList<AnnotatedMethod> result = new ArrayList<>();
        for (AnnotatedMethod am : methods.values()) {
            if (am.method.getDeclaringClass() == clazz) {
                result.add(am);
            }
        }
        return result.toArray(new AnnotatedMethod[result.size()]);
    }

    // TODO: cache this?
    public AnnotatedMethod[] getAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        ArrayList<AnnotatedMethod> result = new ArrayList<>();
        for (AnnotatedMethod am : methods.values()) {
            if (am.annotations.containsKey(annotationClass)) {
                result.add(am);
            }
        }
        return result.toArray(new AnnotatedMethod[result.size()]);
    }

    public AnnotatedMethod[] getDeclaredAnnotatedMethods(Class<? extends Annotation> annotationClass) {
        ArrayList<AnnotatedMethod> result = new ArrayList<>();
        for (AnnotatedMethod am : methods.values()) {
            if (am.method.getDeclaringClass() == clazz && am.annotations.containsKey(annotationClass)) {
                result.add(am);
            }
        }
        return result.toArray(new AnnotatedMethod[result.size()]);
    }

    public void addMethod(AnnotatedMethod method) {
        methods.put(method.method, method);
        // TODO cache annotations to annotated method?
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() == AnnotatedClass.class) {
            return ((AnnotatedClass<?>) obj).clazz == clazz;
        }
        return false;
    }

    @Override
    public String toString() {
        return "AnnotatedCass: " + clazz;
    }

}
