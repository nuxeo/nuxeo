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

import org.mockito.configuration.AnnotationEngine;
import org.mockito.internal.configuration.InjectingAnnotationEngine;
import org.mockito.internal.configuration.SpyAnnotationEngine;

/**
 * @since 5.7.8
 */
public class NuxeoInjectingAnnotationEngine extends InjectingAnnotationEngine {
    private AnnotationEngine delegate = new NuxeoDefaultAnnotationEngine();

    private AnnotationEngine spyAnnotationEngine = new SpyAnnotationEngine();

    /**
     * Process the fields of the test instance and create Mocks, Spies, Captors and inject them on fields annotated
     * &#64;InjectMocks.
     * <p>
     * This code process the test class and the super classes.
     * <ol>
     * <li>First create Mocks, Spies, Captors.</li>
     * <li>Then try to inject them.</li>
     * </ol>
     *
     * @param clazz Not used
     * @param testInstance The instance of the test, should not be null.
     * @see org.mockito.configuration.AnnotationEngine#process(Class, Object)
     */
    @Override
    public void process(Class<?> clazz, Object testInstance) {
        processIndependentAnnotations(testInstance.getClass(), testInstance);
        processInjectMocks(testInstance.getClass(), testInstance);
    }

    private void processInjectMocks(final Class<?> clazz, final Object testInstance) { // NOSONAR
        Class<?> classContext = clazz;
        while (classContext != Object.class) {
            injectMocks(testInstance);
            classContext = classContext.getSuperclass();
        }
    }

    private void processIndependentAnnotations(final Class<?> clazz, final Object testInstance) { // NOSONAR
        Class<?> classContext = clazz;
        while (classContext != Object.class) {
            // this will create @Mocks, @Captors, etc:
            delegate.process(classContext, testInstance);
            // this will create @Spies:
            spyAnnotationEngine.process(classContext, testInstance);

            classContext = classContext.getSuperclass();
        }
    }

}
