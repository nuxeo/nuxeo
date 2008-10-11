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
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AnnotatedMethod {

    protected AnnotatedClass<?> aclass;
    protected Method method;
    protected Map<Class<? extends Annotation>, Annotation> annotations;

    AnnotatedMethod(AnnotatedClass<?> aclass, Method method) {
        this (aclass, method, new HashMap<Class<? extends Annotation>, Annotation>());
    }

    AnnotatedMethod(AnnotatedClass<?> aclass, Method method, Map<Class<? extends Annotation>, Annotation> annos) {
        this.aclass = aclass;
        this.method = method;
        this.annotations = annos;
    }

    public AnnotatedClass<?> getAnnotatedClass() {
        return aclass;
    }

    /**
     * @return the method.
     */
    public Method getMethod() {
        return method;
    }

    /**
     * @return the annotations.
     */
    public Annotation[] getAnnotations() {
        return annotations.values().toArray(new Annotation[annotations.size()]);
    }

    public Annotation[] getDeclaredAnnotations() {
        return method.getDeclaredAnnotations();
    }

    @SuppressWarnings("unchecked")
    public <A extends  Annotation> A getAnnotation(Class<A> annotationClass) {
        return (A)annotations.get(annotationClass);
    }

    public <A extends  Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    public boolean isAnnotationPresent(Class<? extends  Annotation> annotationClass) {
        return annotations.containsKey(annotationClass);
    }

    public boolean isDeclaringAnnotation(Class<? extends  Annotation> annotationClass) {
        return method.isAnnotationPresent(annotationClass);
    }



}
