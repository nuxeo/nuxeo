/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.runtime.test.runner;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AnnotationScanner {

    protected Map<Class<?>, List<Annotation>> classes = new Hashtable<Class<?>, List<Annotation>>();

    public synchronized void scan(Class<?> clazz) {
        if (classes.containsKey(clazz)) {
            return;
        }
        List<Annotation> result = new ArrayList<Annotation>();
        Set<Class<?>> visitedClasses = new HashSet<Class<?>>();
        collectAnnotations(clazz, result, visitedClasses);
    }

    public List<? extends Annotation> getAnnotations(Class<?> clazz) {
        return classes.get(clazz);
    }

    public <T extends Annotation> T getFirstAnnotation(Class<?> clazz, Class<T> annotationType) {
        List<T> result = getAnnotations(clazz, annotationType);
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> List<T> getAnnotations(Class<?> clazz, Class<T> annotationType) {
        List<Annotation> list = classes.get(clazz);
        if (list != null) {
            List<T> result = new ArrayList<T>();
            for (Annotation anno : list) {
                if (anno.annotationType() == annotationType) {
                    result.add((T)anno);
                }
            }
            return result;
        }
        return null;
    }

    /**
     * TODO when collecting annotations annotated with {@link Inherited} they will be collected twice.
     * @param clazz
     * @param result
     * @param visitedClasses
     */
    protected void collectAnnotations(Class<?> clazz, List<Annotation> result, Set<Class<?>> visitedClasses) {
        if (visitedClasses.contains(clazz)) {
            return;
        }
        visitedClasses.add(clazz);
        List<Annotation> partialResult = new ArrayList<Annotation>(); // collect only the annotation on this class
        List<Annotation> annos = classes.get(clazz);
        if (annos != null) {
            result.addAll(annos);
            return;
        }
        // collect local annotations
        for (Annotation anno : clazz.getAnnotations()) {
            partialResult.add(anno);
        }
        // first scan interfaces
        for (Class<?> itf : clazz.getInterfaces()) {
            collectAnnotations(itf, partialResult, visitedClasses);
        }
        // collect annotations from super classes
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            collectAnnotations(superClass, partialResult, visitedClasses);
        }
        if (!partialResult.isEmpty()) {
            result.addAll(partialResult);
        }
        classes.put(clazz, partialResult);
    }

}
