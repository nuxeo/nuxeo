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
package org.mockito.configuration;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.mockito.Mock;
import org.mockito.internal.configuration.FieldAnnotationProcessor;
import org.mockito.internal.configuration.IndependentAnnotationEngine;
import org.mockito.internal.configuration.InjectingAnnotationEngine;
import org.nuxeo.runtime.mockito.NuxeoServiceMockAnnotationProcessor;

/**
 * Mockito loads this with reflection, so this class might appear unused.
 *
 * @since 5.7.8
 */
public class MockitoConfiguration extends DefaultMockitoConfiguration {

    @Override
    @SuppressWarnings("deprecation")
    public AnnotationEngine getAnnotationEngine() {
        // these classes are hard to subclass as they have many private methods
        // so instead we use reflection to set our NuxeoServiceMockAnnotationProcessor
        InjectingAnnotationEngine engine = new InjectingAnnotationEngine();
        NuxeoServiceMockAnnotationProcessor annotationProcessor = new NuxeoServiceMockAnnotationProcessor();
        try {
            IndependentAnnotationEngine delegate = (IndependentAnnotationEngine) FieldUtils.readField(engine,
                    "delegate", true);
            @SuppressWarnings("unchecked")
            Map<Class<? extends Annotation>, FieldAnnotationProcessor<?>> annotationProcessorMap = (Map<Class<? extends Annotation>, FieldAnnotationProcessor<?>>) FieldUtils.readField(
                    delegate, "annotationProcessorMap", true);
            annotationProcessorMap.put(Mock.class, annotationProcessor);
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException(e);
        }
        return engine;
    }
}
