/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.annotations;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings("unchecked")
public class AnnotationManager {

    protected Map<Class<?>, AnnotatedClass<?>> classCache;

    public AnnotationManager() {
        classCache = createCache();
    }

    protected Map<Class<?>, AnnotatedClass<?>> createCache() {
        return new ConcurrentHashMap<Class<?>, AnnotatedClass<?>>();
    }

    public void flushCache() {
        classCache = createCache();
    }

    public <T> AnnotatedClass<T> getAnnotatedClass(Class<T> clazz) {
        // use a local variable to avoid problem when flushing cache
        // in another thread while this method is executing
        Map<Class<?>, AnnotatedClass<?>> cache = classCache;
        AnnotatedClass<T> aclass = (AnnotatedClass<T>) cache.get(clazz);
        if (aclass == null) {
            aclass = load(clazz);
            cache.put(clazz, aclass);
        }
        return aclass;
    }

    public <T> AnnotatedClass<T> lookup(Class<T> clazz) {
        return (AnnotatedClass<T>) classCache.get(clazz);
    }

    public <T> AnnotatedClass<T> load(Class<T> clazz) {
        AnnotatedClass<T> aclass = new AnnotatedClass<T>(clazz);
        Class<?> zuper = clazz.getSuperclass();
        MethodAnnotations mannos = new MethodAnnotations();

        // collect super class annotations
        if (zuper != null) {
            AnnotatedClass<?> azuper = getAnnotatedClass(zuper);
            aclass.annotations.putAll(azuper.annotations);
            for (AnnotatedMethod am : azuper.getAnnotatedMethods()) {
                mannos.addSuperMethod(am);
            }
        }

        // collect interface annotations
        for (Class<?> itf : clazz.getInterfaces()) {
            AnnotatedClass<?> aitf = getAnnotatedClass(itf);
            aclass.annotations.putAll(aitf.annotations);
            for (AnnotatedMethod am : aitf.getAnnotatedMethods()) {
                mannos.addSuperMethod(am);
            }
        }

        // finally add class own annotations
        mannos.addMethods(clazz);
        for (Annotation anno : clazz.getAnnotations()) {
            aclass.annotations.put(anno.annotationType(), anno);
        }

        // create annotated methods from collected methods
        for (MethodAnnotations.Entry entry : mannos.entries) {
            AnnotatedMethod am = new AnnotatedMethod(aclass, entry.method, entry.annos);
            aclass.addMethod(am);
        }
        return aclass;
    }

}
