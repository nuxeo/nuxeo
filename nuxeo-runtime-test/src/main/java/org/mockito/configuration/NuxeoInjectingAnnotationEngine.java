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
package org.mockito.configuration;

import org.mockito.internal.configuration.InjectingAnnotationEngine;
import org.mockito.internal.configuration.SpyAnnotationEngine;

/**
 *
 *
 * @since 5.7.8
 */
public class NuxeoInjectingAnnotationEngine extends InjectingAnnotationEngine {
    private AnnotationEngine delegate = new NuxeoDefaultAnnotationEngine();

    private AnnotationEngine spyAnnotationEngine = new SpyAnnotationEngine();

    /**
     * Process the fields of the test instance and create Mocks, Spies, Captors
     * and inject them on fields annotated &#64;InjectMocks.
     *
     * <p>
     * This code process the test class and the super classes.
     * <ol>
     * <li>First create Mocks, Spies, Captors.</li>
     * <li>Then try to inject them.</li>
     * </ol>
     *
     * @param clazz Not used
     * @param testInstance The instance of the test, should not be null.
     *
     * @see org.mockito.configuration.AnnotationEngine#process(Class, Object)
     */
    @Override
    public void process(Class<?> clazz, Object testInstance) {
        processIndependentAnnotations(testInstance.getClass(), testInstance);
        processInjectMocks(testInstance.getClass(), testInstance);
    }

    private void processInjectMocks(final Class<?> clazz,
            final Object testInstance) {
        Class<?> classContext = clazz;
        while (classContext != Object.class) {
            injectMocks(testInstance);
            classContext = classContext.getSuperclass();
        }
    }

    private void processIndependentAnnotations(final Class<?> clazz,
            final Object testInstance) {
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
