/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * Unfortunately, since there are some private methos in the Mockito
 * DefaultAnnotationEngine we have to copy/paste some original code to insert
 * our own logic.
 *
 * @since 5.7.8
 */
public class NuxeoDefaultAnnotationEngine extends DefaultAnnotationEngine {

    private final Map<Class<? extends Annotation>, FieldAnnotationProcessor<?>> annotationProcessorMap = new HashMap<Class<? extends Annotation>, FieldAnnotationProcessor<?>>();

    public NuxeoDefaultAnnotationEngine() {
        registerAnnotationProcessor(Mock.class,
                new NuxeoServiceMockAnnotationProcessor());
        registerAnnotationProcessor(MockitoAnnotations.Mock.class,
                new MockitoAnnotationsMockAnnotationProcessor());
        registerAnnotationProcessor(Captor.class,
                new CaptorAnnotationProcessor());

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.mockito.AnnotationEngine#createMockFor(java.lang.annotation.Annotation
     * , java.lang.reflect.Field)
     */
    @Override
    @SuppressWarnings("deprecation")
    public Object createMockFor(Annotation annotation, Field field) {
        return forAnnotation(annotation).process(annotation, field);
    }

    private <A extends Annotation> FieldAnnotationProcessor<A> forAnnotation(
            A annotation) {
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

    private <A extends Annotation> void registerAnnotationProcessor(
            Class<A> annotationClass,
            FieldAnnotationProcessor<A> fieldAnnotationProcessor) {
        annotationProcessorMap.put(annotationClass, fieldAnnotationProcessor);
    }

    void throwIfAlreadyAssigned(Field field, boolean alreadyAssigned) {
        if (alreadyAssigned) {
            new Reporter().moreThanOneAnnotationNotAllowed(field.getName());
        }
    }
}
