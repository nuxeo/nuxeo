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
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AnnotationScanner {

    protected final Set<Class<?>> visitedClasses = new HashSet<Class<?>>();

    protected final Map<Class<?>, List<Annotation>> classes = new Hashtable<Class<?>, List<Annotation>>();

    public synchronized void scan(Class<?> clazz) {
        if (classes.containsKey(clazz)) {
            return;
        }
        collectAnnotations(clazz);
    }

    public List<? extends Annotation> getAnnotations(Class<?> clazz) {
        if (!visitedClasses.contains(clazz)) {
            scan(clazz);
        }
        return classes.get(clazz);
    }

    public <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationType) {
        final List<T> annotations = getAnnotations(clazz, annotationType);
        if (annotations.isEmpty()) {
            return null;
        }
        return Defaults.of(annotationType, annotations);
    }

    public <T extends Annotation> T getFirstAnnotation(Class<?> clazz, Class<T> annotationType) {
        List<T> result = getAnnotations(clazz, annotationType);
        if (result.isEmpty()) {
            return null;
        }
        return result.get(0);
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> List<T> getAnnotations(Class<?> clazz, Class<T> annotationType) {
        if (!visitedClasses.contains(clazz)) {
            scan(clazz);
        }
        return (List<T>) ImmutableList.copyOf(Iterables.filter(classes.get(clazz),
                Predicates.instanceOf(annotationType)));
    }

    /**
     * TODO when collecting annotations annotated with {@link Inherited} they will be collected twice.
     *
     * @param clazz
     * @param result
     * @param visitedClasses
     */
    protected List<Annotation> collectAnnotations(Class<?> clazz) {
        if (visitedClasses.contains(clazz)) {
            return classes.get(clazz);
        }
        visitedClasses.add(clazz);
        List<Annotation> result = new ArrayList<Annotation>(); // collect only the annotation on this class
        try {
            Annotation[] data = clazz.getAnnotations();
            result.addAll(Arrays.asList(data));
        } catch (ArrayStoreException cause) {
            throw new AssertionError(
                    "Cannot load annotations of " + clazz.getName() + ", check your classpath for missing classes\n" + new FastClasspathScanner().getUniqueClasspathElements(),
                    cause);
        }
        // first scan interfaces
        for (Class<?> itf : clazz.getInterfaces()) {
            result.addAll(collectAnnotations(itf));
        }
        // collect annotations from super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            result.addAll(collectAnnotations(superClass));
        }
        classes.put(clazz, result);
        return result;
    }

}
