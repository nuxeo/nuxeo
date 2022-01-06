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
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AnnotationScanner {

    protected final Set<Class<?>> visitedClasses = new HashSet<Class<?>>();

    protected final Map<Class<?>, List<Annotation>> classes = new Hashtable<Class<?>, List<Annotation>>();

    protected final Map<Class<?>, Map<Class<?>, List<Annotation>>> classesAnnotations = new ConcurrentHashMap<>();

    /**
     * @deprecated since 2021.15, doesn't take into account @Repeatable annotations, prefer to use
     *             {@link #getAnnotations(Class, Class)} instead.
     */
    @Deprecated
    public synchronized void scan(Class<?> clazz) {
        if (classes.containsKey(clazz)) {
            return;
        }
        collectAnnotations(clazz);
    }

    /**
     * @deprecated since 2021.15, doesn't take into account @Repeatable annotations, prefer to use
     *             {@link #getAnnotations(Class, Class)} instead.
     */
    @Deprecated
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
    public <T extends Annotation> List<T> getAnnotations(Class<?> clazz, Class<T> annotationClass) {
        return (List<T>) classesAnnotations.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>())
                                           .computeIfAbsent(annotationClass,
                                                            k -> collectAnnotations(clazz, annotationClass));
    }

    protected <T extends Annotation> List<Annotation> collectAnnotations(Class<?> clazz, Class<T> annotationClass) {
        Set<Annotation> annotations = new LinkedHashSet<>();
        collectAnnotations(clazz, annotationClass, annotations);
        return new ArrayList<>(annotations);
    }

    protected <T extends Annotation> void collectAnnotations(Class<?> clazz, Class<T> annotationClass,
            Set<Annotation> annotations) {
        // check if we already computed the annotations for the given clazz
        if (classesAnnotations.getOrDefault(clazz, Collections.emptyMap()).containsKey(annotationClass)) {
            annotations.addAll(classesAnnotations.get(clazz).get(annotationClass));
            return;
        }
        // first collect annotations from class
        annotations.addAll(Arrays.asList(clazz.getAnnotationsByType(annotationClass)));
        // second collect annotations from interfaces
        for (Class<?> itf : clazz.getInterfaces()) {
            collectAnnotations(itf, annotationClass, annotations);
        }
        // third collect annotations from super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            collectAnnotations(superClass, annotationClass, annotations);
        }
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
