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
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AnnotatedMethod {

    protected final AnnotatedClass<?> aclass;

    protected final Method method;

    protected final Map<Class<? extends Annotation>, Annotation> annotations;

    AnnotatedMethod(AnnotatedClass<?> aclass, Method method) {
        this(aclass, method, new HashMap<Class<? extends Annotation>, Annotation>());
    }

    AnnotatedMethod(AnnotatedClass<?> aclass, Method method, Map<Class<? extends Annotation>, Annotation> annos) {
        this.aclass = aclass;
        this.method = method;
        annotations = annos;
    }

    public AnnotatedClass<?> getAnnotatedClass() {
        return aclass;
    }

    public Method getMethod() {
        return method;
    }

    public Annotation[] getAnnotations() {
        return annotations.values().toArray(new Annotation[annotations.size()]);
    }

    public Annotation[] getDeclaredAnnotations() {
        return method.getDeclaredAnnotations();
    }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        return (A) annotations.get(annotationClass);
    }

    public <A extends Annotation> A getDeclaredAnnotation(Class<A> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return annotations.containsKey(annotationClass);
    }

    public boolean isDeclaringAnnotation(Class<? extends Annotation> annotationClass) {
        return method.isAnnotationPresent(annotationClass);
    }

}
