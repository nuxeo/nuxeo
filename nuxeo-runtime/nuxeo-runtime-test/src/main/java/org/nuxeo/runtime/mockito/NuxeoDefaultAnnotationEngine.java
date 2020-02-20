/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.runtime.mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.exceptions.Reporter;
import org.mockito.internal.configuration.CaptorAnnotationProcessor;
import org.mockito.internal.configuration.DefaultAnnotationEngine;
import org.mockito.internal.configuration.FieldAnnotationProcessor;
import org.mockito.internal.configuration.MockitoAnnotationsMockAnnotationProcessor;

/**
 * Unfortunately, since there are some private methos in the Mockito DefaultAnnotationEngine we have to copy/paste some
 * original code to insert our own logic.
 *
 * @since 5.7.8
 */
public class NuxeoDefaultAnnotationEngine extends DefaultAnnotationEngine {

    private final Map<Class<? extends Annotation>, FieldAnnotationProcessor<?>> annotationProcessorMap = new HashMap<>();

    public NuxeoDefaultAnnotationEngine() {
        registerAnnotationProcessor(Mock.class, new NuxeoServiceMockAnnotationProcessor());
        registerAnnotationProcessor(MockitoAnnotations.Mock.class, new MockitoAnnotationsMockAnnotationProcessor());
        registerAnnotationProcessor(Captor.class, new CaptorAnnotationProcessor());

    }

    /*
     * (non-Javadoc)
     * @see org.mockito.AnnotationEngine#createMockFor(java.lang.annotation.Annotation , java.lang.reflect.Field)
     */
    @Override
    @SuppressWarnings("deprecation")
    public Object createMockFor(Annotation annotation, Field field) {
        return forAnnotation(annotation).process(annotation, field);
    }

    private <A extends Annotation> FieldAnnotationProcessor<A> forAnnotation(A annotation) { // NOSONAR
        if (annotationProcessorMap.containsKey(annotation.annotationType())) {
            return (FieldAnnotationProcessor<A>) annotationProcessorMap.get(annotation.annotationType());
        }
        return new FieldAnnotationProcessor<A>() {
            @Override
            public Object process(A annotation, Field field) {
                return null;
            }
        };
    }

    private <A extends Annotation> void registerAnnotationProcessor(Class<A> annotationClass, // NOSONAR
            FieldAnnotationProcessor<A> fieldAnnotationProcessor) {
        annotationProcessorMap.put(annotationClass, fieldAnnotationProcessor);
    }

    void throwIfAlreadyAssigned(Field field, boolean alreadyAssigned) { // NOSONAR
        if (alreadyAssigned) {
            new Reporter().moreThanOneAnnotationNotAllowed(field.getName());
        }
    }
}
